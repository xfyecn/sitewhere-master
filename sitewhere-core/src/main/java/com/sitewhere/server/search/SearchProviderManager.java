/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.server.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sitewhere.server.lifecycle.LifecycleComponent;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.search.external.ISearchProvider;
import com.sitewhere.spi.search.external.ISearchProviderManager;
import com.sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor;
import com.sitewhere.spi.server.lifecycle.LifecycleComponentType;

/**
 * Manages a list of {@link ISearchProvider} that are available for querying
 * device events
 * 
 * @author Derek
 */
public class SearchProviderManager extends LifecycleComponent implements ISearchProviderManager {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /** List of available search providers */
    private List<ISearchProvider> searchProviders = new ArrayList<ISearchProvider>();

    /** Map of search providers by id */
    private Map<String, ISearchProvider> providersById = new HashMap<String, ISearchProvider>();

    public SearchProviderManager() {
	super(LifecycleComponentType.SearchProviderManager);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.lifecycle.ILifecycleComponent#start(com.
     * sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor)
     */
    @Override
    public void start(ILifecycleProgressMonitor monitor) throws SiteWhereException {
	for (ISearchProvider provider : getSearchProviders()) {
	    provider.lifecycleStart(monitor);
	    providersById.put(provider.getId(), provider);
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
     * @see
     * com.sitewhere.spi.server.lifecycle.ILifecycleComponent#stop(com.sitewhere
     * .spi.server.lifecycle.ILifecycleProgressMonitor)
     */
    @Override
    public void stop(ILifecycleProgressMonitor monitor) throws SiteWhereException {
	for (ISearchProvider provider : getSearchProviders()) {
	    provider.lifecycleStop(monitor);
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.search.external.ISearchProviderManager#
     * getSearchProviders()
     */
    public List<ISearchProvider> getSearchProviders() {
	return searchProviders;
    }

    public void setSearchProviders(List<ISearchProvider> searchProviders) {
	this.searchProviders = searchProviders;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.search.external.ISearchProviderManager#
     * getSearchProvider(java .lang.String)
     */
    @Override
    public ISearchProvider getSearchProvider(String id) {
	return providersById.get(id);
    }
}