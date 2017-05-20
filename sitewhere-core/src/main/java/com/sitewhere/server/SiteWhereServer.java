/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.server;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.sitewhere.SiteWhere;
import com.sitewhere.common.MarshalUtils;
import com.sitewhere.configuration.ConfigurationUtils;
import com.sitewhere.configuration.ResourceManagerGlobalConfigurationResolver;
import com.sitewhere.core.Boilerplate;
import com.sitewhere.groovy.configuration.GroovyConfiguration;
import com.sitewhere.hazelcast.HazelcastConfiguration;
import com.sitewhere.rest.model.search.tenant.TenantSearchCriteria;
import com.sitewhere.rest.model.server.SiteWhereServerRuntime;
import com.sitewhere.rest.model.server.SiteWhereServerRuntime.GeneralInformation;
import com.sitewhere.rest.model.server.SiteWhereServerRuntime.JavaInformation;
import com.sitewhere.rest.model.server.SiteWhereServerState;
import com.sitewhere.rest.model.server.TenantPersistentState;
import com.sitewhere.rest.model.user.User;
import com.sitewhere.security.SitewhereAuthentication;
import com.sitewhere.security.SitewhereUserDetails;
import com.sitewhere.server.debug.NullTracer;
import com.sitewhere.server.jvm.JvmHistoryMonitor;
import com.sitewhere.server.lifecycle.CompositeLifecycleStep;
import com.sitewhere.server.lifecycle.LifecycleComponent;
import com.sitewhere.server.lifecycle.LifecycleProgressMonitor;
import com.sitewhere.server.lifecycle.SimpleLifecycleStep;
import com.sitewhere.server.lifecycle.StartComponentLifecycleStep;
import com.sitewhere.server.lifecycle.StopComponentLifecycleStep;
import com.sitewhere.server.resource.SiteWhereHomeResourceManager;
import com.sitewhere.server.tenant.TenantManagementTriggers;
import com.sitewhere.server.tenant.TenantTemplateManager;
import com.sitewhere.spi.ServerStartupException;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.SiteWhereSystemException;
import com.sitewhere.spi.asset.IAssetManagement;
import com.sitewhere.spi.asset.IAssetModuleManager;
import com.sitewhere.spi.configuration.IGlobalConfigurationResolver;
import com.sitewhere.spi.device.IDeviceManagement;
import com.sitewhere.spi.device.IDeviceManagementCacheProvider;
import com.sitewhere.spi.device.communication.IDeviceCommunication;
import com.sitewhere.spi.device.event.IDeviceEventManagement;
import com.sitewhere.spi.device.event.IEventProcessing;
import com.sitewhere.spi.error.ErrorCode;
import com.sitewhere.spi.error.ErrorLevel;
import com.sitewhere.spi.resource.IResource;
import com.sitewhere.spi.resource.IResourceManager;
import com.sitewhere.spi.scheduling.IScheduleManagement;
import com.sitewhere.spi.scheduling.IScheduleManager;
import com.sitewhere.spi.search.ISearchResults;
import com.sitewhere.spi.search.external.ISearchProviderManager;
import com.sitewhere.spi.server.IBackwardCompatibilityService;
import com.sitewhere.spi.server.ISiteWhereServer;
import com.sitewhere.spi.server.ISiteWhereServerRuntime;
import com.sitewhere.spi.server.ISiteWhereServerState;
import com.sitewhere.spi.server.debug.ITracer;
import com.sitewhere.spi.server.groovy.IGroovyConfiguration;
import com.sitewhere.spi.server.groovy.ITenantGroovyConfiguration;
import com.sitewhere.spi.server.hazelcast.IHazelcastConfiguration;
import com.sitewhere.spi.server.lifecycle.ICompositeLifecycleStep;
import com.sitewhere.spi.server.lifecycle.IDiscoverableTenantLifecycleComponent;
import com.sitewhere.spi.server.lifecycle.ILifecycleComponent;
import com.sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor;
import com.sitewhere.spi.server.lifecycle.LifecycleComponentType;
import com.sitewhere.spi.server.lifecycle.LifecycleStatus;
import com.sitewhere.spi.server.tenant.ISiteWhereTenantEngine;
import com.sitewhere.spi.server.tenant.ITenantModelInitializer;
import com.sitewhere.spi.server.tenant.ITenantPersistentState;
import com.sitewhere.spi.server.tenant.ITenantTemplateManager;
import com.sitewhere.spi.server.user.IUserModelInitializer;
import com.sitewhere.spi.system.IVersion;
import com.sitewhere.spi.system.IVersionChecker;
import com.sitewhere.spi.tenant.ITenant;
import com.sitewhere.spi.tenant.ITenantManagement;
import com.sitewhere.spi.user.IGrantedAuthority;
import com.sitewhere.spi.user.IUserManagement;
import com.sitewhere.version.VersionHelper;

/**
 * Implementation of {@link ISiteWhereServer} for community edition.
 * 
 * @author Derek Adams
 */
public class SiteWhereServer extends LifecycleComponent implements ISiteWhereServer {

    /** Number of threads in pool for starting tenants */
    private static final int TENANT_STARTUP_PARALLELISM = 5;

    /** Private logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /** Spring context for server */
    public static ApplicationContext SERVER_SPRING_CONTEXT;

    /** Contains version information */
    private IVersion version;

