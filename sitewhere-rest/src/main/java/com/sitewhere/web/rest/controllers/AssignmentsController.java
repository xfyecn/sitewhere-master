/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.web.rest.controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
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
import com.sitewhere.device.charting.ChartBuilder;
import com.sitewhere.device.communication.symbology.DefaultEntityUriProvider;
import com.sitewhere.device.marshaling.DeviceAssignmentMarshalHelper;
import com.sitewhere.device.marshaling.DeviceCommandInvocationMarshalHelper;
import com.sitewhere.rest.model.common.MetadataProvider;
import com.sitewhere.rest.model.device.DeviceAssignment;
import com.sitewhere.rest.model.device.event.DeviceAlert;
import com.sitewhere.rest.model.device.event.DeviceCommandInvocation;
import com.sitewhere.rest.model.device.event.DeviceCommandResponse;
import com.sitewhere.rest.model.device.event.DeviceLocation;
import com.sitewhere.rest.model.device.event.DeviceMeasurements;
import com.sitewhere.rest.model.device.event.DeviceStateChange;
import com.sitewhere.rest.model.device.event.request.DeviceAlertCreateRequest;
import com.sitewhere.rest.model.device.event.request.DeviceCommandInvocationCreateRequest;
import com.sitewhere.rest.model.device.event.request.DeviceCommandResponseCreateRequest;
import com.sitewhere.rest.model.device.event.request.DeviceLocationCreateRequest;
import com.sitewhere.rest.model.device.event.request.DeviceMeasurementsCreateRequest;
import com.sitewhere.rest.model.device.event.request.DeviceStateChangeCreateRequest;
import com.sitewhere.rest.model.device.event.request.DeviceStreamDataCreateRequest;
import com.sitewhere.rest.model.device.request.DeviceAssignmentCreateRequest;
import com.sitewhere.rest.model.device.request.DeviceStreamCreateRequest;
import com.sitewhere.rest.model.device.streaming.DeviceStream;
import com.sitewhere.rest.model.search.DateRangeSearchCriteria;
import com.sitewhere.rest.model.search.SearchResults;
import com.sitewhere.server.scheduling.ScheduledJobHelper;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.SiteWhereSystemException;
import com.sitewhere.spi.device.DeviceAssignmentStatus;
import com.sitewhere.spi.device.DeviceAssignmentType;
import com.sitewhere.spi.device.IDeviceAssignment;
import com.sitewhere.spi.device.IDeviceManagement;
import com.sitewhere.spi.device.charting.IChartSeries;
import com.sitewhere.spi.device.command.IDeviceCommand;
import com.sitewhere.spi.device.event.IDeviceAlert;
import com.sitewhere.spi.device.event.IDeviceCommandInvocation;
import com.sitewhere.spi.device.event.IDeviceCommandResponse;
import com.sitewhere.spi.device.event.IDeviceEvent;
import com.sitewhere.spi.device.event.IDeviceLocation;
import com.sitewhere.spi.device.event.IDeviceMeasurements;
import com.sitewhere.spi.device.event.IDeviceStateChange;
import com.sitewhere.spi.device.event.IDeviceStreamData;
import com.sitewhere.spi.device.streaming.IDeviceStream;
import com.sitewhere.spi.device.symbology.IEntityUriProvider;
import com.sitewhere.spi.device.symbology.ISymbolGenerator;
import com.sitewhere.spi.device.symbology.ISymbolGeneratorManager;
import com.sitewhere.spi.error.ErrorCode;
import com.sitewhere.spi.error.ErrorLevel;
import com.sitewhere.spi.scheduling.IScheduledJob;
import com.sitewhere.spi.scheduling.request.IScheduledJobCreateRequest;
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
import com.sitewhere.web.rest.documentation.Assignments;
import com.sitewhere.web.rest.documentation.Schedules;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/*
 * Controller for assignment operations.
 * 
 * @author Derek Adams
 */
