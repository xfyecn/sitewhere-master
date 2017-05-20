/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.rest.model.tenant.request.scripting;

import java.util.ArrayList;
import java.util.List;

import com.sitewhere.rest.model.tenant.request.TenantCreateRequest;
import com.sitewhere.rest.model.tenant.request.TenantGroupCreateRequest;
import com.sitewhere.rest.model.tenant.request.TenantGroupElementCreateRequest;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.configuration.IDefaultResourcePaths;
import com.sitewhere.spi.tenant.ITenant;
import com.sitewhere.spi.tenant.ITenantGroup;
import com.sitewhere.spi.tenant.ITenantGroupElement;
import com.sitewhere.spi.tenant.ITenantManagement;
import com.sitewhere.spi.tenant.request.ITenantGroupElementCreateRequest;

/**
 * Builder that supports creating tenant management entities.
 * 
 * @author Derek
 */
public class TenantManagementRequestBuilder {

    /** Device management implementation */
    private ITenantManagement tenantManagement;

    public TenantManagementRequestBuilder(ITenantManagement tenantManagement) {
	this.tenantManagement = tenantManagement;
    }

    /**
     * Create builder for new tenant request. Assumes tenant uses the "empty"
     * tenant template.
     * 
     * @param id
     * @param name
     * @param authenticationToken
     * @param logoUrl
     * @return
     */
    public TenantCreateRequest.Builder newTenant(String id, String name, String authenticationToken, String logoUrl) {
	return newTenant(id, name, authenticationToken, logoUrl, IDefaultResourcePaths.EMPTY_TEMPLATE_NAME);
    }

    /**
     * Create builder for new tenant request. Allows tenant template to be
     * specified.
     * 
     * @param id
     * @param name
     * @param authenticationToken
     * @param logoUrl
     * @param tenantTemplateId
     * @return
     */
    public TenantCreateRequest.Builder newTenant(String id, String name, String authenticationToken, String logoUrl,
	    String tenantTemplateId) {
	return new TenantCreateRequest.Builder(id, name, authenticationToken, logoUrl, tenantTemplateId);
    }

    /**
     * Persist tenant contructed via builder.
     * 
     * @param builder
     * @return
     * @throws SiteWhereException
     */
    public ITenant persist(TenantCreateRequest.Builder builder) throws SiteWhereException {
	return getTenantManagement().createTenant(builder.build());
    }

    /**
     * Get tenant by unique id.
     * 
     * @param id
     * @return
     * @throws SiteWhereException
     */
    public ITenant getTenant(String id) throws SiteWhereException {
	return getTenantManagement().getTenantById(id);
    }

    /**
     * Indicates whether a tenant exists for the given id.
     * 
     * @param id
     * @return
     * @throws SiteWhereException
     */
    public boolean hasTenant(String id) throws SiteWhereException {
	return getTenant(id) != null;
    }

    /**
     * Create builder for new tenant group request. This version allows a token
     * to be auto-generated.
     * 
     * @param name
     * @return
     */
    public TenantGroupCreateRequest.Builder newTenantGroup(String name) {
	return new TenantGroupCreateRequest.Builder(name);
    }

    /**
     * Create builder for new tenant group request.
     * 
     * @param token
     * @param name
     * @return
     */
    public TenantGroupCreateRequest.Builder newTenantGroup(String token, String name) {
	return new TenantGroupCreateRequest.Builder(token, name);
    }

    /**
     * Persist tenant group contructed via builder.
     * 
     * @param builder
     * @return
     * @throws SiteWhereException
     */
    public ITenantGroup persist(TenantGroupCreateRequest.Builder builder) throws SiteWhereException {
	return getTenantManagement().createTenantGroup(builder.build());
    }

    /**
     * Get a tenant group by token.
     * 
     * @param token
     * @return
     * @throws SiteWhereException
     */
    public ITenantGroup getTenantGroup(String token) throws SiteWhereException {
	return getTenantManagement().getTenantGroupByToken(token);
    }

    /**
     * Indicates whether a tenant group exists for the given id.
     * 
     * @param id
     * @return
     * @throws SiteWhereException
     */
    public boolean hasTenantGroup(String id) throws SiteWhereException {
	return getTenantGroup(id) != null;
    }

    /**
     * Create builder for a tenant group element.
     * 
     * @param tenantGroupId
     * @param tenantId
     * @return
     */
    public TenantGroupElementCreateRequest.Builder newTenantGroupElement(String tenantId) {
	return new TenantGroupElementCreateRequest.Builder(tenantId);
    }

    /**
     * Add elements to a tenant group.
     * 
     * @param group
     * @param builders
     * @return
     * @throws SiteWhereException
     */
    public List<ITenantGroupElement> persist(ITenantGroup group, List<TenantGroupElementCreateRequest.Builder> builders)
	    throws SiteWhereException {
	List<ITenantGroupElementCreateRequest> elements = new ArrayList<ITenantGroupElementCreateRequest>();
	for (TenantGroupElementCreateRequest.Builder builder : builders) {
	    elements.add(builder.build());
	}
	return getTenantManagement().addTenantGroupElements(group.getToken(), elements);
    }

    public ITenantManagement getTenantManagement() {
	return tenantManagement;
    }

    public void setTenantManagement(ITenantManagement tenantManagement) {
	this.tenantManagement = tenantManagement;
    }
}