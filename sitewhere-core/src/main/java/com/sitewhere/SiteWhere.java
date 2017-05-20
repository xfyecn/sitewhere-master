/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere;

import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.server.ISiteWhereApplication;
import com.sitewhere.spi.server.ISiteWhereServer;
import com.sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor;
import com.sitewhere.spi.server.lifecycle.LifecycleStatus;

/**
 * Main class for accessing core SiteWhere functionality.
 * 
 * @author Derek
 */
public class SiteWhere {

    /** Singleton server instance */
    private static ISiteWhereServer SERVER;

    /**
     * Called once to bootstrap the SiteWhere server.
     * 
     * @param application
     * @param monitor
     * @throws SiteWhereException
     */
    public static void start(ISiteWhereApplication application, ILifecycleProgressMonitor monitor)
	    throws SiteWhereException {
	Class<? extends ISiteWhereServer> clazz = application.getServerClass();
	try {
	    SERVER = clazz.newInstance();
	    SERVER.initialize(monitor);
	    SERVER.lifecycleStart(monitor);

	    // Handle errors that prevent server startup.
	    if (SERVER.getLifecycleStatus() == LifecycleStatus.Error) {
		throw SERVER.getLifecycleError();
	    }
	} catch (InstantiationException e) {
	    throw new SiteWhereException("Unable to create SiteWhere server instance.", e);
	} catch (IllegalAccessException e) {
	    throw new SiteWhereException("Unable to access SiteWhere server class.", e);
	}
    }

    /**
     * Called to shut down the SiteWhere server.
     * 
     * @param monitor
     * @throws SiteWhereException
     */
    public static void stop(ILifecycleProgressMonitor monitor) throws SiteWhereException {
	getServer().lifecycleStop(monitor);

	// Handle errors that prevent server shutdown.
	if (SERVER.getLifecycleStatus() == LifecycleStatus.Error) {
	    throw SERVER.getLifecycleError();
	}
    }

    /**
     * Get the singleton SiteWhere server instance.
     * 
     * @return
     */
    public static ISiteWhereServer getServer() {
	if (SERVER == null) {
	    throw new RuntimeException("SiteWhere server has not been initialized.");
	}
	return SERVER;
    }

    /**
     * Determine whether server is available.
     * 
     * @return
     */
    public static boolean isServerAvailable() {
	return ((SERVER != null && (SERVER.getLifecycleStatus() == LifecycleStatus.Started)));
    }
}