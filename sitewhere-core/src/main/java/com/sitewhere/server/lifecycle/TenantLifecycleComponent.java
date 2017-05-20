/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.server.lifecycle;

import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.server.lifecycle.ILifecycleComponent;
import com.sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor;
import com.sitewhere.spi.server.lifecycle.ITenantLifecycleComponent;
import com.sitewhere.spi.server.lifecycle.LifecycleComponentType;
import com.sitewhere.spi.tenant.ITenant;

/**
 * Base class for implementing {@link ITenantLifecycleComponent}.
 * 
 * @author Derek
 */
public abstract class TenantLifecycleComponent extends LifecycleComponent implements ITenantLifecycleComponent {

    public TenantLifecycleComponent(LifecycleComponentType type) {
	super(type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.server.lifecycle.LifecycleComponent#
     * initializeNestedComponent(com.sitewhere.spi.server.lifecycle.
     * ILifecycleComponent,
     * com.sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor)
     */
    @Override
    public void initializeNestedComponent(ILifecycleComponent component, ILifecycleProgressMonitor monitor)
	    throws SiteWhereException {
	if (component instanceof ITenantLifecycleComponent) {
	    ((ITenantLifecycleComponent) component).setTenant(getTenant());
	}
	super.initializeNestedComponent(component, monitor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.server.lifecycle.LifecycleComponent#startNestedComponent(
     * com.sitewhere.spi.server.lifecycle.ILifecycleComponent,
     * com.sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor,
     * java.lang.String, boolean)
     */
    @Override
    public void startNestedComponent(ILifecycleComponent component, ILifecycleProgressMonitor monitor,
	    String errorMessage, boolean require) throws SiteWhereException {
	if (component instanceof ITenantLifecycleComponent) {
	    ((ITenantLifecycleComponent) component).setTenant(getTenant());
	}
	super.startNestedComponent(component, monitor, errorMessage, require);
    }

    /** Tenant associated with component */
    private ITenant tenant;

    public ITenant getTenant() {
	return tenant;
    }

    public void setTenant(ITenant tenant) {
	this.tenant = tenant;
    }
}