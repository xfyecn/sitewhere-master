/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.server.tenant;

import java.util.concurrent.Callable;

import com.sitewhere.spi.command.ICommandResponse;
import com.sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor;
import com.sitewhere.spi.server.tenant.ISiteWhereTenantEngine;

/**
 * Base class for commands executed on a tenant engine.
 * 
 * @author Derek
 */
public abstract class TenantEngineCommand implements Callable<ICommandResponse> {

    /** Progress monitor for commands */
    private ILifecycleProgressMonitor progressMonitor;

    /** Tenant engine */
    private ISiteWhereTenantEngine engine;

    public ILifecycleProgressMonitor getProgressMonitor() {
	return progressMonitor;
    }

    public void setProgressMonitor(ILifecycleProgressMonitor progressMonitor) {
	this.progressMonitor = progressMonitor;
    }

    public ISiteWhereTenantEngine getEngine() {
	return engine;
    }

    public void setEngine(ISiteWhereTenantEngine engine) {
	this.engine = engine;
    }
}