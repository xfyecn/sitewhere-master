/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.server.asset;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sitewhere.SiteWhere;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.asset.IAsset;
import com.sitewhere.spi.asset.IAssetManagement;
import com.sitewhere.spi.asset.IAssetModule;
import com.sitewhere.spi.asset.IAssetModuleManager;
import com.sitewhere.spi.asset.IHardwareAsset;
import com.sitewhere.spi.asset.ILocationAsset;
import com.sitewhere.spi.asset.IPersonAsset;
import com.sitewhere.spi.asset.request.IHardwareAssetCreateRequest;
import com.sitewhere.spi.asset.request.ILocationAssetCreateRequest;
import com.sitewhere.spi.asset.request.IPersonAssetCreateRequest;
import com.sitewhere.spi.server.lifecycle.LifecycleStatus;

/**
 * Trigger actions based on asset management API calls.
 * 
 * @author Derek
 */
public class AssetManagementTriggers extends AssetManagementDecorator {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    public AssetManagementTriggers(IAssetManagement delegate) {
	super(delegate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.server.asset.AssetManagementDecorator#createPersonAsset(
     * java.lang .String,
     * com.sitewhere.spi.asset.request.IPersonAssetCreateRequest)
     */
    @Override
    public IPersonAsset createPersonAsset(String categoryId, IPersonAssetCreateRequest request)
	    throws SiteWhereException {
	IPersonAsset asset = super.createPersonAsset(categoryId, request);
	putForAssetModule(categoryId, asset);
	return asset;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.server.asset.AssetManagementDecorator#updatePersonAsset(
     * java.lang .String, java.lang.String,
     * com.sitewhere.spi.asset.request.IPersonAssetCreateRequest)
     */
    @Override
    public IPersonAsset updatePersonAsset(String categoryId, String assetId, IPersonAssetCreateRequest request)
	    throws SiteWhereException {
	IPersonAsset asset = super.updatePersonAsset(categoryId, assetId, request);
	putForAssetModule(categoryId, asset);
	return asset;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.server.asset.AssetManagementDecorator#createHardwareAsset(
     * java.lang .String,
     * com.sitewhere.spi.asset.request.IHardwareAssetCreateRequest)
     */
    @Override
    public IHardwareAsset createHardwareAsset(String categoryId, IHardwareAssetCreateRequest request)
	    throws SiteWhereException {
	IHardwareAsset asset = super.createHardwareAsset(categoryId, request);
	putForAssetModule(categoryId, asset);
	return asset;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.server.asset.AssetManagementDecorator#updateHardwareAsset(
     * java.lang .String, java.lang.String,
     * com.sitewhere.spi.asset.request.IHardwareAssetCreateRequest)
     */
    @Override
    public IHardwareAsset updateHardwareAsset(String categoryId, String assetId, IHardwareAssetCreateRequest request)
	    throws SiteWhereException {
	IHardwareAsset asset = super.updateHardwareAsset(categoryId, assetId, request);
	putForAssetModule(categoryId, asset);
	return asset;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.server.asset.AssetManagementDecorator#createLocationAsset(
     * java.lang .String,
     * com.sitewhere.spi.asset.request.ILocationAssetCreateRequest)
     */
    @Override
    public ILocationAsset createLocationAsset(String categoryId, ILocationAssetCreateRequest request)
	    throws SiteWhereException {
	ILocationAsset asset = super.createLocationAsset(categoryId, request);
	putForAssetModule(categoryId, asset);
	return asset;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.server.asset.AssetManagementDecorator#updateLocationAsset(
     * java.lang .String, java.lang.String,
     * com.sitewhere.spi.asset.request.ILocationAssetCreateRequest)
     */
    @Override
    public ILocationAsset updateLocationAsset(String categoryId, String assetId, ILocationAssetCreateRequest request)
	    throws SiteWhereException {
	ILocationAsset asset = super.updateLocationAsset(categoryId, assetId, request);
	putForAssetModule(categoryId, asset);
	return asset;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.server.asset.AssetManagementDecorator#deleteAsset(java.lang
     * .String, java.lang.String)
     */
    @Override
    public IAsset deleteAsset(String categoryId, String assetId) throws SiteWhereException {
	IAsset asset = super.deleteAsset(categoryId, assetId);
	deleteForAssetModule(categoryId, asset);
	return asset;
    }

    /**
     * Push an updated asset to its associated asset module.
     * 
     * @param categoryId
     * @param asset
     * @throws SiteWhereException
     */
    @SuppressWarnings("unchecked")
    protected <T extends IAsset> void putForAssetModule(String categoryId, T asset) throws SiteWhereException {
	IAssetModuleManager manager = SiteWhere.getServer().getAssetModuleManager(getTenant());
	if (manager.getLifecycleStatus() == LifecycleStatus.Started) {
	    IAssetModule<T> module = (IAssetModule<T>) SiteWhere.getServer().getAssetModuleManager(getTenant())
		    .getModule(categoryId);
	    if (module != null) {
		module.putAsset(asset.getId(), asset);
		LOGGER.info("Pushing asset update for '" + asset.getName() + "' to asset module '" + categoryId + "'.");
	    }
	}
    }

    /**
     * Delete an asset from its associated asset module.
     * 
     * @param categoryId
     * @param asset
     * @throws SiteWhereException
     */
    @SuppressWarnings("unchecked")
    protected <T extends IAsset> void deleteForAssetModule(String categoryId, T asset) throws SiteWhereException {
	IAssetModuleManager manager = SiteWhere.getServer().getAssetModuleManager(getTenant());
	if (manager.getLifecycleStatus() == LifecycleStatus.Started) {
	    IAssetModule<T> module = (IAssetModule<T>) SiteWhere.getServer().getAssetModuleManager(getTenant())
		    .getModule(categoryId);
	    if (module != null) {
		module.removeAsset(asset.getId());
		LOGGER.info("Pushing asset delete for '" + asset.getName() + "' to asset module '" + categoryId + "'.");
	    }
	}
    }
}