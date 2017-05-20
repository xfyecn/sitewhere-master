/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.web.rest.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sitewhere.SiteWhere;
import com.sitewhere.Tracer;
import com.sitewhere.rest.model.asset.AssetModule;
import com.sitewhere.rest.model.asset.request.AssetCategoryCreateRequest;
import com.sitewhere.rest.model.asset.request.HardwareAssetCreateRequest;
import com.sitewhere.rest.model.asset.request.LocationAssetCreateRequest;
import com.sitewhere.rest.model.asset.request.PersonAssetCreateRequest;
import com.sitewhere.rest.model.search.SearchCriteria;
import com.sitewhere.rest.model.search.SearchResults;
import com.sitewhere.rest.model.search.device.AssignmentsForAssetSearchCriteria;
import com.sitewhere.server.lifecycle.LifecycleProgressMonitor;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.SiteWhereSystemException;
import com.sitewhere.spi.asset.AssetType;
import com.sitewhere.spi.asset.IAsset;
import com.sitewhere.spi.asset.IAssetCategory;
import com.sitewhere.spi.asset.IAssetModule;
import com.sitewhere.spi.asset.IHardwareAsset;
import com.sitewhere.spi.asset.ILocationAsset;
import com.sitewhere.spi.asset.IPersonAsset;
import com.sitewhere.spi.device.DeviceAssignmentStatus;
import com.sitewhere.spi.device.IDeviceAssignment;
import com.sitewhere.spi.error.ErrorCode;
import com.sitewhere.spi.error.ErrorLevel;
import com.sitewhere.spi.search.ISearchResults;
import com.sitewhere.spi.server.debug.TracerCategory;
import com.sitewhere.spi.user.SiteWhereRoles;
import com.sitewhere.web.rest.RestController;
import com.sitewhere.web.rest.annotations.Concerns;
import com.sitewhere.web.rest.annotations.Concerns.ConcernType;
import com.sitewhere.web.rest.annotations.Documented;
import com.sitewhere.web.rest.annotations.DocumentedController;
import com.sitewhere.web.rest.annotations.Example;
import com.sitewhere.web.rest.annotations.Example.Stage;
import com.sitewhere.web.rest.documentation.Assets;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * Controller for site operations.
 * 
 * @author Derek Adams
 */