@Controller
@CrossOrigin
@RequestMapping(value = "/assignments")
@Api(value = "assignments", description = "Operations related to SiteWhere device assignments.")
@DocumentedController(name = "Device Assignments")
public class AssignmentsController extends RestController {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /**
     * Used by AJAX calls to create a device assignment.
     * 
     * @param request
     * @return
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Create a new device assignment")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assignments.CreateUnassociatedRequest.class, description = "createUnassociatedRequest.md"),
	    @Example(stage = Stage.Request, json = Assignments.CreateAssociatedRequest.class, description = "createAssociatedRequest.md"),
	    @Example(stage = Stage.Response, json = Assignments.CreateAssociatedResponse.class, description = "createAssociatedResponse.md") })
    public DeviceAssignment createDeviceAssignment(@RequestBody DeviceAssignmentCreateRequest request,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "createDeviceAssignment", LOGGER);
	try {
	    if (StringUtils.isEmpty(request.getDeviceHardwareId())) {
		throw new SiteWhereException("Hardware id required.");
	    }
	    if (request.getAssignmentType() == null) {
		throw new SiteWhereException("Assignment type required.");
	    }
	    if (request.getAssignmentType() != DeviceAssignmentType.Unassociated) {
		if (request.getAssetModuleId() == null) {
		    throw new SiteWhereException("Asset module id required.");
		}
		if (request.getAssetId() == null) {
		    throw new SiteWhereException("Asset id required.");
		}
	    }
	    IDeviceManagement management = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest));
	    IDeviceAssignment created = management.createDeviceAssignment(request);
	    DeviceAssignmentMarshalHelper helper = new DeviceAssignmentMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(true);
	    helper.setIncludeDevice(true);
	    helper.setIncludeSite(true);
	    return helper.convert(created, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest)));
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Get an assignment by its unique token.
     * 
     * @param token
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "Get device assignment by token")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assignments.CreateAssociatedResponse.class, description = "getDeviceAssignmentResponse.md") })
    public DeviceAssignment getDeviceAssignment(
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "getDeviceAssignment", LOGGER);
	try {
	    IDeviceAssignment assignment = assureAssignment(token, servletRequest);
	    DeviceAssignmentMarshalHelper helper = new DeviceAssignmentMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(true);
	    helper.setIncludeDevice(true);
	    helper.setIncludeSite(true);
	    return helper.convert(assignment, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest)));
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Delete an existing device assignment.
     * 
     * @param token
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "Delete an existing device assignment")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assignments.CreateAssociatedResponse.class, description = "deleteDeviceAssignmentResponse.md") })
    public DeviceAssignment deleteDeviceAssignment(
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    @ApiParam(value = "Delete permanently", required = false) @RequestParam(defaultValue = "false") @Concerns(values = {
		    ConcernType.ForceDelete }) boolean force,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "deleteDeviceAssignment", LOGGER);
	try {
	    IDeviceAssignment assignment = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		    .deleteDeviceAssignment(token, force);
	    DeviceAssignmentMarshalHelper helper = new DeviceAssignmentMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(true);
	    helper.setIncludeDevice(true);
	    helper.setIncludeSite(true);
	    return helper.convert(assignment, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest)));
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Update metadata associated with an assignment.
     * 
     * @param request
     * @return
     */
    @RequestMapping(value = "/{token}/metadata", method = RequestMethod.PUT)
    @ResponseBody
    @ApiOperation(value = "Update device assignment metadata")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assignments.UpdateAssignmentMetadataRequest.class, description = "updateAssignmentMetadataRequest.md") })
    public DeviceAssignment updateDeviceAssignmentMetadata(
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    @RequestBody MetadataProvider metadata, HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "updateDeviceAssignmentMetadata", LOGGER);
	try {
	    IDeviceAssignment result = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		    .updateDeviceAssignmentMetadata(token, metadata);
	    DeviceAssignmentMarshalHelper helper = new DeviceAssignmentMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(true);
	    helper.setIncludeDevice(true);
	    helper.setIncludeSite(true);
	    return helper.convert(result, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest)));
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * List all device events for an assignment that match the given criteria.
     * 
     * @param token
     * @param page
     * @param pageSize
     * @param startDate
     * @param endDate
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/events", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List events for device assignment")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assignments.ListAssignmentEventsResponse.class, description = "listEventsResponse.md") })
    public ISearchResults<IDeviceEvent> listEvents(
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    @ApiParam(value = "Start date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
	    @ApiParam(value = "End date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listEvents", LOGGER);
	try {
	    DateRangeSearchCriteria criteria = new DateRangeSearchCriteria(page, pageSize, startDate, endDate);
	    return SiteWhere.getServer().getDeviceEventManagement(getTenant(servletRequest)).listDeviceEvents(token,
		    criteria);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * List all device measurements for a given assignment.
     * 
     * @param assignmentToken
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/measurements", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List measurement events for device assignment")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assignments.ListAssignmentMeasurementsResponse.class, description = "listMeasurementsResponse.md") })
    public ISearchResults<IDeviceMeasurements> listMeasurements(
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    @ApiParam(value = "Start date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
	    @ApiParam(value = "End date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listMeasurements", LOGGER);
	try {
	    DateRangeSearchCriteria criteria = new DateRangeSearchCriteria(page, pageSize, startDate, endDate);
	    return SiteWhere.getServer().getDeviceEventManagement(getTenant(servletRequest))
		    .listDeviceMeasurements(token, criteria);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * List device measurements for a given assignment.
     * 
     * @param assignmentToken
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/measurements/series", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List assignment measurements as chart series")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assignments.ListAssignmentMeasurementsChartSeriesResponse.class, description = "listMeasurementsAsChartSeriesResponse.md") })
    public List<IChartSeries<Double>> listMeasurementsAsChartSeries(
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    @ApiParam(value = "Start date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
	    @ApiParam(value = "End date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
	    @ApiParam(value = "Measurement Ids", required = false) @RequestParam(required = false) String[] measurementIds,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listMeasurementsAsChartSeries", LOGGER);
	try {
	    DateRangeSearchCriteria criteria = new DateRangeSearchCriteria(page, pageSize, startDate, endDate);
	    ISearchResults<IDeviceMeasurements> measurements = SiteWhere.getServer()
		    .getDeviceEventManagement(getTenant(servletRequest)).listDeviceMeasurements(token, criteria);
	    ChartBuilder builder = new ChartBuilder();
	    return builder.process(measurements.getResults(), measurementIds);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Create measurements to be associated with a device assignment.
     * 
     * @param input
     * @param token
     * @param updateState
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/measurements", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Create measurements event for device assignment")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assignments.CreateAssignmentMeasurementsRequest.class, description = "createMeasurementsRequest.md"),
	    @Example(stage = Stage.Response, json = Assignments.CreateAssignmentMeasurementsResponse.class, description = "createMeasurementsResponse.md") })
    public DeviceMeasurements createMeasurements(@RequestBody DeviceMeasurementsCreateRequest input,
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "createMeasurements", LOGGER);
	try {
	    IDeviceMeasurements result = SiteWhere.getServer().getDeviceEventManagement(getTenant(servletRequest))
		    .addDeviceMeasurements(token, input);
	    return DeviceMeasurements.copy(result);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * List device locations for a given assignment.
     * 
     * @param assignmentToken
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/locations", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List location events for device assignment")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assignments.ListAssignmentLocationsResponse.class, description = "listLocationsResponse.md") })
    public ISearchResults<IDeviceLocation> listLocations(
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    @ApiParam(value = "Start date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
	    @ApiParam(value = "End date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listLocations", LOGGER);
	try {
	    DateRangeSearchCriteria criteria = new DateRangeSearchCriteria(page, pageSize, startDate, endDate);
	    return SiteWhere.getServer().getDeviceEventManagement(getTenant(servletRequest)).listDeviceLocations(token,
		    criteria);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Create location to be associated with a device assignment.
     * 
     * @param input
     * @param token
     * @param updateState
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/locations", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Create location event for device assignment")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assignments.CreateAssignmentLocationRequest.class, description = "createLocationRequest.md"),
	    @Example(stage = Stage.Response, json = Assignments.CreateAssignmentLocationResponse.class, description = "createLocationResponse.md") })
    public DeviceLocation createLocation(@RequestBody DeviceLocationCreateRequest input,
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "createLocation", LOGGER);
	try {
	    IDeviceLocation result = SiteWhere.getServer().getDeviceEventManagement(getTenant(servletRequest))
		    .addDeviceLocation(token, input);
	    return DeviceLocation.copy(result);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * List device alerts for a given assignment.
     * 
     * @param assignmentToken
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/alerts", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List alert events for device assignment")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assignments.ListAssignmenAlertsResponse.class, description = "listAlertsResponse.md") })
    public ISearchResults<IDeviceAlert> listAlerts(
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    @ApiParam(value = "Start date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
	    @ApiParam(value = "End date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listAlerts", LOGGER);
	try {
	    DateRangeSearchCriteria criteria = new DateRangeSearchCriteria(page, pageSize, startDate, endDate);
	    return SiteWhere.getServer().getDeviceEventManagement(getTenant(servletRequest)).listDeviceAlerts(token,
		    criteria);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Create alert to be associated with a device assignment.
     * 
     * @param input
     * @param token
     * @param updateState
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/alerts", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Create alert event for device assignment")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assignments.CreateAssignmentAlertRequest.class, description = "createAlertRequest.md"),
	    @Example(stage = Stage.Response, json = Assignments.CreateAssignmentAlertResponse.class, description = "createAlertResponse.md") })
    public DeviceAlert createAlert(@RequestBody DeviceAlertCreateRequest input,
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "createAlert", LOGGER);
	try {
	    IDeviceAlert result = SiteWhere.getServer().getDeviceEventManagement(getTenant(servletRequest))
		    .addDeviceAlert(token, input);
	    return DeviceAlert.copy(result);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Create a stream to be associated with a device assignment.
     * 
     * @param input
     * @param token
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/streams", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Create data stream for a device assignment")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assignments.CreateDeviceStreamRequest.class, description = "createDeviceStreamRequest.md"),
	    @Example(stage = Stage.Response, json = Assignments.CreateDeviceStreamResponse.class, description = "createDeviceStreamResponse.md") })
    public DeviceStream createDeviceStream(@RequestBody DeviceStreamCreateRequest request,
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "createDeviceStream", LOGGER);
	try {
	    IDeviceStream result = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		    .createDeviceStream(token, request);
	    return DeviceStream.copy(result);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    @RequestMapping(value = "/{token}/streams", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List data streams for device assignment")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assignments.ListDeviceStreamsResponse.class, description = "listDeviceStreamsResponse.md") })
    public ISearchResults<IDeviceStream> listDeviceStreams(
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    @ApiParam(value = "Start date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
	    @ApiParam(value = "End date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listDeviceStreams", LOGGER);
	try {
	    DateRangeSearchCriteria criteria = new DateRangeSearchCriteria(page, pageSize, startDate, endDate);
	    ISearchResults<IDeviceStream> matches = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		    .listDeviceStreams(token, criteria);
	    List<IDeviceStream> converted = new ArrayList<IDeviceStream>();
	    for (IDeviceStream stream : matches.getResults()) {
		converted.add(DeviceStream.copy(stream));
	    }
	    return new SearchResults<IDeviceStream>(converted);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Get an existing device stream associated with an assignment.
     * 
     * @param token
     * @param streamId
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/streams/{streamId:.+}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ApiOperation(value = "Get device assignment data stream by id")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assignments.GetDeviceStreamResponse.class, description = "getDeviceStreamResponse.md") })
    public DeviceStream getDeviceStream(
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    @ApiParam(value = "Stream Id", required = true) @PathVariable String streamId,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "getDeviceStream", LOGGER);
	try {
	    IDeviceStream result = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		    .getDeviceStream(token, streamId);
	    if (result == null) {
		throw new SiteWhereSystemException(ErrorCode.InvalidStreamId, ErrorLevel.ERROR,
			HttpServletResponse.SC_NOT_FOUND);
	    }
	    return DeviceStream.copy(result);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Adds data to an existing device stream.
     * 
     * @param token
     * @param streamId
     * @param sequenceNumber
     * @param svtRequest
     * @param svtResponse
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/streams/{streamId:.+}", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Add data to device assignment data stream")
    @Secured({ SiteWhereRoles.REST })
    @Documented
    public void addDeviceStreamData(@ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    @ApiParam(value = "Stream Id", required = true) @PathVariable String streamId,
	    @ApiParam(value = "Sequence Number", required = false) @RequestParam(required = false) Long sequenceNumber,
	    HttpServletRequest servletRequest, HttpServletResponse svtResponse) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "addDeviceStreamData", LOGGER);
	try {
	    ServletInputStream inData = servletRequest.getInputStream();
	    ByteArrayOutputStream byteData = new ByteArrayOutputStream();
	    int data;
	    while ((data = inData.read()) != -1) {
		byteData.write(data);
	    }
	    byte[] payload = byteData.toByteArray();
	    DeviceStreamDataCreateRequest request = new DeviceStreamDataCreateRequest();
	    request.setStreamId(streamId);
	    request.setSequenceNumber(sequenceNumber);
	    request.setEventDate(new Date());
	    request.setUpdateState(false);
	    request.setData(payload);
	    SiteWhere.getServer().getDeviceEventManagement(getTenant(servletRequest)).addDeviceStreamData(token,
		    request);
	    svtResponse.setStatus(HttpServletResponse.SC_CREATED);
	} catch (SiteWhereSystemException e) {
	    if (e.getCode() == ErrorCode.InvalidStreamId) {
		svtResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
	    } else {
		LOGGER.error("Unhandled SiteWhere exception.", e);
		svtResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	    }
	} catch (IOException e) {
	    LOGGER.error(e);
	    svtResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Get a single chunk of data from a device stream.
     * 
     * @param token
     * @param streamId
     * @param sequenceNumber
     * @param svtResponse
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/streams/{streamId:.+}/data/{sequenceNumber}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "Get data from device assignment data stream")
    @Secured({ SiteWhereRoles.REST })
    @Documented
    public void getDeviceStreamData(@ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    @ApiParam(value = "Stream Id", required = true) @PathVariable String streamId,
	    @ApiParam(value = "Sequence Number", required = true) @PathVariable long sequenceNumber,
	    HttpServletRequest servletRequest, HttpServletResponse svtResponse) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listDeviceStreamData", LOGGER);
	IDeviceStreamData chunk = SiteWhere.getServer().getDeviceEventManagement(getTenant(servletRequest))
		.getDeviceStreamData(token, streamId, sequenceNumber);
	if (chunk == null) {
	    svtResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
	    return;
	}
	try {
	    svtResponse.getOutputStream().write(chunk.getData());
	} catch (IOException e) {
	    throw new SiteWhereException("Unable to write device stream data chunk.", e);
	}
    }

    @RequestMapping(value = "/{token}/streams/{streamId:.+}/data", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "Get all data from device assignment data stream")
    @Secured({ SiteWhereRoles.REST })
    @Documented
    public void listDeviceStreamData(@ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    @ApiParam(value = "Stream Id", required = true) @PathVariable String streamId,
	    HttpServletRequest servletRequest, HttpServletResponse svtResponse) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listDeviceStreamData", LOGGER);
	IDeviceStream stream = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		.getDeviceStream(token, streamId);
	if (stream == null) {
	    throw new SiteWhereSystemException(ErrorCode.InvalidStreamId, ErrorLevel.ERROR,
		    HttpServletResponse.SC_NOT_FOUND);
	}
	svtResponse.setContentType(stream.getContentType());

	DateRangeSearchCriteria criteria = new DateRangeSearchCriteria(1, 0, null, null);
	ISearchResults<IDeviceStreamData> data = SiteWhere.getServer()
		.getDeviceEventManagement(getTenant(servletRequest)).listDeviceStreamData(token, streamId, criteria);

	// Sort results by sequence number.
	Collections.sort(data.getResults(), new Comparator<IDeviceStreamData>() {

	    @Override
	    public int compare(IDeviceStreamData o1, IDeviceStreamData o2) {
		return o1.getSequenceNumber().compareTo(o2.getSequenceNumber());
	    }
	});
	for (IDeviceStreamData chunk : data.getResults()) {
	    try {
		svtResponse.getOutputStream().write(chunk.getData());
	    } catch (IOException e) {
		LOGGER.error("Error writing chunk to servlet output stream.", e);
	    }
	}
    }

    /**
     * Create command invocation to be associated with a device assignment.
     * 
     * @param input
     * @param token
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/invocations", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Create command invocation event for assignment")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assignments.CreateCommandInvocationRequest.class, description = "createCommandInvocationRequest.md"),
	    @Example(stage = Stage.Response, json = Assignments.CreateCommandInvocationResponse.class, description = "createCommandInvocationResponse.md") })
    public DeviceCommandInvocation createCommandInvocation(@RequestBody DeviceCommandInvocationCreateRequest request,
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "createCommandInvocation", LOGGER);
	try {
	    IDeviceCommand command = assureDeviceCommand(request.getCommandToken(), servletRequest);
	    IDeviceCommandInvocation result = SiteWhere.getServer().getDeviceEventManagement(getTenant(servletRequest))
		    .addDeviceCommandInvocation(token, command, request);
	    DeviceCommandInvocationMarshalHelper helper = new DeviceCommandInvocationMarshalHelper(
		    getTenant(servletRequest));
	    return helper.convert(result);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    @RequestMapping(value = "/{token}/invocations/schedules/{scheduleToken}", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Schedule command invocation")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assignments.CreateCommandInvocationRequest.class, description = "scheduleCommandInvocationRequest.md"),
	    @Example(stage = Stage.Response, json = Schedules.CreateScheduledJobResponse.class, description = "scheduleCommandInvocationResponse.md") })
    public IScheduledJob scheduleCommandInvocation(@RequestBody DeviceCommandInvocationCreateRequest request,
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    @ApiParam(value = "Schedule token", required = true) @PathVariable String scheduleToken,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "scheduleCommandInvocation", LOGGER);
	try {
	    assureDeviceCommand(request.getCommandToken(), servletRequest);
	    IScheduledJobCreateRequest job = ScheduledJobHelper.createCommandInvocationJob(UUID.randomUUID().toString(),
		    token, request.getCommandToken(), request.getParameterValues(), scheduleToken);
	    return SiteWhere.getServer().getScheduleManagement(getTenant(servletRequest)).createScheduledJob(job);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * List device command invocations for a given assignment.
     * 
     * @param assignmentToken
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/invocations", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List command invocation events for assignment")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assignments.ListCommandInvocationsResponse.class, description = "listCommandInvocationsResponse.md") })
    public ISearchResults<IDeviceCommandInvocation> listCommandInvocations(
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    @ApiParam(value = "Include command information", required = false) @RequestParam(defaultValue = "true") boolean includeCommand,
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    @ApiParam(value = "Start date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
	    @ApiParam(value = "End date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listCommandInvocations", LOGGER);
	try {
	    DateRangeSearchCriteria criteria = new DateRangeSearchCriteria(page, pageSize, startDate, endDate);
	    ISearchResults<IDeviceCommandInvocation> matches = SiteWhere.getServer()
		    .getDeviceEventManagement(getTenant(servletRequest)).listDeviceCommandInvocations(token, criteria);
	    DeviceCommandInvocationMarshalHelper helper = new DeviceCommandInvocationMarshalHelper(
		    getTenant(servletRequest));
	    helper.setIncludeCommand(includeCommand);
	    List<IDeviceCommandInvocation> converted = new ArrayList<IDeviceCommandInvocation>();
	    for (IDeviceCommandInvocation invocation : matches.getResults()) {
		converted.add(helper.convert(invocation));
	    }
	    return new SearchResults<IDeviceCommandInvocation>(converted);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Create state change to be associated with a device assignment.
     * 
     * @param input
     * @param token
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/statechanges", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Create an state change event for a device assignment")
    @Secured({ SiteWhereRoles.REST })
    public DeviceStateChange createStateChange(@RequestBody DeviceStateChangeCreateRequest input,
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "createStateChange", LOGGER);
	try {
	    IDeviceStateChange result = SiteWhere.getServer().getDeviceEventManagement(getTenant(servletRequest))
		    .addDeviceStateChange(token, input);
	    return DeviceStateChange.copy(result);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * List device state changes for a given assignment.
     * 
     * @param assignmentToken
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/statechanges", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List state change events for a device assignment")
    @Secured({ SiteWhereRoles.REST })
    public ISearchResults<IDeviceStateChange> listStateChanges(
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    @ApiParam(value = "Start date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
	    @ApiParam(value = "End date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listStateChanges", LOGGER);
	try {
	    DateRangeSearchCriteria criteria = new DateRangeSearchCriteria(page, pageSize, startDate, endDate);
	    return SiteWhere.getServer().getDeviceEventManagement(getTenant(servletRequest))
		    .listDeviceStateChanges(token, criteria);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Create command response to be associated with a device assignment.
     * 
     * @param input
     * @param token
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/responses", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Create command response event for assignment")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Request, json = Assignments.CreateCommandResponseEventRequest.class, description = "createCommandResponseEventRequest.md"),
	    @Example(stage = Stage.Request, json = Assignments.CreateCommandResponseSimpleRequest.class, description = "createCommandResponseSimpleRequest.md"),
	    @Example(stage = Stage.Response, json = Assignments.CreateCommandResponseResponse.class, description = "createCommandResponseResponse.md") })
    public DeviceCommandResponse createCommandResponse(@RequestBody DeviceCommandResponseCreateRequest input,
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "createCommandResponse", LOGGER);
	try {
	    IDeviceCommandResponse result = SiteWhere.getServer().getDeviceEventManagement(getTenant(servletRequest))
		    .addDeviceCommandResponse(token, input);
	    return DeviceCommandResponse.copy(result);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * List device command responses for a given assignment.
     * 
     * @param assignmentToken
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/responses", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "List command response events for assignment")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assignments.ListCommandResponsesResponse.class, description = "listCommandResponsesResponse.md") })
    public ISearchResults<IDeviceCommandResponse> listCommandResponses(
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    @ApiParam(value = "Page number", required = false) @RequestParam(required = false, defaultValue = "1") @Concerns(values = {
		    ConcernType.Paging }) int page,
	    @ApiParam(value = "Page size", required = false) @RequestParam(required = false, defaultValue = "100") @Concerns(values = {
		    ConcernType.Paging }) int pageSize,
	    @ApiParam(value = "Start date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
	    @ApiParam(value = "End date", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "listCommandResponses", LOGGER);
	try {
	    DateRangeSearchCriteria criteria = new DateRangeSearchCriteria(page, pageSize, startDate, endDate);
	    return SiteWhere.getServer().getDeviceEventManagement(getTenant(servletRequest))
		    .listDeviceCommandResponses(token, criteria);
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Get the default symbol for a device assignment.
     * 
     * @param token
     * @param servletRequest
     * @param response
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/symbol", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "Get default symbol for assignment")
    public ResponseEntity<byte[]> getDeviceAssignmentSymbol(
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    HttpServletRequest servletRequest, HttpServletResponse response) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "getDeviceAssignmentSymbol", LOGGER);
	try {
	    IDeviceAssignment assignment = assureAssignmentWithoutUserValidation(token, servletRequest);
	    IEntityUriProvider provider = DefaultEntityUriProvider.getInstance();
	    ISymbolGeneratorManager symbols = SiteWhere.getServer()
		    .getDeviceCommunication(getTenant(servletRequest, false)).getSymbolGeneratorManager();
	    ISymbolGenerator generator = symbols.getDefaultSymbolGenerator();
	    if (generator != null) {
		byte[] image = generator.getDeviceAssigmentSymbol(assignment, provider);

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
     * End an existing device assignment.
     * 
     * @param token
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/end", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Release an active device assignment")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assignments.EndDeviceAssignmentResponse.class, description = "endDeviceAssignmentResponse.md") })
    public DeviceAssignment endDeviceAssignment(
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "endDeviceAssignment", LOGGER);
	try {
	    IDeviceManagement management = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest));
	    IDeviceAssignment updated = management.endDeviceAssignment(token);
	    DeviceAssignmentMarshalHelper helper = new DeviceAssignmentMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(true);
	    helper.setIncludeDevice(true);
	    helper.setIncludeSite(true);
	    return helper.convert(updated, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest)));
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Mark a device assignment as missing.
     * 
     * @param token
     * @return
     * @throws SiteWhereException
     */
    @RequestMapping(value = "/{token}/missing", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "Mark device assignment as missing")
    @Secured({ SiteWhereRoles.REST })
    @Documented(examples = {
	    @Example(stage = Stage.Response, json = Assignments.MissingDeviceAssignmentResponse.class, description = "missingDeviceAssignmentResponse.md") })
    public DeviceAssignment missingDeviceAssignment(
	    @ApiParam(value = "Assignment token", required = true) @PathVariable String token,
	    HttpServletRequest servletRequest) throws SiteWhereException {
	Tracer.start(TracerCategory.RestApiCall, "missingDeviceAssignment", LOGGER);
	try {
	    IDeviceManagement management = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest));
	    IDeviceAssignment updated = management.updateDeviceAssignmentStatus(token, DeviceAssignmentStatus.Missing);
	    DeviceAssignmentMarshalHelper helper = new DeviceAssignmentMarshalHelper(getTenant(servletRequest));
	    helper.setIncludeAsset(true);
	    helper.setIncludeDevice(true);
	    helper.setIncludeSite(true);
	    return helper.convert(updated, SiteWhere.getServer().getAssetModuleManager(getTenant(servletRequest)));
	} finally {
	    Tracer.stop(LOGGER);
	}
    }

    /**
     * Get an assignment by unique token. Throw an exception if not found.
     * 
     * @param token
     * @param servletRequest
     * @return
     * @throws SiteWhereException
     */
    protected IDeviceAssignment assureAssignment(String token, HttpServletRequest servletRequest)
	    throws SiteWhereException {
	IDeviceAssignment assignment = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		.getDeviceAssignmentByToken(token);
	if (assignment == null) {
	    throw new SiteWhereSystemException(ErrorCode.InvalidDeviceAssignmentToken, ErrorLevel.ERROR);
	}
	return assignment;
    }

    /**
     * Gets an assignment given its unique token. This implementation skips the
     * validation which ensures the authenticated user has access to the tenant.
     * It should *only* be used to access resources that are not protected.
     * 
     * @param token
     * @param servletRequest
     * @return
     * @throws SiteWhereException
     */
    protected IDeviceAssignment assureAssignmentWithoutUserValidation(String token, HttpServletRequest servletRequest)
	    throws SiteWhereException {
	IDeviceAssignment assignment = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest, false))
		.getDeviceAssignmentByToken(token);
	if (assignment == null) {
	    throw new SiteWhereSystemException(ErrorCode.InvalidDeviceAssignmentToken, ErrorLevel.ERROR);
	}
	return assignment;
    }

    /**
     * Get a device command by unique token. Throw an exception if not found.
     * 
     * @param token
     * @param servletRequest
     * @return
     * @throws SiteWhereException
     */
    protected IDeviceCommand assureDeviceCommand(String token, HttpServletRequest servletRequest)
	    throws SiteWhereException {
	IDeviceCommand command = SiteWhere.getServer().getDeviceManagement(getTenant(servletRequest))
		.getDeviceCommandByToken(token);
	if (command == null) {
	    throw new SiteWhereSystemException(ErrorCode.InvalidDeviceCommandToken, ErrorLevel.ERROR);
	}
	return command;
    }
}