/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.configuration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sitewhere.SiteWhere;
import com.sitewhere.rest.model.resource.request.ResourceCreateRequest;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.configuration.IDefaultResourcePaths;
import com.sitewhere.spi.configuration.IGlobalConfigurationResolver;
import com.sitewhere.spi.configuration.ITenantConfigurationResolver;
import com.sitewhere.spi.resource.IMultiResourceCreateResponse;
import com.sitewhere.spi.resource.IResource;
import com.sitewhere.spi.resource.IResourceCreateError;
import com.sitewhere.spi.resource.IResourceManager;
import com.sitewhere.spi.resource.ResourceCreateMode;
import com.sitewhere.spi.resource.ResourceType;
import com.sitewhere.spi.resource.request.IResourceCreateRequest;
import com.sitewhere.spi.system.IVersion;
import com.sitewhere.spi.tenant.ITenant;

/**
 * Resolves tenant configuration settings via the filesystem.
 * 
 * @author Derek
 */
public class ResourceManagerTenantConfigurationResolver implements ITenantConfigurationResolver {

    /** Static logger instance */
    public static Logger LOGGER = LogManager.getLogger();

    /** Folder containing tenant asset resources */
    public static final String ASSETS_FOLDER = "assets";

    /** Folder containing tenant script resources */
    public static final String SCRIPTS_FOLDER = "scripts";

    /** Filename for tenant configuration information */
    public static final String DEFAULT_TENANT_CONFIGURATION_FILE = "sitewhere-tenant";

    /** Suffix for an active tenant configuration */
    public static final String TENANT_SUFFIX_ACTIVE = "xml";

    /** Suffix for a staged tenant configuration */
    public static final String TENANT_SUFFIX_STAGED = "staged";

    /** Suffix for a backup tenant configuration */
    public static final String TENANT_SUFFIX_BACKUP = "backup";

    /** Tenant */
    private ITenant tenant;

    /** Version information */
    private IVersion version;

    /** Global configuration resolver */
    private IGlobalConfigurationResolver globalConfigurationResolver;

    /** Resource manager implementation */
    private IResourceManager resourceManager;