    /** Version checker implementation */
    private IVersionChecker versionChecker;

    /** Persistent server state information */
    private ISiteWhereServerState serverState;

    /** Contains runtime information about the server */
    private ISiteWhereServerRuntime serverRuntime;

    /** Server startup error */
    private ServerStartupException serverStartupError;

    /** Provides hierarchical tracing for debugging */
    protected ITracer tracer = new NullTracer();

    /** Hazelcast configuration for this node */
    protected IHazelcastConfiguration hazelcastConfiguration;

    /** Groovy configuration for this node */
    protected IGroovyConfiguration groovyConfiguration;

    /** Bootstrap resource manager implementation */
    protected IResourceManager bootstrapResourceManager;

    /** Runtime resource manager implementation */
    protected IResourceManager runtimeResourceManager;

    /** Allows Spring configuration to be resolved */
    protected IGlobalConfigurationResolver configurationResolver;

    /** Tenant template manager implementation */
    protected ITenantTemplateManager tenantTemplateManager;

    /** Interface to user management implementation */
    protected IUserManagement userManagement;

    /** Interface to tenant management implementation */
    protected ITenantManagement tenantManagement;

    /** Components registered to participate in SiteWhere server lifecycle */
    private List<ILifecycleComponent> registeredLifecycleComponents = new ArrayList<ILifecycleComponent>();

    /** Map of component ids to lifecycle components */
    private Map<String, ILifecycleComponent> lifecycleComponentsById = new HashMap<String, ILifecycleComponent>();

    /** Map of tenants by authentication token */
    private Map<String, ITenant> tenantsByAuthToken = new HashMap<String, ITenant>();

    /** Map of tenant engines by tenant id */
    private Map<String, ISiteWhereTenantEngine> tenantEnginesById = new HashMap<String, ISiteWhereTenantEngine>();

    /** Metric regsitry */
    private MetricRegistry metricRegistry = new MetricRegistry();

    /** Health check registry */
    private HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();

    /** Timestamp when server was started */
    private Long uptime;

    /** Records historical values for JVM */
    private JvmHistoryMonitor jvmHistory = new JvmHistoryMonitor(this);

    /** Thread for executing JVM history monitor */
    private ExecutorService executor;

    /** Thread pool for starting tenants in parallel */
    private ExecutorService tenantStarters;

    /** Supports migrating old server version to new format */
    private IBackwardCompatibilityService backwardCompatibilityService = new BackwardCompatibilityService();

    public SiteWhereServer() {
	super(LifecycleComponentType.System);
    }

