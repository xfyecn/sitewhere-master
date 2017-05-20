/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.web.rest.controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import com.sitewhere.device.communication.symbology.DefaultEntityUriProvider;
import com.sitewhere.device.group.DeviceGroupUtils;
import com.sitewhere.device.marshaling.DeviceAssignmentMarshalHelper;
import com.sitewhere.device.marshaling.DeviceMarshalHelper;
import com.sitewhere.rest.model.device.DeviceElementMapping;
import com.sitewhere.rest.model.device.event.DeviceEventBatch;
import com.sitewhere.rest.model.device.event.request.DeviceAlertCreateRequest;
import com.sitewhere.rest.model.device.event.request.DeviceLocationCreateRequest;
import com.sitewhere.rest.model.device.event.request.DeviceMeasurementsCreateRequest;
import com.sitewhere.rest.model.device.request.DeviceCreateRequest;
import com.sitewhere.rest.model.search.SearchCriteria;
import com.sitewhere.rest.model.search.SearchResults;
import com.sitewhere.rest.model.search.device.DeviceSearchCriteria;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.SiteWhereSystemException;
import com.sitewhere.spi.device.IDevice;
import com.sitewhere.spi.device.IDeviceAssignment;
import com.sitewhere.spi.device.event.IDeviceEventBatchResponse;
import com.sitewhere.spi.device.event.request.IDeviceAlertCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceLocationCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceMeasurementsCreateRequest;
import com.sitewhere.spi.device.symbology.IEntityUriProvider;
import com.sitewhere.spi.device.symbology.ISymbolGenerator;
import com.sitewhere.spi.device.symbology.ISymbolGeneratorManager;
import com.sitewhere.spi.error.ErrorCode;
import com.sitewhere.spi.error.ErrorLevel;
import com.sitewhere.spi.search.ISearchResults;
import com.sitewhere.spi.search.device.IDeviceSearchCriteria;
import com.sitewhere.spi.server.debug.TracerCategory;
import com.sitewhere.spi.user.SiteWhereRoles;
import com.sitewhere.web.rest.RestController;
import com.sitewhere.web.rest.annotations.Concerns;
import com.sitewhere.web.rest.annotations.Concerns.ConcernType;
import com.sitewhere.web.rest.annotations.Documented;
import com.sitewhere.web.rest.annotations.DocumentedController;
import com.sitewhere.web.rest.annotations.Example;
import com.sitewhere.web.rest.annotations.Example.Stage;
import com.sitewhere.web.rest.documentation.Devices;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * Controller for device operations.
 * 
 * @author Derek Adams
 */