    public ResourceManagerTenantConfigurationResolver(ITenant tenant, IVersion version,
	    IGlobalConfigurationResolver globalConfigurationResolver) {
	this.tenant = tenant;
	this.version = version;
	this.globalConfigurationResolver = globalConfigurationResolver;
	this.resourceManager = SiteWhere.getServer().getRuntimeResourceManager();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.configuration.ITenantConfigurationResolver#
     * hasValidConfiguration()
     */
    @Override
    public boolean hasValidConfiguration() {
	try {
	    return getActiveTenantConfiguration() != null;
	} catch (SiteWhereException e) {
	    LOGGER.warn("Error checking for valid tenant configuration.", e);
	    return false;
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.configuration.ITenantConfigurationResolver#
     * hasStagedConfiguration()
     */
    @Override
    public boolean hasStagedConfiguration() {
	try {
	    return getStagedTenantConfiguration() != null;
	} catch (SiteWhereException e) {
	    LOGGER.warn("Error checking for staged tenant configuration.", e);
	    return false;
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.configuration.ITenantConfigurationResolver#
     * getResourceForPath(java.lang.String)
     */
    @Override
    public IResource getResourceForPath(String path) throws SiteWhereException {
	return getResourceManager().getTenantResource(getTenant().getId(), path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.configuration.ITenantConfigurationResolver#
     * getAssetResource(java.lang.String)
     */
    @Override
    public IResource getAssetResource(String path) throws SiteWhereException {
	return getResourceManager().getTenantResource(getTenant().getId(), ASSETS_FOLDER + File.separator + path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.configuration.ITenantConfigurationResolver#
     * getScriptResource(java.lang.String)
     */
    @Override
    public IResource getScriptResource(String path) throws SiteWhereException {
	return getResourceManager().getTenantResource(getTenant().getId(), SCRIPTS_FOLDER + File.separator + path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.configuration.ITenantConfigurationResolver#
     * getActiveTenantConfiguration()
     */
    @Override
    public IResource getActiveTenantConfiguration() throws SiteWhereException {
	String path = DEFAULT_TENANT_CONFIGURATION_FILE + "." + TENANT_SUFFIX_ACTIVE;
	IResource resource = getResourceManager().getTenantResource(getTenant().getId(), path);
	if (resource == null) {
	    LOGGER.warn("Active tenant configuration resource not found. " + "Searched for tenant: "
		    + getTenant().getId() + " Path: " + path);
	}
	return resource;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.configuration.ITenantConfigurationResolver#
     * getStagedTenantConfiguration()
     */
    @Override
    public IResource getStagedTenantConfiguration() throws SiteWhereException {
	return getResourceManager().getTenantResource(getTenant().getId(),
		DEFAULT_TENANT_CONFIGURATION_FILE + "." + TENANT_SUFFIX_STAGED);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.configuration.ITenantConfigurationResolver#
     * stageTenantConfiguration(byte[])
     */
    @Override
    public IResource stageTenantConfiguration(byte[] content) throws SiteWhereException {
	List<IResourceCreateRequest> requests = new ArrayList<IResourceCreateRequest>();
	ResourceCreateRequest request = new ResourceCreateRequest();
	request.setPath(DEFAULT_TENANT_CONFIGURATION_FILE + "." + TENANT_SUFFIX_STAGED);
	request.setResourceType(ResourceType.ConfigurationFile);
	request.setContent(content);
	requests.add(request);
	IMultiResourceCreateResponse response = getResourceManager().createTenantResources(getTenant().getId(),
		requests, ResourceCreateMode.PUSH_NEW_VERSION);
	if (response.getErrors().size() > 0) {
	    IResourceCreateError error = response.getErrors().get(0);
	    throw new SiteWhereException("Unable to stage tenant configuration: " + error.getReason().name());
	}
	if (response.getCreatedResources().size() < 1) {
	    throw new SiteWhereException("No resources returned from staging operation.");
	}
	return response.getCreatedResources().get(0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.configuration.ITenantConfigurationResolver#
     * copyTenantTemplateResources()
     */
    @Override
    public IResource copyTenantTemplateResources() throws SiteWhereException {
	String tenantId = getTenant().getId();

	// Account for missing template id (backward compatibility).
	String templateId = getTenant().getTenantTemplateId();
	if (templateId == null) {
	    templateId = IDefaultResourcePaths.EMPTY_TEMPLATE_NAME;
	}

	IMultiResourceCreateResponse response = getResourceManager().copyTemplateResourcesToTenant(templateId, tenantId,
		ResourceCreateMode.OVERWRITE);
	if (response.getErrors().size() > 0) {
	    LOGGER.warn("Errors encountered while copying template to tenant.");
	    for (IResourceCreateError error : response.getErrors()) {
		LOGGER.warn("Error copying: " + error.getPath() + ". Reason: " + error.getReason().name());
	    }
	}
	LOGGER.info("Created configuration for '" + tenantId + "' based on " + templateId + "' template.");

	return getActiveTenantConfiguration();
    }

    /**
     * Create a default properties file for a tenant.
     * 
     * @param tenant
     * @param folder
     * @throws SiteWhereException
     */
    protected void createTenantPropertiesFile(ITenant tenant, File folder) throws SiteWhereException {
	File tenantPropsFile = new File(folder, tenant.getId() + "-tenant.properties");
	try {
	    tenantPropsFile.createNewFile();
	    String content = "# Properties for '" + tenant.getName() + "' tenant configuration.\n";
	    ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes());
	    FileOutputStream out = new FileOutputStream(tenantPropsFile);
	    IOUtils.copy(in, out);
	    IOUtils.closeQuietly(in);
	    IOUtils.closeQuietly(out);
	} catch (IOException e) {
	    LOGGER.error("Unable to copy tenant configuration file: " + tenantPropsFile.getAbsolutePath(), e);
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.configuration.ITenantConfigurationResolver#
     * transitionStagedToActiveTenantConfiguration()
     */
    @Override
    public void transitionStagedToActiveTenantConfiguration() throws SiteWhereException {
	IResource staged = getStagedTenantConfiguration();
	IResource active = getActiveTenantConfiguration();

	List<IResourceCreateRequest> requests = new ArrayList<IResourceCreateRequest>();
	ResourceCreateRequest request = new ResourceCreateRequest();
	request.setPath(active.getPath());
	request.setResourceType(ResourceType.ConfigurationFile);
	request.setContent(staged.getContent());
	requests.add(request);
	IMultiResourceCreateResponse response = getResourceManager().createTenantResources(getTenant().getId(),
		requests, ResourceCreateMode.PUSH_NEW_VERSION);
	if (response.getErrors().size() > 0) {
	    IResourceCreateError error = response.getErrors().get(0);
	    throw new SiteWhereException("Unable to stage tenant configuration: " + error.getReason().name());
	}
	if (response.getCreatedResources().size() < 1) {
	    throw new SiteWhereException("No resources returned from staging operation.");
	}

	getResourceManager().deleteTenantResource(getTenant().getId(), staged.getPath());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.configuration.ITenantConfigurationResolver#
     * getGlobalConfigurationResolver()
     */
    public IGlobalConfigurationResolver getGlobalConfigurationResolver() {
	return globalConfigurationResolver;
    }

    public void setGlobalConfigurationResolver(IGlobalConfigurationResolver globalConfigurationResolver) {
	this.globalConfigurationResolver = globalConfigurationResolver;
    }

    public ITenant getTenant() {
	return tenant;
    }

    public void setTenant(ITenant tenant) {
	this.tenant = tenant;
    }

    public IVersion getVersion() {
	return version;
    }

    public void setVersion(IVersion version) {
	this.version = version;
    }

    public IResourceManager getResourceManager() {
	return resourceManager;
    }

    public void setResourceManager(IResourceManager resourceManager) {
	this.resourceManager = resourceManager;
    }
}