    /**
     * Get Spring application context for Atlas server objects.
     * 
     * @return
     */
    public static ApplicationContext getServerSpringContext() {
	return SERVER_SPRING_CONTEXT;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getVersion()
     */
    public IVersion getVersion() {
	return version;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.server.ISiteWhereServer#getConfigurationParserClassname
     * ()
     */
    @Override
    public String getConfigurationParserClassname() {
	return "com.sitewhere.spring.handler.ConfigurationParser";
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#
     * getTenantConfigurationParserClassname()
     */
    @Override
    public String getTenantConfigurationParserClassname() {
	return "com.sitewhere.spring.handler.TenantConfigurationParser";
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getServerState()
     */
    @Override
    public ISiteWhereServerState getServerState() {
	return serverState;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getServerState(boolean)
     */
    public ISiteWhereServerRuntime getServerRuntimeInformation(boolean includeHistorical) throws SiteWhereException {
	this.serverRuntime = computeServerRuntime(includeHistorical);
	return serverRuntime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getServerStartupError()
     */
    public ServerStartupException getServerStartupError() {
	return serverStartupError;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#setServerStartupError(com.
     * sitewhere.spi .ServerStartupException)
     */
    public void setServerStartupError(ServerStartupException e) {
	this.serverStartupError = e;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getTracer()
     */
    public ITracer getTracer() {
	return tracer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.server.ISiteWhereServer#getHazelcastConfiguration()
     */
    @Override
    public IHazelcastConfiguration getHazelcastConfiguration() {
	return hazelcastConfiguration;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getGroovyConfiguration()
     */
    @Override
    public IGroovyConfiguration getGroovyConfiguration() {
	return groovyConfiguration;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.server.ISiteWhereServer#getBootstrapResourceManager()
     */
    @Override
    public IResourceManager getBootstrapResourceManager() {
	return bootstrapResourceManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.server.ISiteWhereServer#getRuntimeResourceManager()
     */
    @Override
    public IResourceManager getRuntimeResourceManager() {
	return runtimeResourceManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getConfigurationResolver()
     */
    public IGlobalConfigurationResolver getConfigurationResolver() {
	return configurationResolver;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getTenantTemplateManager()
     */
    @Override
    public ITenantTemplateManager getTenantTemplateManager() {
	return tenantTemplateManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.server.ISiteWhereServer#getAuthorizedTenants(java.lang.
     * String, boolean)
     */
    @Override
    public List<ITenant> getAuthorizedTenants(String userId, boolean requireStarted) throws SiteWhereException {
	ISearchResults<ITenant> tenants = SiteWhere.getServer().getTenantManagement()
		.listTenants(new TenantSearchCriteria(1, 0));
	List<ITenant> matches = new ArrayList<ITenant>();
	for (ITenant tenant : tenants.getResults()) {
	    if (tenant.getAuthorizedUserIds().contains(userId)) {
		if (requireStarted) {
		    ISiteWhereTenantEngine engine = getTenantEngine(tenant.getId());
		    if ((engine == null) || (engine.getLifecycleStatus() != LifecycleStatus.Started)) {
			continue;
		    }
		}
		matches.add(tenant);
	    }
	}
	return matches;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getTenantEngine(java.lang.
     * String)
     */
    @Override
    public ISiteWhereTenantEngine getTenantEngine(String tenantId) throws SiteWhereException {
	ISiteWhereTenantEngine engine = tenantEnginesById.get(tenantId);
	if (engine == null) {
	    ITenant tenant = getTenantManagement().getTenantById(tenantId);
	    if (tenant == null) {
		return null;
	    }
	    engine = initializeTenantEngine(tenant);
	}
	return engine;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.server.ISiteWhereServer#onTenantInformationUpdated(com.
     * sitewhere .spi.user.ITenant)
     */
    @Override
    public void onTenantInformationUpdated(ITenant tenant) throws SiteWhereException {
	// Account for updated authentication token.
	for (ITenant current : tenantsByAuthToken.values()) {
	    if (current.getId().equals(tenant.getId())) {
		tenantsByAuthToken.remove(current);
	    }
	}
	tenantsByAuthToken.put(tenant.getAuthenticationToken(), tenant);

	// Update tenant information in tenant engine.
	ISiteWhereTenantEngine engine = tenantEnginesById.get(tenant.getId());
	if (engine != null) {
	    engine.setTenant(tenant);
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getUserManagement()
     */
    public IUserManagement getUserManagement() {
	return userManagement;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getTenantManagement()
     */
    @Override
    public ITenantManagement getTenantManagement() {
	return tenantManagement;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getDeviceManagement(com.
     * sitewhere.spi .user.ITenant)
     */
    @Override
    public IDeviceManagement getDeviceManagement(ITenant tenant) throws SiteWhereException {
	ISiteWhereTenantEngine engine = assureTenantEngine(tenant);
	return engine.getDeviceManagement();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.server.ISiteWhereServer#getDeviceEventManagement(com.
     * sitewhere .spi.user.ITenant)
     */
    @Override
    public IDeviceEventManagement getDeviceEventManagement(ITenant tenant) throws SiteWhereException {
	ISiteWhereTenantEngine engine = assureTenantEngine(tenant);
	return engine.getDeviceEventManagement();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#
     * getDeviceManagementCacheProvider(com. sitewhere.spi.user.ITenant)
     */
    @Override
    public IDeviceManagementCacheProvider getDeviceManagementCacheProvider(ITenant tenant) throws SiteWhereException {
	ISiteWhereTenantEngine engine = assureTenantEngine(tenant);
	return engine.getDeviceManagementCacheProvider();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getAssetManagement(com.
     * sitewhere.spi. user.ITenant)
     */
    @Override
    public IAssetManagement getAssetManagement(ITenant tenant) throws SiteWhereException {
	ISiteWhereTenantEngine engine = assureTenantEngine(tenant);
	return engine.getAssetManagement();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getScheduleManagement(com.
     * sitewhere.spi .user.ITenant)
     */
    @Override
    public IScheduleManagement getScheduleManagement(ITenant tenant) throws SiteWhereException {
	ISiteWhereTenantEngine engine = assureTenantEngine(tenant);
	return engine.getScheduleManagement();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.server.ISiteWhereServer#getDeviceCommunication(com.
     * sitewhere. spi.user.ITenant)
     */
    @Override
    public IDeviceCommunication getDeviceCommunication(ITenant tenant) throws SiteWhereException {
	ISiteWhereTenantEngine engine = assureTenantEngine(tenant);
	return engine.getDeviceCommunication();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getEventProcessing(com.
     * sitewhere.spi. user.ITenant)
     */
    @Override
    public IEventProcessing getEventProcessing(ITenant tenant) throws SiteWhereException {
	ISiteWhereTenantEngine engine = assureTenantEngine(tenant);
	return engine.getEventProcessing();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getAssetModuleManager(com.
     * sitewhere.spi .user.ITenant)
     */
    @Override
    public IAssetModuleManager getAssetModuleManager(ITenant tenant) throws SiteWhereException {
	ISiteWhereTenantEngine engine = assureTenantEngine(tenant);
	return engine.getAssetModuleManager();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.server.ISiteWhereServer#getSearchProviderManager(com.
     * sitewhere .spi.user.ITenant)
     */
    @Override
    public ISearchProviderManager getSearchProviderManager(ITenant tenant) throws SiteWhereException {
	ISiteWhereTenantEngine engine = assureTenantEngine(tenant);
	return engine.getSearchProviderManager();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getScheduleManager(com.
     * sitewhere.spi. user.ITenant)
     */
    @Override
    public IScheduleManager getScheduleManager(ITenant tenant) throws SiteWhereException {
	ISiteWhereTenantEngine engine = assureTenantEngine(tenant);
	return engine.getScheduleManager();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.server.ISiteWhereServer#getTenantGroovyConfiguration(
     * com.sitewhere.spi.tenant.ITenant)
     */
    @Override
    public ITenantGroovyConfiguration getTenantGroovyConfiguration(ITenant tenant) throws SiteWhereException {
	ISiteWhereTenantEngine engine = assureTenantEngine(tenant);
	return engine.getGroovyConfiguration();
    }

    /**
     * Get tenant engine for tenant. Throw an exception if not found.
     * 
     * @param tenant
     * @return
     * @throws SiteWhereException
     */
    protected ISiteWhereTenantEngine assureTenantEngine(ITenant tenant) throws SiteWhereException {
	ISiteWhereTenantEngine engine = tenantEnginesById.get(tenant.getId());
	if (engine == null) {
	    throw new SiteWhereException("No engine registered for tenant.");
	}
	return engine;
    }

    /**
     * Get a tenant engine by authentication token. Throw and exception if not
     * found.
     * 
     * @param tenantAuthToken
     * @return
     * @throws SiteWhereException
     */
    protected ISiteWhereTenantEngine assureTenantEngine(String tenantAuthToken) throws SiteWhereException {
	ITenant tenant = tenantsByAuthToken.get(tenantAuthToken);
	if (tenant == null) {
	    throw new SiteWhereSystemException(ErrorCode.InvalidTenantAuthToken, ErrorLevel.ERROR);
	}
	ISiteWhereTenantEngine engine = tenantEnginesById.get(tenant.getId());
	if (engine == null) {
	    throw new SiteWhereException("Tenant found for auth token, but no engine registered for tenant.");
	}
	return engine;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.server.ISiteWhereServer#getTenantByAuthToken(java.lang.
     * String)
     */
    public ITenant getTenantByAuthToken(String authToken) throws SiteWhereException {
	return tenantsByAuthToken.get(authToken);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getMetricRegistry()
     */
    public MetricRegistry getMetricRegistry() {
	return metricRegistry;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.ISiteWhereServer#getHealthCheckRegistry()
     */
    public HealthCheckRegistry getHealthCheckRegistry() {
	return healthCheckRegistry;
    }

    /**
     * Returns a fake account used for operations on the data model done by the
     * system.
     * 
     * @return
     * @throws SiteWhereException
     */
    public static SitewhereAuthentication getSystemAuthentication() throws SiteWhereException {
	User fake = new User();
	fake.setUsername("system");
	SitewhereUserDetails details = new SitewhereUserDetails(fake, new ArrayList<IGrantedAuthority>());
	SitewhereAuthentication auth = new SitewhereAuthentication(details, null);
	return auth;
    }

    /**
     * Access folder pointed to by SiteWhere home environment variable.
     * 
     * @return
     * @throws SiteWhereException
     */
    public static File getSiteWhereHomeFolder() throws SiteWhereException {
	String sitewhere = System.getProperty(ISiteWhereServer.ENV_SITEWHERE_HOME);
	if (sitewhere == null) {
	    // Support fallback environment variable name.
	    sitewhere = System.getProperty("SITEWHERE_HOME");
	    if (sitewhere == null) {
		throw new SiteWhereException(
			"SiteWhere home environment variable (" + ISiteWhereServer.ENV_SITEWHERE_HOME + ") not set.");
	    }
	}
	File swFolder = new File(sitewhere);
	if (!swFolder.exists()) {
	    throw new SiteWhereException(
		    "SiteWhere home folder does not exist. Looking in: " + swFolder.getAbsolutePath());
	}
	return swFolder;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.server.lifecycle.LifecycleComponent#start(com.sitewhere.spi
     * .server.lifecycle.ILifecycleProgressMonitor)
     */
    @Override
    public void start(ILifecycleProgressMonitor monitor) throws SiteWhereException {

	// Organizes steps for starting server.
	ICompositeLifecycleStep start = new CompositeLifecycleStep("START SERVER");

	// Initialize tenant engines.
	start.addStep(new SimpleLifecycleStep("Preparing server") {

	    @Override
	    public void execute(ILifecycleProgressMonitor monitor) throws SiteWhereException {
		// Handle backward compatibility.
		backwardCompatibilityService.beforeServerStart(monitor);

		// Clear the component list.
		getLifecycleComponents().clear();
	    }
	});

	// Start base service services.
	startBaseServices(start);

	// Start management implementations.
	startManagementImplementations(start);

	// Start tenants.
	startTenants(start);

	// Finish operations for starting server.
	start.addStep(new SimpleLifecycleStep("Finishing server startup") {

	    @Override
	    public void execute(ILifecycleProgressMonitor monitor) throws SiteWhereException {
		// Force refresh on components-by-id map.
		lifecycleComponentsById = buildComponentMap();

		// Set uptime timestamp.
		SiteWhereServer.this.uptime = System.currentTimeMillis();

		// Schedule JVM monitor.
		executor = Executors.newFixedThreadPool(2);
		executor.execute(jvmHistory);

		// If version checker configured, perform in a separate thread.
		if (versionChecker != null) {
		    executor.execute(versionChecker);
		}

		// Handle backward compatibility.
		backwardCompatibilityService.afterServerStart(monitor);
	    }
	});

	// Start the operation and report progress.
	start.execute(monitor);
    }

    /**
     * Start basic services required by other components.
     * 
     * @param start
     * @throws SiteWhereException
     */
    protected void startBaseServices(ICompositeLifecycleStep start) throws SiteWhereException {
	// Start the Hazelcast instance.
	start.addStep(new StartComponentLifecycleStep(this, getHazelcastConfiguration(), "Starting Hazelcast instance",
		"Hazelcast startup failed.", true));

	// Start the Groovy configuration.
	start.addStep(new StartComponentLifecycleStep(this, getGroovyConfiguration(),
		"Starting Groovy scripting engine", "Groovy startup failed.", true));

	// Start all lifecycle components.
	for (ILifecycleComponent component : getRegisteredLifecycleComponents()) {
	    start.addStep(new StartComponentLifecycleStep(this, component, "Starting " + component.getComponentName(),
		    component.getComponentName() + " startup failed.", true));
	}

	// Start the tenant template manager.
	start.addStep(new StartComponentLifecycleStep(this, getTenantTemplateManager(),
		"Starting tenant template manager", "Tenant template manager startup failed.", true));
    }

    /**
     * Initialize and start tenants.
     * 
     * @param start
     * @throws SiteWhereException
     */
    protected void startTenants(ICompositeLifecycleStep start) throws SiteWhereException {
	// Initialize tenant engines.
	start.addStep(new SimpleLifecycleStep("Starting tenant engines") {

	    @Override
	    public void execute(ILifecycleProgressMonitor monitor) throws SiteWhereException {
		initializeTenantEngines();

		// Create thread pool for starting tenants in parallel.
		if (tenantStarters != null) {
		    tenantStarters.shutdownNow();
		}
		tenantStarters = Executors.newFixedThreadPool(TENANT_STARTUP_PARALLELISM);

		// Start tenant engines.
		for (ISiteWhereTenantEngine engine : tenantEnginesById.values()) {

		    // Find state or create initial state as needed.
		    ITenantPersistentState state = engine.getPersistentState();
		    if (state == null) {
			TenantPersistentState newState = new TenantPersistentState();
			newState.setDesiredState(LifecycleStatus.Started);
			newState.setLastKnownState(LifecycleStatus.Starting);
			engine.persistState(newState);
			state = newState;
		    }

		    // Do not start if desired state is 'Stopped'.
		    if (state.getDesiredState() != LifecycleStatus.Stopped) {
			tenantStarters.execute(new Runnable() {

			    @Override
			    public void run() {
				try {
				    startTenantEngine(engine);
				} catch (SiteWhereException e) {
				    LOGGER.error("Tenant engine startup failed.", e);
				}
			    }
			});
		    }
		}
	    }
	});
    }

    /**
     * Add management implementation startup to composite operation.
     * 
     * @param start
     * @throws SiteWhereException
     */
    protected void startManagementImplementations(ICompositeLifecycleStep start) throws SiteWhereException {
	// Start user management.
	start.addStep(new StartComponentLifecycleStep(this, getUserManagement(),
		"Starting user management implementation", "User management startup failed.", true));

	// Start tenant management.
	start.addStep(new StartComponentLifecycleStep(this, getTenantManagement(),
		"Starting tenant management implementation", "Tenant management startup failed", true));

	// Populate user data if requested.
	start.addStep(new SimpleLifecycleStep("Verifying user model") {

	    @Override
	    public void execute(ILifecycleProgressMonitor monitor) throws SiteWhereException {
		verifyUserModel();
	    }
	});

	// Populate tenant data if requested.
	start.addStep(new SimpleLifecycleStep("Verifying tenant model") {

	    @Override
	    public void execute(ILifecycleProgressMonitor monitor) throws SiteWhereException {
		verifyTenantModel();
	    }
	});
    }

    /**
     * Start a tenant engine.
     * 
     * @param engine
     * @throws SiteWhereException
     */
    protected void startTenantEngine(ISiteWhereTenantEngine engine) throws SiteWhereException {
	if (engine.getLifecycleStatus() != LifecycleStatus.Error) {
	    LifecycleProgressMonitor monitor = new LifecycleProgressMonitor();
	    startNestedComponent(engine, monitor,
		    "Tenant engine '" + engine.getTenant().getName() + "' startup failed.", false);
	    engine.logState();
	} else {
	    getLifecycleComponents().put(engine.getComponentId(), engine);
	    LOGGER.info("Skipping startup for tenant engine '" + engine.getTenant().getName()
		    + "' due to initialization errors.");
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.lifecycle.ILifecycleComponent#getLogger()
     */
    @Override
    public Logger getLogger() {
	return LOGGER;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.server.lifecycle.LifecycleComponent#getComponentName()
     */
    @Override
    public String getComponentName() {
	return "SiteWhere Server " + getVersion().getEditionIdentifier() + " " + getVersion().getVersionIdentifier();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.lifecycle.ILifecycleHierarchyRoot#
     * getRegisteredLifecycleComponents()
     */
    public List<ILifecycleComponent> getRegisteredLifecycleComponents() {
	return registeredLifecycleComponents;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.lifecycle.ILifecycleHierarchyRoot#
     * getLifecycleComponentById(java.lang.String)
     */
    @Override
    public ILifecycleComponent getLifecycleComponentById(String id) {
	return lifecycleComponentsById.get(id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.server.lifecycle.LifecycleComponent#stop(com.sitewhere.spi.
     * server.lifecycle.ILifecycleProgressMonitor)
     */
    @Override
    public void stop(ILifecycleProgressMonitor monitor) throws SiteWhereException {
	// Organizes steps for stopping server.
	ICompositeLifecycleStep stop = new CompositeLifecycleStep("STOP SERVER");

	// Shut down services.
	stop.addStep(new SimpleLifecycleStep("Shutting down services") {

	    @Override
	    public void execute(ILifecycleProgressMonitor monitor) throws SiteWhereException {
		if (executor != null) {
		    executor.shutdownNow();
		    executor = null;
		}
	    }
	});

	// Stop tenant engines.
	stopTenantEngines(stop);

	// Stop management implementations.
	stopManagementImplementations(stop);

	// Stop base server services.
	stopBaseServices(stop);

	// Execute stop operation and report progress.
	stop.execute(monitor);
    }

    /**
     * Stop base server services.
     * 
     * @param stop
     * @throws SiteWhereException
     */
    protected void stopBaseServices(ICompositeLifecycleStep stop) throws SiteWhereException {
	// Stop all lifecycle components.
	for (ILifecycleComponent component : getRegisteredLifecycleComponents()) {
	    stop.addStep(new StopComponentLifecycleStep(this, component, "Stop " + component.getComponentName()));
	}

	// Stop the tenant template manager.
	stop.addStep(new StopComponentLifecycleStep(this, getTenantTemplateManager(), "Stop tenant template manager"));

	// Stop the Groovy configuration.
	stop.addStep(new StopComponentLifecycleStep(this, getGroovyConfiguration(), "Stop Groovy script engine"));

	// Stop the Hazelcast instance.
	stop.addStep(new StopComponentLifecycleStep(this, getHazelcastConfiguration(), "Stop Hazelcast instance"));
    }

    /**
     * Stop management implementations.
     * 
     * @param monitor
     * @throws SiteWhereException
     */
    protected void stopManagementImplementations(ICompositeLifecycleStep stop) throws SiteWhereException {
	// Stop tenant management implementation.
	stop.addStep(new StopComponentLifecycleStep(this, getTenantManagement(), "Stop tenant management"));

	// Stop user management implementation.
	stop.addStep(new StopComponentLifecycleStep(this, getUserManagement(), "Stop user management"));
    }

    /**
     * Stop tenant engines.
     * 
     * @param stop
     * @throws SiteWhereException
     */
    protected void stopTenantEngines(ICompositeLifecycleStep stop) throws SiteWhereException {
	stop.addStep(new SimpleLifecycleStep("Stopping tenant engines") {

	    @Override
	    public void execute(ILifecycleProgressMonitor monitor) throws SiteWhereException {
		for (ISiteWhereTenantEngine engine : tenantEnginesById.values()) {
		    if (engine.getLifecycleStatus() == LifecycleStatus.Started) {
			engine.lifecycleStop(monitor);
		    }
		}
	    }
	});
    }

    /**
     * Compute current server state.
     * 
     * @return
     * @throws SiteWhereException
     */
    protected ISiteWhereServerRuntime computeServerRuntime(boolean includeHistorical) throws SiteWhereException {
	SiteWhereServerRuntime state = new SiteWhereServerRuntime();

	String osName = System.getProperty("os.name");
	String osVersion = System.getProperty("os.version");
	String javaVendor = System.getProperty("java.vendor");
	String javaVersion = System.getProperty("java.version");

	GeneralInformation general = new GeneralInformation();
	general.setEdition(getVersion().getEdition());
	general.setEditionIdentifier(getVersion().getEditionIdentifier());
	general.setVersionIdentifier(getVersion().getVersionIdentifier());
	general.setBuildTimestamp(getVersion().getBuildTimestamp());
	general.setUptime(System.currentTimeMillis() - uptime);
	general.setOperatingSystemName(osName);
	general.setOperatingSystemVersion(osVersion);
	state.setGeneral(general);

	JavaInformation java = new JavaInformation();
	java.setJvmVendor(javaVendor);
	java.setJvmVersion(javaVersion);
	state.setJava(java);

	Runtime runtime = Runtime.getRuntime();
	java.setJvmFreeMemory(runtime.freeMemory());
	java.setJvmTotalMemory(runtime.totalMemory());
	java.setJvmMaxMemory(runtime.maxMemory());

	if (includeHistorical) {
	    java.setJvmTotalMemoryHistory(jvmHistory.getTotalMemory());
	    java.setJvmFreeMemoryHistory(jvmHistory.getFreeMemory());
	}

	return state;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.server.ISiteWhereServer#initialize(com.sitewhere.spi.
     * server.lifecycle.ILifecycleProgressMonitor)
     */
    public void initialize(ILifecycleProgressMonitor monitor) throws SiteWhereException {
	// Handle backward compatibility.
	backwardCompatibilityService.beforeServerInitialize(monitor);

	// Set version information.
	this.version = VersionHelper.getVersion();

	// Initialize bootstrap resource manager.
	initializeBootstrapResourceManager();
	LOGGER.info("Bootstrap resources loading from: " + getBootstrapResourceManager().getClass().getCanonicalName());
	getBootstrapResourceManager().start(monitor);

	// Initialize persistent state.
	initializeServerState();

	// Initialize the Hazelcast instance.
	initializeHazelcastConfiguration();

	// Initialize the Groovy configuration.
	initializeGroovyConfiguration();

	// Initialize Spring.
	initializeSpringContext();

	// Initialize discoverable beans.
	initializeDiscoverableBeans(monitor);

	// Initialize version checker.
	initializeVersionChecker();

	// Initialize tracer.
	initializeTracer();

	// Initialize management implementations.
	initializeManagementImplementations();

	// Initialize runtime resource manager.
	initializeRuntimeResourceManager();
	LOGGER.info("Runtime resources loading from: " + getRuntimeResourceManager().getClass().getCanonicalName());
	getRuntimeResourceManager().start(monitor);

	// Initialize tenant template manager.
	initializeTenantTemplateManager();

	// Show banner containing server information.
	showServerBanner();
    }

    /**
     * Displays the server information banner in the log.
     */
    protected void showServerBanner() {
	String os = System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")";
	String java = System.getProperty("java.vendor") + " (" + System.getProperty("java.version") + ")";

	// Print version information.
	List<String> messages = new ArrayList<String>();
	messages.add("SiteWhere Server " + version.getEdition());
	messages.add("Version: " + version.getVersionIdentifier() + "." + version.getBuildTimestamp());
	messages.add("Node id: " + serverState.getNodeId());
	addBannerMessages(messages);
	messages.add("Operating System: " + os);
	messages.add("Java Runtime: " + java);
	messages.add("");
	messages.add("Copyright (c) 2009-2016 SiteWhere, LLC");
	String message = Boilerplate.boilerplate(messages, '*', 60);
	LOGGER.info("\n" + message + "\n");
    }

    /**
     * Allows subclasses to add their own banner messages.
     * 
     * @param messages
     */
    protected void addBannerMessages(List<String> messages) {
	messages.add("");
    }

    /**
     * Initialize the server state information.
     * 
     * @throws SiteWhereException
     */
    protected void initializeServerState() throws SiteWhereException {
	IResource stateResource = getConfigurationResolver().resolveServerState(getVersion());
	SiteWhereServerState state = null;
	if (stateResource == null) {
	    state = new SiteWhereServerState();
	    state.setNodeId(UUID.randomUUID().toString());
	    getConfigurationResolver().storeServerState(version, MarshalUtils.marshalJson(state));
	} else {
	    state = MarshalUtils.unmarshalJson(stateResource.getContent(), SiteWhereServerState.class);
	}
	this.serverState = state;
    }

    /**
     * Initialize Hazelcast configuration.
     * 
     * @throws SiteWhereException
     */
    protected void initializeHazelcastConfiguration() throws SiteWhereException {
	IResource resource = getBootstrapResourceManager().getGlobalResource(HazelcastConfiguration.CONFIG_FILE_NAME);
	if (resource == null) {
	    throw new SiteWhereException(
		    "Base Hazelcast configuration resource not found: " + HazelcastConfiguration.CONFIG_FILE_NAME);
	}
	this.hazelcastConfiguration = new HazelcastConfiguration(resource);
    }

    /**
     * Initialize the Groovy configuration.
     * 
     * @throws SiteWhereException
     */
    protected void initializeGroovyConfiguration() throws SiteWhereException {
	this.groovyConfiguration = new GroovyConfiguration();
    }

    /**
     * Verifies and loads the Spring configuration file.
     * 
     * @throws SiteWhereException
     */
    protected void initializeSpringContext() throws SiteWhereException {
	IResource global = getConfigurationResolver().getGlobalConfiguration(getVersion());
	SERVER_SPRING_CONTEXT = ConfigurationUtils.buildGlobalContext(global, getVersion());
    }

    /**
     * Initialize beans marked with
     * {@link IDiscoverableTenantLifecycleComponent} interface and add them as
     * registered components.
     * 
     * @param monitor
     * @throws SiteWhereException
     */
    protected void initializeDiscoverableBeans(ILifecycleProgressMonitor monitor) throws SiteWhereException {
	Map<String, IDiscoverableTenantLifecycleComponent> components = SERVER_SPRING_CONTEXT
		.getBeansOfType(IDiscoverableTenantLifecycleComponent.class);
	getRegisteredLifecycleComponents().clear();

	LOGGER.info("Registering " + components.size() + " discoverable components.");
	for (IDiscoverableTenantLifecycleComponent component : components.values()) {
	    LOGGER.info("Registering " + component.getComponentName() + ".");
	    initializeNestedComponent(component, monitor);
	    getRegisteredLifecycleComponents().add(component);
	}
    }

    /**
     * Initialize debug tracing implementation.
     * 
     * @throws SiteWhereException
     */
    protected void initializeVersionChecker() throws SiteWhereException {
	try {
	    this.versionChecker = (IVersionChecker) SERVER_SPRING_CONTEXT
		    .getBean(SiteWhereServerBeans.BEAN_VERSION_CHECK);
	} catch (NoSuchBeanDefinitionException e) {
	    LOGGER.info("Version checking not enabled.");
	}
    }

    /**
     * Initialize debug tracing implementation.
     * 
     * @throws SiteWhereException
     */
    protected void initializeTracer() throws SiteWhereException {
	try {
	    this.tracer = (ITracer) SERVER_SPRING_CONTEXT.getBean(SiteWhereServerBeans.BEAN_TRACER);
	    LOGGER.info("Tracer implementation using: " + tracer.getClass().getName());
	} catch (NoSuchBeanDefinitionException e) {
	    LOGGER.info("No Tracer implementation configured.");
	    this.tracer = new NullTracer();
	}
    }

    /**
     * Initialize the tenant template manager.
     * 
     * @throws SiteWhereException
     */
    protected void initializeTenantTemplateManager() throws SiteWhereException {
	this.tenantTemplateManager = new TenantTemplateManager(getRuntimeResourceManager());
    }

    /**
     * Initialize management implementations.
     * 
     * @throws SiteWhereException
     */
    protected void initializeManagementImplementations() throws SiteWhereException {

	// Initialize user management.
	initializeUserManagement();

	// Initialize tenant management.
	initializeTenantManagement();
    }

    /**
     * Verify and initialize user management implementation.
     * 
     * @throws SiteWhereException
     */
    protected void initializeUserManagement() throws SiteWhereException {
	try {
	    this.userManagement = (IUserManagement) SERVER_SPRING_CONTEXT
		    .getBean(SiteWhereServerBeans.BEAN_USER_MANAGEMENT);
	} catch (NoSuchBeanDefinitionException e) {
	    throw new SiteWhereException("No user management implementation configured.");
	}
    }

    /**
     * Verify and initialize tenant management implementation.
     * 
     * @throws SiteWhereException
     */
    protected void initializeTenantManagement() throws SiteWhereException {
	try {
	    ITenantManagement implementation = (ITenantManagement) SERVER_SPRING_CONTEXT
		    .getBean(SiteWhereServerBeans.BEAN_TENANT_MANAGEMENT);
	    this.tenantManagement = new TenantManagementTriggers(implementation);
	} catch (NoSuchBeanDefinitionException e) {
	    throw new SiteWhereException("No user management implementation configured.");
	}
    }

    /**
     * Initialize bootstrap resource manager.
     * 
     * @throws SiteWhereException
     */
    protected void initializeBootstrapResourceManager() throws SiteWhereException {
	this.bootstrapResourceManager = new SiteWhereHomeResourceManager();
	this.configurationResolver = new ResourceManagerGlobalConfigurationResolver(bootstrapResourceManager);
    }

    /**
     * Initialize runtime resource manager and swap configuration resolver to
     * use it.
     * 
     * @throws SiteWhereException
     */
    protected void initializeRuntimeResourceManager() throws SiteWhereException {
	this.runtimeResourceManager = new SiteWhereHomeResourceManager();
	this.configurationResolver = new ResourceManagerGlobalConfigurationResolver(runtimeResourceManager);
    }

    /**
     * Create and initialize all tenant engines.
     * 
     * @throws SiteWhereException
     */
    protected void initializeTenantEngines() throws SiteWhereException {
	TenantSearchCriteria criteria = new TenantSearchCriteria(1, 0);
	ISearchResults<ITenant> tenants = getTenantManagement().listTenants(criteria);
	for (ITenant tenant : tenants.getResults()) {
	    initializeTenantEngine(tenant);
	}
    }

    /**
     * Initialize a tenant engine.
     * 
     * @param tenant
     * @return
     * @throws SiteWhereException
     */
    protected ISiteWhereTenantEngine initializeTenantEngine(ITenant tenant) throws SiteWhereException {
	ISiteWhereTenantEngine engine = createTenantEngine(tenant, SERVER_SPRING_CONTEXT, getConfigurationResolver());
	initializeNestedComponent(engine, new LifecycleProgressMonitor());
	if (engine.getLifecycleStatus() != LifecycleStatus.Error) {
	    registerTenant(tenant, engine);
	    return engine;
	}
	return null;
    }

    /**
     * Create a tenant engine.
     * 
     * @param tenant
     * @param parent
     * @param resolver
     * @return
     * @throws SiteWhereException
     */
    protected ISiteWhereTenantEngine createTenantEngine(ITenant tenant, ApplicationContext parent,
	    IGlobalConfigurationResolver resolver) throws SiteWhereException {
	return new SiteWhereTenantEngine(tenant, SERVER_SPRING_CONTEXT, getConfigurationResolver());
    }

    /**
     * Registers an initialized tenant engine with the server.
     * 
     * @param tenant
     * @param engine
     * @throws SiteWhereException
     */
    protected void registerTenant(ITenant tenant, ISiteWhereTenantEngine engine) throws SiteWhereException {
	tenantsByAuthToken.put(tenant.getAuthenticationToken(), tenant);
	tenantEnginesById.put(tenant.getId(), engine);
    }

    /**
     * Check whether user model is populated and bootstrap system if not.
     */
    protected void verifyUserModel() {
	try {
	    IUserModelInitializer init = (IUserModelInitializer) SERVER_SPRING_CONTEXT
		    .getBean(SiteWhereServerBeans.BEAN_USER_MODEL_INITIALIZER);
	    init.initialize(getUserManagement());
	} catch (NoSuchBeanDefinitionException e) {
	    LOGGER.info("No user model initializer found in Spring bean configuration. Skipping.");
	    return;
	} catch (SiteWhereException e) {
	    LOGGER.warn("Error verifying user model.", e);
	}
    }

    /**
     * Check whether tenant model is populated and bootstrap system if not.
     */
    protected void verifyTenantModel() {
	try {
	    ITenantModelInitializer init = (ITenantModelInitializer) SERVER_SPRING_CONTEXT
		    .getBean(SiteWhereServerBeans.BEAN_TENANT_MODEL_INITIALIZER);
	    init.initialize(getTenantManagement());
	} catch (NoSuchBeanDefinitionException e) {
	    LOGGER.info("No tenant model initializer found in Spring bean configuration. Skipping.");
	    return;
	} catch (SiteWhereException e) {
	    LOGGER.warn("Error verifying tenant model.", e);
	}
    }
}