@Controller
@CrossOrigin
@RequestMapping(value = "/assets")
@Api(value = "assets", description = "Operations related to SiteWhere assets.")
@DocumentedController(name = "Asset Management")
public class AssetsController extends RestController {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /**
     * Search for assets in an {@link IAssetModule} that meet the given
     * criteria.
     * 
     * @param assetModuleId
     * @param criteria
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/modules/{assetModuleId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "Get an asset module by unique id")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assets.GetAssetModuleResponse.class, description = "assets/get-asset-module-response.md") })
    public AssetModule getAssetModule(
	    @ApiParam(value = "Unique asset module id", required = true) @PathVariable String assetModuleId,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "getAssetModule", LOGGER);
	try {
	    return AssetModule.copy(
		    SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest)).getModule(assetModuleId));
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Search for assets in an {@link IAssetModule} that meet the given
     * criteria.
     * 
     * @param assetModuleId
     * @param criteria
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/modules/{assetModuleId}/assets", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "Search for assets in an asset module")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assets.SearchAssetModuleResponse.class, description = "assets/search-asset-module-response.md") })
    public SearchResults<? extends IAsset> searchAssets(
	    @ApiParam(value = "Unique asset module id", required = true) @PathVariable String assetModuleId,
	    @ApiParam(value = "Criteria for search", required = false) @RequestParam(defaultValue = "") String criteria,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "searchAssets", LOGGER);
	List<? extends IAsset> found = SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest))
		.search(assetModuleId, criteria);
	SearchResults<? extends IAsset> results = new SearchResults(found);
	Tracer.stop(LOGGER);
	return results;
    }

    /**
     * Get an asset from an {@link IAssetModule} by unique id.
     * 
     * @param assetModuleId
     * @param assetId
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/modules/{assetModuleId}/assets/{assetId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "Get an asset by unique id")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assets.GetAssetByIdPersonResponse.class, description = "getAssetByIdPersonResponse.md"),
	    @Example(stage = Stage.Response, json = Assets.GetAssetByIdHardwareResponse.class, description = "getAssetByIdHardwareResponse.md"),
	    @Example(stage = Stage.Response, json = Assets.GetAssetByIdLocationResponse.class, description = "getAssetByIdLocationResponse.md") })
    public IAsset getAssetById(
	    @ApiParam(value = "Unique asset module id", required = true) @PathVariable String assetModuleId,
	    @ApiParam(value = "Unique asset id", required = true) @PathVariable String assetId,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "getAssetById", LOGGER);
	try {
	    return SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest)).getAssetById(assetModuleId,
		    assetId);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Get all assignments for a given asset.
     * 
     * @param assetModuleId
     * @param assetId
     * @param siteToken
     * @param page
     * @param pageSize
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/modules/{assetModuleId}/assets/{assetId}/assignments", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List assignments associated with an asset")
    @Secured({ SiteWhereRoles.REST })
    @Concerns(values = { ConcernType.Paging })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assets.GetAssignmentsForAsset.class, description = "getAssignmentsForAsset.md") })
    public ISearchResults<IDeviceAssignment> getAssignmentsForAsset(
	    @ApiParam(value = "Unique asset module id", required = true) @PathVariable String assetModuleId,
	    @ApiParam(value = "Unique asset id", required = true) @PathVariable String assetId,
	    @ApiParam(value = "Limit results to the given site", required = false) @RequestParam(required = false) String siteToken,
	    @ApiParam(value = "Limit results to the given status", required = false) @RequestParam(required = false) String status,
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "getAssetById", LOGGER);
	try {
	    AssignmentsForAssetSearchCriteria criteria = new AssignmentsForAssetSearchCriteria(page, pageSize);
	    DeviceAssignmentStatus decodedStatus = (status != null) ? DeviceAssignmentStatus.valueOf(status) : null;
	    criteria.setStatus(decodedStatus);
	    criteria.setSiteToken(siteToken);

	    return SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		    .getDeviceAssignmentsForAsset(assetModuleId, assetId, criteria);
	} catch (IllegalArgumentException e) {
	    throw new SiteWhereException("Invalid device assignment status: " + status);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * List all asset modules.
     * 
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/modules", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List asset modules that match criteria")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assets.ListAssetModules.class, description = "listAssetModules.md") })
    public List<AssetModule> listAssetModules(
	    @ApiParam(value = "Asset type", required = false) @RequestParam(required = false) String assetType,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listAssetModules", LOGGER);
	try {
	    AssetType type = (assetType == null) ? null : AssetType.valueOf(assetType);
	    List<AssetModule> converted = new ArrayList<AssetModule>();
	    List<IAssetModule<?>> modules = SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest))
		    .listModules();
	    for (IAssetModule<?> module : modules) {
		if ((type == null) || (type == module.getAssetType())) {
		    converted.add(AssetModule.copy(module));
		}
	    }
	    return converted;
	} catch (IllegalArgumentException e) {
	    throw new SiteWhereSystemException(ErrorCode.UnknownAssetType, ErrorLevel.ERROR);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Refresh all asset modules.
     * 
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/modules/refresh", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Refresh the list of asset modules")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assets.RefreshAssetModules.class, description = "refreshModules.md") })
    public void refreshModules(HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "refreshModules", LOGGER);
	try {
	    LifecycleProgressMonitor monitor = new LifecycleProgressMonitor();
	    SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest)).refreshModules(monitor);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Create a new asset category.
     * 
     * @param request
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/categories", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Create a new asset category")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assets.CreateAssetCategoryRequest.class, description = "createAssetCategoryRequest.md") })
    public IAssetCategory createAssetCategory(@RequestBody AssetCategoryCreateRequest request,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "createAssetCategory", LOGGER);
	try {
	    return SiteWhere.getServer().getAssetManagement(getTenant(servletRequest)).createAssetCategory(request);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Update an existing asset category.
     * 
     * @param categoryId
     * @param request
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/categories/{categoryId}", method = RequestMethod.PUT)
    @ResponseBody
    @ApiOperation(value = "Update an existing asset category")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assets.UpdateAssetCategoryRequest.class, description = "updateAssetCategoryRequest.md") })
    public IAssetCategory updateAssetCategory(
	    @ApiParam(value = "Unique category id", required = true) @PathVariable String categoryId,
	    @RequestBody AssetCategoryCreateRequest request, HttpServletRequest servletRequest)
	    throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "updateAssetCategory", LOGGER);
	try {
	    return SiteWhere.getServer().getAssetManagement(getTenant(servletRequest)).updateAssetCategory(categoryId,
		    request);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Get an asset category by unique id.
     * 
     * @param categoryId
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/categories/{categoryId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "Get an asset category by unique id")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assets.GetAssetCategoryByIdResponse.class, description = "getAssetCategoryByIdResponse.md") })
    public IAssetCategory getAssetCategoryById(
	    @ApiParam(value = "Unique category id", required = true) @PathVariable String categoryId,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "getAssetCategoryById", LOGGER);
	try {
	    return SiteWhere.getServer().getAssetManagement(getTenant(servletRequest)).getAssetCategory(categoryId);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Delete an existing asset category.
     * 
     * @param categoryId
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/categories/{categoryId}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "Delete an existing asset category")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assets.GetAssetCategoryByIdResponse.class, description = "deleteAssetCategoryByIdResponse.md") })
    public IAssetCategory deleteAssetCategory(
	    @ApiParam(value = "Unique category id", required = true) @PathVariable String categoryId,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "deleteAssetCategory", LOGGER);
	try {
	    return SiteWhere.getServer().getAssetManagement(getTenant(servletRequest)).deleteAssetCategory(categoryId);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * List asset categories that match the given search criteria.
     * 
     * @param page
     * @param pageSize
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/categories", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List asset categories that match criteria")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assets.ListAssetCategories.class, description = "assets/listAssetCategoriesResponse.md") })
    public ISearchResults<IAssetCategory> listAssetCategories(
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listAssetCategories", LOGGER);
	try {
	    SearchCriteria criteria = new SearchCriteria(page, pageSize);
	    return SiteWhere.getServer().getAssetManagement(getTenant(servletRequest)).listAssetCategories(criteria);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Creates a new person asset in the category. If the category does not
     * support person assets, an exception will be thrown.
     * 
     * @param categoryId
     * @param request
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/categories/{categoryId}/persons", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Create a new person asset in category")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assets.CreatePersonAssetInCategory.class, description = "createPersonAssetRequest.md") })
    public IPersonAsset createPersonAsset(
	    @ApiParam(value = "Unique category id", required = true) @PathVariable String categoryId,
	    @RequestBody PersonAssetCreateRequest request, HttpServletRequest servletRequest)
	    throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "createPersonAsset", LOGGER);
	try {
	    return SiteWhere.getServer().getAssetManagement(getTenant(servletRequest)).createPersonAsset(categoryId,
		    request);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Update an existing person asset.
     * 
     * @param categoryId
     * @param assetId
     * @param request
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/categories/{categoryId}/persons/{assetId}", method = RequestMethod.PUT)
    @ResponseBody
    @ApiOperation(value = "Update an existing person asset in category")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assets.UpdatePersonAssetInCategory.class, description = "updatePersonAssetRequest.md") })
    public IPersonAsset updatePersonAsset(
	    @ApiParam(value = "Unique category id", required = true) @PathVariable String categoryId,
	    @ApiParam(value = "Unique asset id", required = true) @PathVariable String assetId,
	    @RequestBody PersonAssetCreateRequest request, HttpServletRequest servletRequest)
	    throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "updatePersonAsset", LOGGER);
	try {
	    return SiteWhere.getServer().getAssetManagement(getTenant(servletRequest)).updatePersonAsset(categoryId,
		    assetId, request);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Creates a new hardware asset in the category. If the category does not
     * support hardware assets, an exception will be thrown.
     * 
     * @param categoryId
     * @param request
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/categories/{categoryId}/hardware", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Create a new hardware asset in category")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assets.CreateHardwareAssetInCategory.class, description = "createHardwareAssetRequest.md") })
    public IHardwareAsset createHardwareAsset(
	    @ApiParam(value = "Unique category id", required = true) @PathVariable String categoryId,
	    @RequestBody HardwareAssetCreateRequest request, HttpServletRequest servletRequest)
	    throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "createHardwareAsset", LOGGER);
	try {
	    return SiteWhere.getServer().getAssetManagement(getTenant(servletRequest)).createHardwareAsset(categoryId,
		    request);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Update an existing hardware asset.
     * 
     * @param categoryId
     * @param assetId
     * @param request
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/categories/{categoryId}/hardware/{assetId}", method = RequestMethod.PUT)
    @ResponseBody
    @ApiOperation(value = "Update an existing hardware asset in category")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assets.UpdateHardwareAssetInCategory.class, description = "updateHardwareAssetRequest.md") })
    public IHardwareAsset updateHardwareAsset(
	    @ApiParam(value = "Unique category id", required = true) @PathVariable String categoryId,
	    @ApiParam(value = "Unique asset id", required = true) @PathVariable String assetId,
	    @RequestBody HardwareAssetCreateRequest request, HttpServletRequest servletRequest)
	    throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "updateHardwareAsset", LOGGER);
	try {
	    return SiteWhere.getServer().getAssetManagement(getTenant(servletRequest)).updateHardwareAsset(categoryId,
		    assetId, request);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Creates a new location asset in the category. If the category does not
     * support location assets, an exception will be thrown.
     * 
     * @param categoryId
     * @param request
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/categories/{categoryId}/locations", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Create a new location asset in category")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assets.CreateLocationAssetInCategory.class, description = "createLocationAssetRequest.md") })
    public ILocationAsset createLocationAsset(
	    @ApiParam(value = "Unique category id", required = true) @PathVariable String categoryId,
	    @RequestBody LocationAssetCreateRequest request, HttpServletRequest servletRequest)
	    throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "createLocationAsset", LOGGER);
	try {
	    return SiteWhere.getServer().getAssetManagement(getTenant(servletRequest)).createLocationAsset(categoryId,
		    request);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Update an existing location asset.
     * 
     * @param categoryId
     * @param assetId
     * @param request
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/categories/{categoryId}/locations/{assetId}", method = RequestMethod.PUT)
    @ResponseBody
    @ApiOperation(value = "Update an existing location asset in category")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assets.UpdateLocationAssetInCategory.class, description = "updateLocationAssetRequest.md") })
    public ILocationAsset updateLocationAsset(
	    @ApiParam(value = "Unique category id", required = true) @PathVariable String categoryId,
	    @ApiParam(value = "Unique asset id", required = true) @PathVariable String assetId,
	    @RequestBody LocationAssetCreateRequest request, HttpServletRequest servletRequest)
	    throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "updateLocationAsset", LOGGER);
	try {
	    return SiteWhere.getServer().getAssetManagement(getTenant(servletRequest)).updateLocationAsset(categoryId,
		    assetId, request);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Get an asset from a category by unique id.
     * 
     * @param categoryId
     * @param assetId
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/categories/{categoryId}/assets/{assetId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "Get a category asset by unique id")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assets.GetCategoryAssetPerson.class, description = "getCategoryAssetPersonResponse.md"),
	    @Example(stage = Stage.Response, json = Assets.GetCategoryAssetHardware.class, description = "getCategoryAssetHardwareResponse.md"),
	    @Example(stage = Stage.Response, json = Assets.GetCategoryAssetLocation.class, description = "getCategoryAssetLocationResponse.md") })
    public IAsset getCategoryAsset(
	    @ApiParam(value = "Unique category id", required = true) @PathVariable String categoryId,
	    @ApiParam(value = "Unique asset id", required = true) @PathVariable String assetId,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "getCategoryAsset", LOGGER);
	try {
	    return SiteWhere.getServer().getAssetManagement(getTenant(servletRequest)).getAsset(categoryId, assetId);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Delete an asset from a category based on unique id.
     * 
     * @param categoryId
     * @param assetId
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/categories/{categoryId}/assets/{assetId}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "Delete an existing category asset")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assets.GetCategoryAssetPerson.class, description = "deleteCategoryAssetPersonResponse.md"),
	    @Example(stage = Stage.Response, json = Assets.GetCategoryAssetHardware.class, description = "deleteCategoryAssetHardwareResponse.md"),
	    @Example(stage = Stage.Response, json = Assets.GetCategoryAssetLocation.class, description = "deleteCategoryAssetLocationResponse.md") })
    public IAsset deleteCategoryAsset(
	    @ApiParam(value = "Unique category id", required = true) @PathVariable String categoryId,
	    @ApiParam(value = "Unique asset id", required = true) @PathVariable String assetId,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "deleteCategoryAsset", LOGGER);
	try {
	    return SiteWhere.getServer().getAssetManagement(getTenant(servletRequest)).deleteAsset(categoryId, assetId);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * List all assets for a given category.
     * 
     * @param categoryId
     * @param page
     * @param pageSize
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/categories/{categoryId}/assets", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List category assets that match criteria")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assets.ListCategoryAssets.class, description = "listCategoryAssetsResponse.md") })
    public ISearchResults<IAsset> listCategoryAssets(
	    @ApiParam(value = "Unique category id", required = true) @PathVariable String categoryId,
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listCategoryAssets", LOGGER);
	try {
	    SearchCriteria criteria = new SearchCriteria(page, pageSize);
	    return SiteWhere.getServer().getAssetManagement(getTenant(servletRequest)).listAssets(categoryId, criteria);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }
}