@Controller
@CrossOrigin
@RequestMapping(value = "/devices")
@Api(value = "devices", description = "Operations related to SiteWhere devices.")
@DocumentedController(name = "Devices")
public class DevicesController extends RestController {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /**
     * Create a device.
     * 
     * @param request
     * @return
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Create new device")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Devices.CreateDeviceRequest.class, description = "createDeviceGroupRequest.md"),
	    @Example(stage = Stage.Response, json = Devices.CreateDeviceResponse.class, description = "createDeviceGroupResponse.md") })
    public IDevice createDevice(@RequestBody DeviceCreateRequest request, HttpServletRequest servletRequest)
	    throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "createDevice", LOGGER);
	try {
	    IDevice result = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest)).createDevice(request);
	    DeviceMarshalHelper helper = new DeviceMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(false);
	    helper.setIncludeAssignment(false);
	    return helper.convert(result, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest)));
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Used by AJAX calls to find a device by hardware id.
     * 
     * @param hardwareId
     * @return
     */
    @RequestMapping(value = "/{hardwareId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "Get device by unique hardware id")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Devices.GetDeviceByHardwareIdResponse.class, description = "getDeviceByHardwareIdResponse.md") })
    public IDevice getDeviceByHardwareId(
	    @ApiParam(value = "Hardware id", required = true) @PathVariable String hardwareId,
	    @ApiParam(value = "Include specification information", required = false) @RequestParam(defaultValue = "true") boolean includeSpecification,
	    @ApiParam(value = "Include assignment if associated", required = false) @RequestParam(defaultValue = "true") boolean includeAssignment,
	    @ApiParam(value = "Include site information", required = false) @RequestParam(defaultValue = "true") boolean includeSite,
	    @ApiParam(value = "Include detailed asset information", required = false) @RequestParam(defaultValue = "true") boolean includeAsset,
	    @ApiParam(value = "Include detailed nested device information", required = false) @RequestParam(defaultValue = "false") boolean includeNested,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "getDeviceByHardwareId", LOGGER);
	try {
	    IDevice result = assertDeviceByHardwareId(hardwareId, servletRequest);
	    DeviceMarshalHelper helper = new DeviceMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeSpecification(includeSpecification);
	    helper.setIncludeAsset(includeAsset);
	    helper.setIncludeAssignment(includeAssignment);
	    helper.setIncludeSite(includeSite);
	    helper.setIncludeNested(includeNested);
	    return helper.convert(result, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest)));
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Update device information.
     * 
     * @param hardwareId
     *            unique hardware id
     * @param request
     *            updated information
     * @return the updated device
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{hardwareId}", method = RequestMethod.PUT)
    @ResponseBody
    @ApiOperation(value = "Update an existing device")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Devices.UpdateDeviceRequest.class, description = "updateDeviceRequest.md"),
	    @Example(stage = Stage.Response, json = Devices.UpdateDeviceResponse.class, description = "updateDeviceResponse.md") })
    public IDevice updateDevice(@ApiParam(value = "Hardware id", required = true) @PathVariable String hardwareId,
	    @RequestBody DeviceCreateRequest request, HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "updateDevice", LOGGER);
	try {
	    IDevice result = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		    .updateDevice(hardwareId, request);
	    DeviceMarshalHelper helper = new DeviceMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(true);
	    helper.setIncludeAssignment(true);
	    return helper.convert(result, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest)));
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Delete device identified by hardware id.
     * 
     * @param hardwareId
     * @return
     */
    @RequestMapping(value = "/{hardwareId}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "Delete device based on unique hardware id")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Devices.CreateDeviceResponse.class, description = "deleteDeviceResponse.md") })
    public IDevice deleteDevice(@ApiParam(value = "Hardware id", required = true) @PathVariable String hardwareId,
	    @ApiParam(value = "Delete permanently", required = false) @RequestParam(defaultValue = "false") boolean force,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "deleteDevice", LOGGER);
	try {
	    IDevice result = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		    .deleteDevice(hardwareId, force);
	    DeviceMarshalHelper helper = new DeviceMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(true);
	    helper.setIncludeAssignment(true);
	    return helper.convert(result, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest)));
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * List device assignment history for a given device hardware id.
     * 
     * @param hardwareId
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{hardwareId}/assignment", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "Get current assignment for device")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Devices.GetCurrentDeviceAssignmentResponse.class, description = "getDeviceCurrentAssignmentResponse.md") })
    public IDeviceAssignment getDeviceCurrentAssignment(
	    @ApiParam(value = "Hardware id", required = true) @PathVariable String hardwareId,
	    @ApiParam(value = "Include detailed asset information", required = false) @RequestParam(defaultValue = "true") boolean includeAsset,
	    @ApiParam(value = "Include detailed device information", required = false) @RequestParam(defaultValue = "false") boolean includeDevice,
	    @ApiParam(value = "Include detailed site information", required = false) @RequestParam(defaultValue = "true") boolean includeSite,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "getDeviceCurrentAssignment", LOGGER);
	try {
	    IDevice device = assertDeviceByHardwareId(hardwareId, servletRequest);
	    IDeviceAssignment assignment = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		    .getCurrentDeviceAssignment(device);
	    if (assignment == null) {
		throw new SiteWhereSystemException(ErrorCode.DeviceNotAssigned, ErrorLevel.INFO,
			HttpServletResponse.SC_NOT_FOUND);
	    }
	    DeviceAssignmentMarshalHelper helper = new DeviceAssignmentMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(includeAsset);
	    helper.setIncludeDevice(includeDevice);
	    helper.setIncludeSite(includeSite);
	    return helper.convert(assignment, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest)));
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * List device assignment history for a given device hardware id.
     * 
     * @param hardwareId
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{hardwareId}/assignments", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List assignment history for device")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Devices.ListDeviceAssignmentHistoryResponse.class, description = "listDeviceAssignmentHistoryResponse.md") })
    public ISearchResults<IDeviceAssignment> listDeviceAssignmentHistory(
	    @ApiParam(value = "Hardware id", required = true) @PathVariable String hardwareId,
	    @ApiParam(value = "Include detailed asset information", required = false) @RequestParam(defaultValue = "false") boolean includeAsset,
	    @ApiParam(value = "Include detailed device information", required = false) @RequestParam(defaultValue = "false") boolean includeDevice,
	    @ApiParam(value = "Include detailed site information", required = false) @RequestParam(defaultValue = "false") boolean includeSite,
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listDeviceAssignmentHistory", LOGGER);
	try {
	    SearchCriteria criteria = new SearchCriteria(page, pageSize);
	    ISearchResults<IDeviceAssignment> history = SiteWhere.getServer()
		    .getDeviceManagement(getTenant(servletRequest)).getDeviceAssignmentHistory(hardwareId, criteria);
	    DeviceAssignmentMarshalHelper helper = new DeviceAssignmentMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(includeAsset);
	    helper.setIncludeDevice(includeDevice);
	    helper.setIncludeSite(includeSite);
	    List<IDeviceAssignment> converted = new ArrayList<IDeviceAssignment>();
	    for (IDeviceAssignment assignment : history.getResults()) {
		converted.add(helper.convert(assignment,
			SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest))));
	    }
	    return new SearchResults<IDeviceAssignment>(converted, history.getNumResults());
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Create a new device element mapping.
     * 
     * @param request
     * @return
     */
    @RequestMapping(value = "/{hardwareId}/mappings", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Create new device element mapping")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Devices.AddMappingRequest.class, description = "addDeviceElementMappingRequest.md"),
	    @Example(stage = Stage.Response, json = Devices.AddMappingResponse.class, description = "addDeviceElementMappingResponse.md") })
    public IDevice addDeviceElementMapping(
	    @ApiParam(value = "Hardware id", required = true) @PathVariable String hardwareId,
	    @RequestBody DeviceElementMapping request, HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "addDeviceElementMapping", LOGGER);
	try {
	    IDevice updated = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		    .createDeviceElementMapping(hardwareId, request);
	    DeviceMarshalHelper helper = new DeviceMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(false);
	    helper.setIncludeAssignment(false);
	    return helper.convert(updated, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest)));
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    @RequestMapping(value = "/{hardwareId}/mappings", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "Delete existing device element mapping")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Devices.DeleteMappingResponse.class, description = "deleteDeviceElementMappingResponse.md") })
    public IDevice deleteDeviceElementMapping(
	    @ApiParam(value = "Hardware id", required = true) @PathVariable String hardwareId,
	    @ApiParam(value = "Device element path", required = true) @RequestParam(required = true) String path,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "deleteDeviceElementMapping", LOGGER);
	try {
	    IDevice updated = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		    .deleteDeviceElementMapping(hardwareId, path);
	    DeviceMarshalHelper helper = new DeviceMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(false);
	    helper.setIncludeAssignment(false);
	    return helper.convert(updated, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest)));
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Get default symbol for device.
     * 
     * @param hardwareId
     * @param servletRequest
     * @param response
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{hardwareId}/symbol", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "Get default symbol for device")
    public ResponseEntity<byte[]> getDeviceDefaultSymbol(
	    @ApiParam(value = "Hardware id", required = true) @PathVariable String hardwareId,
	    HttpServletRequest servletRequest, HttpServletResponse response) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "getDeviceDefaultSymbol", LOGGER);
	try {
	    IDevice device = assertDeviceWithoutUserValidation(hardwareId, servletRequest);
	    IEntityUriProvider provider = DefaultEntityUriProvider.getInstance();
	    ISymbolGeneratorManager symbols = SiteWhere.getServer()
		    .getDeviceCommunication(getTenant(servletRequest, false)).getSymbolGeneratorManager();
	    ISymbolGenerator generator = symbols.getDefaultSymbolGenerator();
	    if (generator != null) {
		byte[] image = generator.getDeviceSymbol(device, provider);

		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.IMAGE_PNG);
		return new ResponseEntity<byte[]>(image, headers, HttpStatus.CREATED);
	    } else {
		return null;
	    }
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * List devices that match given criteria.
     * 
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List devices that match criteria")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Devices.ListDevicesForCriteriaResponse.class, description = "listDevicesResponse.md") })
    public ISearchResults<IDevice> listDevices(
	    @ApiParam(value = "Specification filter", required = false) @RequestParam(required = false) String specification,
	    @ApiParam(value = "Site filter", required = false) @RequestParam(required = false) String site,
	    @ApiParam(value = "Include deleted devices", required = false) @RequestParam(required = false, defaultValue = "false") boolean includeDeleted,
	    @ApiParam(value = "Exclude assigned devices", required = false) @RequestParam(required = false, defaultValue = "false") boolean excludeAssigned,
	    @ApiParam(value = "Include specification information", required = false) @RequestParam(required = false, defaultValue = "false") boolean includeSpecification,
	    @ApiParam(value = "Include assignment information if associated", required = false) @RequestParam(required = false, defaultValue = "false") boolean includeAssignment,
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    @ApiParam(value = "Start date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
	    @ApiParam(value = "End date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listDevices", LOGGER);
	try {
	    IDeviceSearchCriteria criteria = new DeviceSearchCriteria(specification, site, excludeAssigned, page,
		    pageSize, startDate, endDate);
	    ISearchResults<IDevice> results = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		    .listDevices(includeDeleted, criteria);
	    DeviceMarshalHelper helper = new DeviceMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(true);
	    helper.setIncludeSpecification(includeSpecification);
	    helper.setIncludeAssignment(includeAssignment);
	    List<IDevice> devicesConv = new ArrayList<IDevice>();
	    for (IDevice device : results.getResults()) {
		devicesConv.add(
			helper.convert(device, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest))));
	    }
	    return new SearchResults<IDevice>(devicesConv, results.getNumResults());
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    @Deprecated
    @RequestMapping(value = "/specification/{token}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List devices using a given specification")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Devices.ListDevicesForSpecificationResponse.class, description = "listDevicesForSpecificationResponse.md") })
    public ISearchResults<IDevice> listDevicesForSpecification(
	    @ApiParam(value = "Specification token", required = true) @PathVariable String token,
	    @ApiParam(value = "Site filter", required = false) @RequestParam(required = false) String site,
	    @ApiParam(value = "Include deleted devices", required = false) @RequestParam(required = false, defaultValue = "false") boolean includeDeleted,
	    @ApiParam(value = "Exclude assigned devices", required = false) @RequestParam(required = false, defaultValue = "false") boolean excludeAssigned,
	    @ApiParam(value = "Include specification information", required = false) @RequestParam(required = false, defaultValue = "false") boolean includeSpecification,
	    @ApiParam(value = "Include assignment information if associated", required = false) @RequestParam(required = false, defaultValue = "false") boolean includeAssignment,
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    @ApiParam(value = "Start date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
	    @ApiParam(value = "End date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listDevices", LOGGER);
	try {
	    IDeviceSearchCriteria criteria = new DeviceSearchCriteria(token, site, excludeAssigned, page, pageSize,
		    startDate, endDate);
	    ISearchResults<IDevice> results = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		    .listDevices(includeDeleted, criteria);
	    DeviceMarshalHelper helper = new DeviceMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(true);
	    helper.setIncludeSpecification(includeSpecification);
	    helper.setIncludeAssignment(includeAssignment);
	    List<IDevice> devicesConv = new ArrayList<IDevice>();
	    for (IDevice device : results.getResults()) {
		devicesConv.add(
			helper.convert(device, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest))));
	    }
	    return new SearchResults<IDevice>(devicesConv, results.getNumResults());
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    @RequestMapping(value = "/group/{groupToken}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List devices in device group")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Devices.ListDevicesForSpecificationResponse.class, description = "listDevicesForGroupResponse.md") })
    public ISearchResults<IDevice> listDevicesForGroup(
	    @ApiParam(value = "Group token", required = true) @PathVariable String groupToken,
	    @ApiParam(value = "Specification filter", required = false) @RequestParam(required = false) String specification,
	    @ApiParam(value = "Site filter", required = false) @RequestParam(required = false) String site,
	    @ApiParam(value = "Include deleted devices", required = false) @RequestParam(required = false, defaultValue = "false") boolean includeDeleted,
	    @ApiParam(value = "Exclude assigned devices", required = false) @RequestParam(required = false, defaultValue = "false") boolean excludeAssigned,
	    @ApiParam(value = "Include specification information", required = false) @RequestParam(required = false, defaultValue = "false") boolean includeSpecification,
	    @ApiParam(value = "Include assignment information if associated", required = false) @RequestParam(required = false, defaultValue = "false") boolean includeAssignment,
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    @ApiParam(value = "Start date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
	    @ApiParam(value = "End date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listDevicesForGroup", LOGGER);
	try {
	    IDeviceSearchCriteria criteria = new DeviceSearchCriteria(specification, site, excludeAssigned, page,
		    pageSize, startDate, endDate);
	    List<IDevice> matches = DeviceGroupUtils.getDevicesInGroup(groupToken, criteria, getTenant(servletRequest));
	    DeviceMarshalHelper helper = new DeviceMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(true);
	    helper.setIncludeSpecification(includeSpecification);
	    helper.setIncludeAssignment(includeAssignment);
	    List<IDevice> devicesConv = new ArrayList<IDevice>();
	    for (IDevice device : matches) {
		devicesConv.add(
			helper.convert(device, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest))));
	    }
	    return new SearchResults<IDevice>(devicesConv, matches.size());
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    @RequestMapping(value = "/grouprole/{role}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List devices in device groups with role")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Devices.ListDevicesForSpecificationResponse.class, description = "listDevicesForGroupsWithRoleResponse.md") })
    public ISearchResults<IDevice> listDevicesForGroupsWithRole(
	    @ApiParam(value = "Group role", required = true) @PathVariable String role,
	    @ApiParam(value = "Specification filter", required = false) @RequestParam(required = false) String specification,
	    @ApiParam(value = "Site filter", required = false) @RequestParam(required = false) String site,
	    @ApiParam(value = "Include deleted devices", required = false) @RequestParam(required = false, defaultValue = "false") boolean includeDeleted,
	    @ApiParam(value = "Exclude assigned devices", required = false) @RequestParam(required = false, defaultValue = "false") boolean excludeAssigned,
	    @ApiParam(value = "Include specification information", required = false) @RequestParam(required = false, defaultValue = "false") boolean includeSpecification,
	    @ApiParam(value = "Include assignment information if associated", required = false) @RequestParam(required = false, defaultValue = "false") boolean includeAssignment,
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    @ApiParam(value = "Start date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
	    @ApiParam(value = "End date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listDevicesForGroupsWithRole", LOGGER);
	try {
	    IDeviceSearchCriteria criteria = new DeviceSearchCriteria(specification, site, excludeAssigned, page,
		    pageSize, startDate, endDate);
	    Collection<IDevice> matches = DeviceGroupUtils.getDevicesInGroupsWithRole(role, criteria,
		    getTenant(servletRequest));
	    DeviceMarshalHelper helper = new DeviceMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(true);
	    helper.setIncludeSpecification(includeSpecification);
	    helper.setIncludeAssignment(includeAssignment);
	    List<IDevice> devicesConv = new ArrayList<IDevice>();
	    for (IDevice device : matches) {
		devicesConv.add(
			helper.convert(device, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest))));
	    }
	    return new SearchResults<IDevice>(devicesConv, matches.size());
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Add a batch of events for the current assignment of the given device.
     * Note that the hardware id in the URL overrides the one specified in the
     * {@link DeviceEventBatch} object.
     * 
     * @param request
     * @return
     */
    @RequestMapping(value = "/{hardwareId}/batch", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Add multiple events for device")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Devices.AddDeviceEventBatchRequest.class, description = "addDeviceEventBatchRequest.md"),
	    @Example(stage = Stage.Response, json = Devices.AddDeviceEventBatchResponse.class, description = "addDeviceEventBatchResponse.md") })
    public IDeviceEventBatchResponse addDeviceEventBatch(
	    @ApiParam(value = "Hardware id", required = true) @PathVariable String hardwareId,
	    @RequestBody DeviceEventBatch batch, HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "addDeviceEventBatch", LOGGER);
	try {
	    IDevice device = assertDeviceByHardwareId(hardwareId, servletRequest);
	    if (device.getAssignmentToken() == null) {
		throw new SiteWhereSystemException(ErrorCode.DeviceNotAssigned, ErrorLevel.ERROR);
	    }

	    // Set event dates if not set by client.
	    for (IDeviceLocationCreateRequest locReq : batch.getLocations()) {
		if (locReq.getEventDate() == null) {
		    ((DeviceLocationCreateRequest) locReq).setEventDate(new Date());
		}
	    }
	    for (IDeviceMeasurementsCreateRequest measReq : batch.getMeasurements()) {
		if (measReq.getEventDate() == null) {
		    ((DeviceMeasurementsCreateRequest) measReq).setEventDate(new Date());
		}
	    }
	    for (IDeviceAlertCreateRequest alertReq : batch.getAlerts()) {
		if (alertReq.getEventDate() == null) {
		    ((DeviceAlertCreateRequest) alertReq).setEventDate(new Date());
		}
	    }

	    return SiteWhere.getServer().getDeviceEventManagement(getTenant(servletRequest))
		    .addDeviceEventBatch(device.getAssignmentToken(), batch);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Gets a device by unique hardware id and throws an exception if not found.
     * 
     * @param hardwareId
     * @param servletRequest
     * @return
     * @throws SiteWhereException
     */
    protected IDevice assertDeviceByHardwareId(String hardwareId, HttpServletRequest servletRequest)
	    throws SiteWhereException {
	IDevice result = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		.getDeviceByHardwareId(hardwareId);
	if (result == null) {
	    throw new SiteWhereSystemException(ErrorCode.InvalidHardwareId, ErrorLevel.ERROR,
		    HttpServletResponse.SC_NOT_FOUND);
	}
	return result;
    }

    /**
     * Gets a device by unique hardware id. Does not validate that the current
     * user has access to the tenant. This should *only* be used by non-secure
     * REST calls as it can be a security risk.
     * 
     * @param hardwareId
     * @param servletRequest
     * @return
     * @throws SiteWhereException
     */
    protected IDevice assertDeviceWithoutUserValidation(String hardwareId, HttpServletRequest servletRequest)
	    throws SiteWhereException {
	IDevice result = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest, false))
		.getDeviceByHardwareId(hardwareId);
	if (result == null) {
	    throw new SiteWhereSystemException(ErrorCode.InvalidHardwareId, ErrorLevel.ERROR,
		    HttpServletResponse.SC_NOT_FOUND);
	}
	return result;
    }
}