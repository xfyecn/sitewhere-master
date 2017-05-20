/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.authentication.encoding.MessageDigestPasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;

import com.sitewhere.SiteWhere;
import com.sitewhere.rest.model.asset.Asset;
import com.sitewhere.rest.model.asset.AssetCategory;
import com.sitewhere.rest.model.asset.HardwareAsset;
import com.sitewhere.rest.model.asset.LocationAsset;
import com.sitewhere.rest.model.asset.PersonAsset;
import com.sitewhere.rest.model.common.MetadataProvider;
import com.sitewhere.rest.model.common.MetadataProviderEntity;
import com.sitewhere.rest.model.device.Device;
import com.sitewhere.rest.model.device.DeviceAssignment;
import com.sitewhere.rest.model.device.DeviceElementMapping;
import com.sitewhere.rest.model.device.DeviceSpecification;
import com.sitewhere.rest.model.device.Site;
import com.sitewhere.rest.model.device.SiteMapData;
import com.sitewhere.rest.model.device.Zone;
import com.sitewhere.rest.model.device.batch.BatchElement;
import com.sitewhere.rest.model.device.batch.BatchOperation;
import com.sitewhere.rest.model.device.command.DeviceCommand;
import com.sitewhere.rest.model.device.element.DeviceElementSchema;
import com.sitewhere.rest.model.device.event.DeviceAlert;
import com.sitewhere.rest.model.device.event.DeviceCommandInvocation;
import com.sitewhere.rest.model.device.event.DeviceCommandResponse;
import com.sitewhere.rest.model.device.event.DeviceEvent;
import com.sitewhere.rest.model.device.event.DeviceEventBatchResponse;
import com.sitewhere.rest.model.device.event.DeviceLocation;
import com.sitewhere.rest.model.device.event.DeviceMeasurements;
import com.sitewhere.rest.model.device.event.DeviceStateChange;
import com.sitewhere.rest.model.device.event.DeviceStreamData;
import com.sitewhere.rest.model.device.group.DeviceGroup;
import com.sitewhere.rest.model.device.group.DeviceGroupElement;
import com.sitewhere.rest.model.device.request.BatchOperationCreateRequest;
import com.sitewhere.rest.model.device.request.DeviceCreateRequest;
import com.sitewhere.rest.model.device.streaming.DeviceStream;
import com.sitewhere.rest.model.scheduling.Schedule;
import com.sitewhere.rest.model.scheduling.ScheduledJob;
import com.sitewhere.rest.model.search.tenant.TenantSearchCriteria;
import com.sitewhere.rest.model.tenant.Tenant;
import com.sitewhere.rest.model.tenant.TenantGroup;
import com.sitewhere.rest.model.tenant.TenantGroupElement;
import com.sitewhere.rest.model.tenant.request.TenantCreateRequest;
import com.sitewhere.rest.model.user.GrantedAuthority;
import com.sitewhere.rest.model.user.User;
import com.sitewhere.security.LoginManager;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.SiteWhereSystemException;
import com.sitewhere.spi.asset.AssetType;
import com.sitewhere.spi.asset.IAssetCategory;
import com.sitewhere.spi.asset.request.IAssetCategoryCreateRequest;
import com.sitewhere.spi.asset.request.IAssetCreateRequest;
import com.sitewhere.spi.asset.request.IHardwareAssetCreateRequest;
import com.sitewhere.spi.asset.request.ILocationAssetCreateRequest;
import com.sitewhere.spi.asset.request.IPersonAssetCreateRequest;
import com.sitewhere.spi.common.ILocation;
import com.sitewhere.spi.device.DeviceAssignmentStatus;
import com.sitewhere.spi.device.DeviceAssignmentType;
import com.sitewhere.spi.device.DeviceContainerPolicy;
import com.sitewhere.spi.device.DeviceStatus;
import com.sitewhere.spi.device.IDevice;
import com.sitewhere.spi.device.IDeviceAssignment;
import com.sitewhere.spi.device.IDeviceElementMapping;
import com.sitewhere.spi.device.IDeviceManagement;
import com.sitewhere.spi.device.IDeviceSpecification;
import com.sitewhere.spi.device.batch.ElementProcessingStatus;
import com.sitewhere.spi.device.batch.OperationType;
import com.sitewhere.spi.device.command.ICommandParameter;
import com.sitewhere.spi.device.command.IDeviceCommand;
import com.sitewhere.spi.device.element.IDeviceElementSchema;
import com.sitewhere.spi.device.event.AlertLevel;
import com.sitewhere.spi.device.event.AlertSource;
import com.sitewhere.spi.device.event.CommandStatus;
import com.sitewhere.spi.device.event.IDeviceEventBatch;
import com.sitewhere.spi.device.event.IDeviceEventManagement;
import com.sitewhere.spi.device.event.request.IDeviceAlertCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceCommandInvocationCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceCommandResponseCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceEventCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceLocationCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceMeasurementsCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceStateChangeCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceStreamCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceStreamDataCreateRequest;
import com.sitewhere.spi.device.request.IBatchCommandInvocationRequest;
import com.sitewhere.spi.device.request.IBatchElementUpdateRequest;
import com.sitewhere.spi.device.request.IBatchOperationCreateRequest;
import com.sitewhere.spi.device.request.IBatchOperationUpdateRequest;
import com.sitewhere.spi.device.request.IDeviceAssignmentCreateRequest;
import com.sitewhere.spi.device.request.IDeviceCommandCreateRequest;
import com.sitewhere.spi.device.request.IDeviceCreateRequest;
import com.sitewhere.spi.device.request.IDeviceGroupCreateRequest;
import com.sitewhere.spi.device.request.IDeviceGroupElementCreateRequest;
import com.sitewhere.spi.device.request.IDeviceSpecificationCreateRequest;
import com.sitewhere.spi.device.request.ISiteCreateRequest;
import com.sitewhere.spi.device.request.IZoneCreateRequest;
import com.sitewhere.spi.device.util.DeviceSpecificationUtils;
import com.sitewhere.spi.error.ErrorCode;
import com.sitewhere.spi.error.ErrorLevel;
import com.sitewhere.spi.scheduling.ScheduledJobState;
import com.sitewhere.spi.scheduling.request.IScheduleCreateRequest;
import com.sitewhere.spi.scheduling.request.IScheduledJobCreateRequest;
import com.sitewhere.spi.search.ISearchResults;
import com.sitewhere.spi.search.user.ITenantSearchCriteria;
import com.sitewhere.spi.server.tenant.ISiteWhereTenantEngine;
import com.sitewhere.spi.tenant.ITenant;
import com.sitewhere.spi.tenant.ITenantManagement;
import com.sitewhere.spi.tenant.request.ITenantCreateRequest;
import com.sitewhere.spi.tenant.request.ITenantGroupCreateRequest;
import com.sitewhere.spi.tenant.request.ITenantGroupElementCreateRequest;
import com.sitewhere.spi.user.request.IGrantedAuthorityCreateRequest;
import com.sitewhere.spi.user.request.IUserCreateRequest;

/**
 * Common methods needed by device service provider implementations.
 * 
 * @author Derek
 */
public class SiteWherePersistence {

    /** Password encoder */
    private static MessageDigestPasswordEncoder passwordEncoder = new ShaPasswordEncoder();

    /**
     * Initialize entity fields.
     * 
     * @param entity
     * @throws SiteWhereException
     */
    public static void initializeEntityMetadata(MetadataProviderEntity entity) throws SiteWhereException {
	entity.setCreatedDate(new Date());
	entity.setCreatedBy(LoginManager.getCurrentlyLoggedInUser().getUsername());
	entity.setDeleted(false);
    }

    /**
     * Set updated fields.
     * 
     * @param entity
     * @throws SiteWhereException
     */
    public static void setUpdatedEntityMetadata(MetadataProviderEntity entity) throws SiteWhereException {
	entity.setUpdatedDate(new Date());
	entity.setUpdatedBy(LoginManager.getCurrentlyLoggedInUser().getUsername());
    }

    /**
     * Common logic for creating new device specification and populating it from
     * request.
     * 
     * @param request
     * @param token
     * @return
     * @throws SiteWhereException
     */
    public static DeviceSpecification deviceSpecificationCreateLogic(IDeviceSpecificationCreateRequest request,
	    String token) throws SiteWhereException {
	DeviceSpecification spec = new DeviceSpecification();

	// Unique token is required.
	if (token == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	spec.setToken(token);

	// Name is required.
	if (request.getName() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	spec.setName(request.getName());

	// Asset module id is required.
	if (request.getAssetModuleId() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	spec.setAssetModuleId(request.getAssetModuleId());

	// Asset id is required.
	if (request.getAssetId() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	spec.setAssetId(request.getAssetId());

	// Container policy is required.
	if (request.getContainerPolicy() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	spec.setContainerPolicy(request.getContainerPolicy());

	// If composite container policy and no device element schema, create an
	// empty
	// schema.
	if (request.getContainerPolicy() == DeviceContainerPolicy.Composite) {
	    IDeviceElementSchema schema = request.getDeviceElementSchema();
	    if (schema == null) {
		schema = new DeviceElementSchema();
	    }
	    spec.setDeviceElementSchema((DeviceElementSchema) schema);
	}

	MetadataProvider.copy(request.getMetadata(), spec);
	SiteWherePersistence.initializeEntityMetadata(spec);
	return spec;
    }

    /**
     * Common logic for updating a device specification from request.
     * 
     * @param request
     * @param target
     * @throws SiteWhereException
     */
    public static void deviceSpecificationUpdateLogic(IDeviceSpecificationCreateRequest request,
	    DeviceSpecification target) throws SiteWhereException {
	if (request.getName() != null) {
	    target.setName(request.getName());
	}
	if (request.getContainerPolicy() != null) {
	    target.setContainerPolicy(request.getContainerPolicy());
	}
	// Only allow schema to be set if new or existing container policy is
	// 'composite'.
	if (target.getContainerPolicy() == DeviceContainerPolicy.Composite) {
	    if (request.getContainerPolicy() == DeviceContainerPolicy.Standalone) {
		target.setDeviceElementSchema(null);
	    } else {
		IDeviceElementSchema schema = request.getDeviceElementSchema();
		if (schema == null) {
		    schema = new DeviceElementSchema();
		}
		target.setDeviceElementSchema((DeviceElementSchema) schema);
	    }
	}
	if (request.getAssetModuleId() != null) {
	    target.setAssetModuleId(request.getAssetModuleId());
	}
	if (request.getAssetId() != null) {
	    target.setAssetId(request.getAssetId());
	}
	if (request.getMetadata() != null) {
	    target.getMetadata().clear();
	    MetadataProvider.copy(request.getMetadata(), target);
	}
	SiteWherePersistence.setUpdatedEntityMetadata(target);
    }

    /**
     * Common logic for creating new device command and populating it from
     * request.
     * 
     * @param request
     * @param token
     * @return
     * @throws SiteWhereException
     */
    public static DeviceCommand deviceCommandCreateLogic(IDeviceSpecification spec, IDeviceCommandCreateRequest request,
	    String token, List<IDeviceCommand> existing) throws SiteWhereException {
	DeviceCommand command = new DeviceCommand();

	// Token is required.
	if (token == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	command.setToken(token);

	// Name is required.
	if (request.getName() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	command.setName(request.getName());

	command.setSpecificationToken(spec.getToken());
	command.setNamespace(request.getNamespace());
	command.setDescription(request.getDescription());
	command.getParameters().addAll(request.getParameters());

	checkDuplicateCommand(command, existing);

	MetadataProvider.copy(request.getMetadata(), command);
	SiteWherePersistence.initializeEntityMetadata(command);
	return command;
    }

    /**
     * Checks whether a command is already in the given list (same name and
     * namespace).
     * 
     * @param command
     * @param existing
     * @throws SiteWhereException
     */
    protected static void checkDuplicateCommand(DeviceCommand command, List<IDeviceCommand> existing)
	    throws SiteWhereException {
	boolean duplicate = false;
	for (IDeviceCommand current : existing) {
	    if (current.getName().equals(command.getName())) {
		if (current.getNamespace() == null) {
		    if (command.getNamespace() == null) {
			duplicate = true;
			break;
		    }
		} else if (current.getNamespace().equals(command.getNamespace())) {
		    duplicate = true;
		    break;
		}
	    }
	}
	if (duplicate) {
	    throw new SiteWhereSystemException(ErrorCode.DeviceCommandExists, ErrorLevel.ERROR);
	}
    }

    /**
     * Common logic for updating a device command from request.
     * 
     * @param request
     * @param target
     * @param existing
     * @throws SiteWhereException
     */
    public static void deviceCommandUpdateLogic(IDeviceCommandCreateRequest request, DeviceCommand target,
	    List<IDeviceCommand> existing) throws SiteWhereException {
	if (request.getName() != null) {
	    target.setName(request.getName());
	}
	if (request.getNamespace() != null) {
	    target.setNamespace(request.getNamespace());
	}

	// Make sure the update will not result in a duplicate.
	checkDuplicateCommand(target, existing);

	if (request.getDescription() != null) {
	    target.setDescription(request.getDescription());
	}
	if (request.getParameters() != null) {
	    target.getParameters().clear();
	    target.getParameters().addAll(request.getParameters());
	}
	if (request.getMetadata() != null) {
	    target.getMetadata().clear();
	    MetadataProvider.copy(request.getMetadata(), target);
	}
	SiteWherePersistence.setUpdatedEntityMetadata(target);
    }

    /**
     * Common logic for creating new device object and populating it from
     * request.
     * 
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static Device deviceCreateLogic(IDeviceCreateRequest request) throws SiteWhereException {
	Device device = new Device();
	if (request.getHardwareId() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	device.setHardwareId(request.getHardwareId());

	if (request.getSiteToken() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	device.setSiteToken(request.getSiteToken());

	if (request.getSpecificationToken() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	device.setSpecificationToken(request.getSpecificationToken());

	device.setComments(request.getComments());
	device.setStatus(DeviceStatus.Ok);

	MetadataProvider.copy(request.getMetadata(), device);
	SiteWherePersistence.initializeEntityMetadata(device);
	return device;
    }

    /**
     * Common logic for updating a device object from request.
     * 
     * @param request
     * @param target
     * @throws SiteWhereException
     */
    public static void deviceUpdateLogic(IDeviceCreateRequest request, Device target) throws SiteWhereException {
	// Can not update the hardware id on a device.
	if ((request.getHardwareId() != null) && (!request.getHardwareId().equals(target.getHardwareId()))) {
	    throw new SiteWhereSystemException(ErrorCode.DeviceHardwareIdCanNotBeChanged, ErrorLevel.ERROR);
	}
	if (request.getSiteToken() != null) {
	    // Can not change the site for an assigned device.
	    if (target.getAssignmentToken() != null) {
		if (!target.getSiteToken().equals(request.getSiteToken())) {
		    throw new SiteWhereSystemException(ErrorCode.DeviceSiteCanNotBeChangedIfAssigned, ErrorLevel.ERROR);
		}
	    }
	    target.setSiteToken(request.getSiteToken());
	}
	if (request.getSpecificationToken() != null) {
	    target.setSpecificationToken(request.getSpecificationToken());
	}
	if (request.isRemoveParentHardwareId() == Boolean.TRUE) {
	    target.setParentHardwareId(null);
	}
	if (request.getParentHardwareId() != null) {
	    target.setParentHardwareId(request.getParentHardwareId());
	}
	if (request.getDeviceElementMappings() != null) {
	    List<DeviceElementMapping> mappings = new ArrayList<DeviceElementMapping>();
	    for (IDeviceElementMapping mapping : request.getDeviceElementMappings()) {
		mappings.add(DeviceElementMapping.copy(mapping));
	    }
	    target.setDeviceElementMappings(mappings);
	}
	if (request.getComments() != null) {
	    target.setComments(request.getComments());
	}
	if (request.getStatus() != null) {
	    target.setStatus(request.getStatus());
	}
	if (request.getMetadata() != null) {
	    target.getMetadata().clear();
	    MetadataProvider.copy(request.getMetadata(), target);
	}
	SiteWherePersistence.setUpdatedEntityMetadata(target);
    }

    /**
     * Encapsulates logic for creating a new {@link IDeviceElementMapping}.
     * 
     * @param management
     * @param hardwareId
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static IDevice deviceElementMappingCreateLogic(IDeviceManagement management, String hardwareId,
	    IDeviceElementMapping request) throws SiteWhereException {
	IDevice device = management.getDeviceByHardwareId(hardwareId);
	if (device == null) {
	    throw new SiteWhereSystemException(ErrorCode.InvalidHardwareId, ErrorLevel.ERROR);
	}
	IDevice mapped = management.getDeviceByHardwareId(request.getHardwareId());
	if (mapped == null) {
	    throw new SiteWhereException("Device referenced by mapping does not exist.");
	}

	// Check whether target device is already parented to another device.
	if (mapped.getParentHardwareId() != null) {
	    throw new SiteWhereSystemException(ErrorCode.DeviceParentMappingExists, ErrorLevel.ERROR);
	}

	// Verify that requested path is valid on device specification.
	IDeviceSpecification specification = management.getDeviceSpecificationByToken(device.getSpecificationToken());
	DeviceSpecificationUtils.getDeviceSlotByPath(specification, request.getDeviceElementSchemaPath());

	// Verify that there is not an existing mapping for the path.
	List<IDeviceElementMapping> existing = device.getDeviceElementMappings();
	List<DeviceElementMapping> newMappings = new ArrayList<DeviceElementMapping>();
	for (IDeviceElementMapping mapping : existing) {
	    if (mapping.getDeviceElementSchemaPath().equals(request.getDeviceElementSchemaPath())) {
		throw new SiteWhereSystemException(ErrorCode.DeviceElementMappingExists, ErrorLevel.ERROR);
	    }
	    newMappings.add(DeviceElementMapping.copy(mapping));
	}
	DeviceElementMapping newMapping = DeviceElementMapping.copy(request);
	newMappings.add(newMapping);

	// Add parent backreference for nested device.
	DeviceCreateRequest nested = new DeviceCreateRequest();
	nested.setParentHardwareId(hardwareId);
	management.updateDevice(mapped.getHardwareId(), nested);

	// Update device with new mapping.
	DeviceCreateRequest update = new DeviceCreateRequest();
	update.setDeviceElementMappings(newMappings);
	IDevice updated = management.updateDevice(hardwareId, update);
	return updated;
    }

    /**
     * Encapsulates logic for deleting an {@link IDeviceElementMapping}.
     * 
     * @param management
     * @param hardwareId
     * @param path
     * @return
     * @throws SiteWhereException
     */
    public static IDevice deviceElementMappingDeleteLogic(IDeviceManagement management, String hardwareId, String path)
	    throws SiteWhereException {
	IDevice device = management.getDeviceByHardwareId(hardwareId);
	if (device == null) {
	    throw new SiteWhereSystemException(ErrorCode.InvalidHardwareId, ErrorLevel.ERROR);
	}

	// Verify that mapping exists and build list without deleted mapping.
	List<IDeviceElementMapping> existing = device.getDeviceElementMappings();
	List<DeviceElementMapping> newMappings = new ArrayList<DeviceElementMapping>();
	IDeviceElementMapping match = null;
	for (IDeviceElementMapping mapping : existing) {
	    if (mapping.getDeviceElementSchemaPath().equals(path)) {
		match = mapping;
	    } else {
		newMappings.add(DeviceElementMapping.copy(mapping));
	    }
	}
	if (match == null) {
	    throw new SiteWhereSystemException(ErrorCode.DeviceElementMappingDoesNotExist, ErrorLevel.ERROR);
	}

	// Remove parent reference from nested device.
	IDevice mapped = management.getDeviceByHardwareId(match.getHardwareId());
	if (mapped != null) {
	    DeviceCreateRequest nested = new DeviceCreateRequest();
	    nested.setRemoveParentHardwareId(true);
	    management.updateDevice(mapped.getHardwareId(), nested);
	}

	// Update device with new mappings.
	DeviceCreateRequest update = new DeviceCreateRequest();
	update.setDeviceElementMappings(newMappings);
	IDevice updated = management.updateDevice(hardwareId, update);
	return updated;
    }

    /**
     * Common logic for creating new site object and populating it from request.
     * 
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static Site siteCreateLogic(ISiteCreateRequest request) throws SiteWhereException {
	Site site = new Site();

	if (request.getToken() != null) {
	    site.setToken(request.getToken());
	} else {
	    site.setToken(UUID.randomUUID().toString());
	}

	site.setName(request.getName());
	site.setDescription(request.getDescription());
	site.setImageUrl(request.getImageUrl());
	site.setMap(SiteMapData.copy(request.getMap()));

	SiteWherePersistence.initializeEntityMetadata(site);
	MetadataProvider.copy(request.getMetadata(), site);
	return site;
    }

    /**
     * Common logic for copying data from site update request to existing site.
     * 
     * @param request
     * @param target
     * @throws SiteWhereException
     */
    public static void siteUpdateLogic(ISiteCreateRequest request, Site target) throws SiteWhereException {
	if (request.getName() != null) {
	    target.setName(request.getName());
	}
	if (request.getDescription() != null) {
	    target.setDescription(request.getDescription());
	}
	if (request.getImageUrl() != null) {
	    target.setImageUrl(request.getImageUrl());
	}
	if (request.getMap() != null) {
	    target.setMap(SiteMapData.copy(request.getMap()));
	}
	if (request.getMetadata() != null) {
	    target.getMetadata().clear();
	    MetadataProvider.copy(request.getMetadata(), target);
	}
	SiteWherePersistence.setUpdatedEntityMetadata(target);
    }

    /**
     * Common logic for creating a device assignment from a request.
     * 
     * @param source
     * @param device
     * @param uuid
     * @return
     * @throws SiteWhereException
     */
    public static DeviceAssignment deviceAssignmentCreateLogic(IDeviceAssignmentCreateRequest source, IDevice device)
	    throws SiteWhereException {
	DeviceAssignment newAssignment = new DeviceAssignment();
	newAssignment.setToken(source.getToken());

	// Copy site token from device.
	newAssignment.setSiteToken(device.getSiteToken());

	if (source.getDeviceHardwareId() == null) {
	    throw new SiteWhereSystemException(ErrorCode.InvalidHardwareId, ErrorLevel.ERROR);
	}
	newAssignment.setDeviceHardwareId(source.getDeviceHardwareId());

	if (source.getAssignmentType() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	newAssignment.setAssignmentType(source.getAssignmentType());

	if (source.getAssignmentType() == DeviceAssignmentType.Associated) {
	    if (source.getAssetModuleId() == null) {
		throw new SiteWhereSystemException(ErrorCode.InvalidAssetReferenceId, ErrorLevel.ERROR);
	    }
	    newAssignment.setAssetModuleId(source.getAssetModuleId());

	    if (source.getAssetId() == null) {
		throw new SiteWhereSystemException(ErrorCode.InvalidAssetReferenceId, ErrorLevel.ERROR);
	    }
	    newAssignment.setAssetId(source.getAssetId());
	}

	newAssignment.setActiveDate(new Date());
	newAssignment.setStatus(DeviceAssignmentStatus.Active);

	SiteWherePersistence.initializeEntityMetadata(newAssignment);
	MetadataProvider.copy(source.getMetadata(), newAssignment);

	return newAssignment;
    }

    /**
     * Executes logic to process a batch of device events.
     * 
     * @param assignmentToken
     * @param batch
     * @param management
     * @return
     * @throws SiteWhereException
     */
    public static DeviceEventBatchResponse deviceEventBatchLogic(String assignmentToken, IDeviceEventBatch batch,
	    IDeviceEventManagement management) throws SiteWhereException {
	DeviceEventBatchResponse response = new DeviceEventBatchResponse();
	for (IDeviceMeasurementsCreateRequest measurements : batch.getMeasurements()) {
	    response.getCreatedMeasurements().add(management.addDeviceMeasurements(assignmentToken, measurements));
	}
	for (IDeviceLocationCreateRequest location : batch.getLocations()) {
	    response.getCreatedLocations().add(management.addDeviceLocation(assignmentToken, location));
	}
	for (IDeviceAlertCreateRequest alert : batch.getAlerts()) {
	    response.getCreatedAlerts().add(management.addDeviceAlert(assignmentToken, alert));
	}
	return response;
    }

    /**
     * Common creation logic for all device events.
     * 
     * @param request
     * @param assignment
     * @param target
     * @throws SiteWhereException
     */
    public static void deviceEventCreateLogic(IDeviceEventCreateRequest request, IDeviceAssignment assignment,
	    DeviceEvent target) throws SiteWhereException {
	target.setSiteToken(assignment.getSiteToken());
	target.setDeviceAssignmentToken(assignment.getToken());
	target.setAssignmentType(assignment.getAssignmentType());
	target.setAssetModuleId(assignment.getAssetModuleId());
	target.setAssetId(assignment.getAssetId());
	if (request.getEventDate() != null) {
	    target.setEventDate(request.getEventDate());
	} else {
	    target.setEventDate(new Date());
	}
	target.setReceivedDate(new Date());
	MetadataProvider.copy(request.getMetadata(), target);
    }

    /**
     * Common logic for creating {@link DeviceMeasurements} from
     * {@link IDeviceMeasurementsCreateRequest}.
     * 
     * @param request
     * @param assignment
     * @return
     * @throws SiteWhereException
     */
    public static DeviceMeasurements deviceMeasurementsCreateLogic(IDeviceMeasurementsCreateRequest request,
	    IDeviceAssignment assignment) throws SiteWhereException {
	DeviceMeasurements measurements = new DeviceMeasurements();
	deviceEventCreateLogic(request, assignment, measurements);
	for (String key : request.getMeasurements().keySet()) {
	    measurements.addOrReplaceMeasurement(key, request.getMeasurement(key));
	}
	return measurements;
    }

    /**
     * Common logic for creating {@link DeviceLocation} from
     * {@link IDeviceLocationCreateRequest}.
     * 
     * @param assignment
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static DeviceLocation deviceLocationCreateLogic(IDeviceAssignment assignment,
	    IDeviceLocationCreateRequest request) throws SiteWhereException {
	DeviceLocation location = new DeviceLocation();
	deviceEventCreateLogic(request, assignment, location);
	location.setLatitude(request.getLatitude());
	location.setLongitude(request.getLongitude());
	location.setElevation(request.getElevation());
	return location;
    }

    /**
     * Common logic for creating {@link DeviceAlert} from
     * {@link IDeviceAlertCreateRequest}.
     * 
     * @param assignment
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static DeviceAlert deviceAlertCreateLogic(IDeviceAssignment assignment, IDeviceAlertCreateRequest request)
	    throws SiteWhereException {
	DeviceAlert alert = new DeviceAlert();
	deviceEventCreateLogic(request, assignment, alert);

	if (request.getSource() != null) {
	    alert.setSource(request.getSource());
	} else {
	    alert.setSource(AlertSource.Device);
	}

	if (request.getLevel() != null) {
	    alert.setLevel(request.getLevel());
	} else {
	    alert.setLevel(AlertLevel.Info);
	}

	alert.setType(request.getType());
	alert.setMessage(request.getMessage());
	return alert;
    }

    /**
     * Common logic for creating {@link DeviceStream} from
     * {@link IDeviceStreamCreateRequest}.
     * 
     * @param assignment
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static DeviceStream deviceStreamCreateLogic(IDeviceAssignment assignment, IDeviceStreamCreateRequest request)
	    throws SiteWhereException {
	DeviceStream stream = new DeviceStream();
	stream.setAssignmentToken(assignment.getToken());

	// Verify the stream id is specified and contains only valid characters.
	assureData(request.getStreamId());
	if (!request.getStreamId().matches("^[a-zA-Z0-9_\\-]+$")) {
	    throw new SiteWhereSystemException(ErrorCode.InvalidCharsInStreamId, ErrorLevel.ERROR);
	}
	stream.setStreamId(request.getStreamId());

	// Content type is required.
	assureData(request.getContentType());
	stream.setContentType(request.getContentType());

	MetadataProvider.copy(request.getMetadata(), stream);
	SiteWherePersistence.initializeEntityMetadata(stream);
	return stream;
    }

    /**
     * Common logic for creating {@link DeviceStreamData} from
     * {@link IDeviceStreamDataCreateRequest}.
     * 
     * @param assignment
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static DeviceStreamData deviceStreamDataCreateLogic(IDeviceAssignment assignment,
	    IDeviceStreamDataCreateRequest request) throws SiteWhereException {
	DeviceStreamData streamData = new DeviceStreamData();
	deviceEventCreateLogic(request, assignment, streamData);

	assureData(request.getStreamId());
	streamData.setStreamId(request.getStreamId());

	assureData(request.getSequenceNumber());
	streamData.setSequenceNumber(request.getSequenceNumber());

	streamData.setData(request.getData());
	return streamData;
    }

    /**
     * Common logic for creating {@link DeviceCommandInvocation} from an
     * {@link IDeviceCommandInvocationCreateRequest}.
     * 
     * @param assignment
     * @param command
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static DeviceCommandInvocation deviceCommandInvocationCreateLogic(IDeviceAssignment assignment,
	    IDeviceCommand command, IDeviceCommandInvocationCreateRequest request) throws SiteWhereException {
	if (request.getInitiator() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	if (request.getTarget() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	for (ICommandParameter parameter : command.getParameters()) {
	    checkData(parameter, request.getParameterValues());
	}
	DeviceCommandInvocation ci = new DeviceCommandInvocation();
	deviceEventCreateLogic(request, assignment, ci);
	ci.setCommandToken(command.getToken());
	ci.setInitiator(request.getInitiator());
	ci.setInitiatorId(request.getInitiatorId());
	ci.setTarget(request.getTarget());
	ci.setTargetId(request.getTargetId());
	ci.setParameterValues(request.getParameterValues());
	if (request.getStatus() != null) {
	    ci.setStatus(request.getStatus());
	} else {
	    ci.setStatus(CommandStatus.Pending);
	}
	return ci;
    }

    /**
     * Verify that data supplied for command parameters is valid.
     * 
     * @param parameter
     * @param values
     * @throws SiteWhereException
     */
    protected static void checkData(ICommandParameter parameter, Map<String, String> values) throws SiteWhereException {
	// Make sure required fields are passed.
	if (parameter.isRequired()) {
	    if (values.get(parameter.getName()) == null) {
		throw new SiteWhereException("Required parameter '" + parameter.getName() + "' is missing.");
	    }
	}
	// If no value, do not try to validate.
	String value = values.get(parameter.getName());
	if (value == null) {
	    return;
	}
	switch (parameter.getType()) {
	case Fixed32:
	case Fixed64:
	case Int32:
	case Int64:
	case SFixed32:
	case SFixed64:
	case SInt32:
	case SInt64:
	case UInt32:
	case UInt64: {
	    try {
		Long.parseLong(value);
	    } catch (NumberFormatException e) {
		throw new SiteWhereException("Parameter '" + parameter.getName() + "' must be numeric.");
	    }
	}
	case Float: {
	    try {
		Float.parseFloat(value);
	    } catch (NumberFormatException e) {
		throw new SiteWhereException("Parameter '" + parameter.getName() + "' must be a float.");
	    }
	}
	case Double: {
	    try {
		Double.parseDouble(value);
	    } catch (NumberFormatException e) {
		throw new SiteWhereException("Parameter '" + parameter.getName() + "' must be a double.");
	    }
	}
	default:
	}
    }

    /**
     * Common logic for creating a {@link DeviceCommandResponse} from an
     * {@link IDeviceCommandResponseCreateRequest}.
     * 
     * @param assignment
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static DeviceCommandResponse deviceCommandResponseCreateLogic(IDeviceAssignment assignment,
	    IDeviceCommandResponseCreateRequest request) throws SiteWhereException {
	if (request.getOriginatingEventId() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	DeviceCommandResponse response = new DeviceCommandResponse();
	deviceEventCreateLogic(request, assignment, response);
	response.setOriginatingEventId(request.getOriginatingEventId());
	response.setResponseEventId(request.getResponseEventId());
	response.setResponse(request.getResponse());
	return response;
    }

    /**
     * Common logic for creating a {@link DeviceStateChange} from an
     * {@link IDeviceStateChangeCreateRequest}.
     * 
     * @param assignment
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static DeviceStateChange deviceStateChangeCreateLogic(IDeviceAssignment assignment,
	    IDeviceStateChangeCreateRequest request) throws SiteWhereException {
	if (request.getCategory() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	if (request.getType() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	DeviceStateChange state = new DeviceStateChange();
	deviceEventCreateLogic(request, assignment, state);
	state.setCategory(request.getCategory());
	state.setType(request.getType());
	state.setPreviousState(request.getPreviousState());
	state.setNewState(request.getNewState());
	if (request.getData() != null) {
	    state.getData().putAll(request.getData());
	}
	return state;
    }

    /**
     * Common logic for creating a zone based on an incoming request.
     * 
     * @param source
     * @param siteToken
     * @param uuid
     * @return
     * @throws SiteWhereException
     */
    public static Zone zoneCreateLogic(IZoneCreateRequest source, String siteToken, String uuid)
	    throws SiteWhereException {
	Zone zone = new Zone();
	zone.setToken(uuid);
	zone.setSiteToken(siteToken);
	zone.setName(source.getName());
	zone.setBorderColor(source.getBorderColor());
	zone.setFillColor(source.getFillColor());
	zone.setOpacity(source.getOpacity());

	SiteWherePersistence.initializeEntityMetadata(zone);
	MetadataProvider.copy(source.getMetadata(), zone);

	for (ILocation coordinate : source.getCoordinates()) {
	    zone.getCoordinates().add(coordinate);
	}
	return zone;
    }

    /**
     * Common code for copying information from an update request to an existing
     * zone.
     * 
     * @param request
     * @param target
     * @throws SiteWhereException
     */
    public static void zoneUpdateLogic(IZoneCreateRequest request, Zone target) throws SiteWhereException {
	target.setName(request.getName());
	target.setBorderColor(request.getBorderColor());
	target.setFillColor(request.getFillColor());
	target.setOpacity(request.getOpacity());

	target.getCoordinates().clear();
	for (ILocation coordinate : request.getCoordinates()) {
	    target.getCoordinates().add(coordinate);
	}

	if (request.getMetadata() != null) {
	    target.getMetadata().clear();
	    MetadataProvider.copy(request.getMetadata(), target);
	}
	SiteWherePersistence.setUpdatedEntityMetadata(target);
    }

    /**
     * Common logic for creating a device group based on an incoming request.
     * 
     * @param source
     * @param uuid
     * @return
     * @throws SiteWhereException
     */
    public static DeviceGroup deviceGroupCreateLogic(IDeviceGroupCreateRequest source, String uuid)
	    throws SiteWhereException {
	DeviceGroup group = new DeviceGroup();
	group.setToken(uuid);
	group.setName(source.getName());
	group.setDescription(source.getDescription());
	if (source.getRoles() != null) {
	    group.getRoles().addAll(source.getRoles());
	}

	SiteWherePersistence.initializeEntityMetadata(group);
	MetadataProvider.copy(source.getMetadata(), group);
	return group;
    }

    /**
     * Common logic for updating an existing device group.
     * 
     * @param request
     * @param target
     * @throws SiteWhereException
     */
    public static void deviceGroupUpdateLogic(IDeviceGroupCreateRequest request, DeviceGroup target)
	    throws SiteWhereException {
	target.setName(request.getName());
	target.setDescription(request.getDescription());

	if (request.getRoles() != null) {
	    target.getRoles().clear();
	    target.getRoles().addAll(request.getRoles());
	}

	if (request.getMetadata() != null) {
	    target.getMetadata().clear();
	    MetadataProvider.copy(request.getMetadata(), target);
	}
	SiteWherePersistence.setUpdatedEntityMetadata(target);
    }

    /**
     * Common logic for creating a new device group element.
     * 
     * @param source
     * @param groupToken
     * @param index
     * @return
     * @throws SiteWhereException
     */
    public static DeviceGroupElement deviceGroupElementCreateLogic(IDeviceGroupElementCreateRequest source,
	    String groupToken, long index) throws SiteWhereException {
	DeviceGroupElement element = new DeviceGroupElement();
	element.setGroupToken(groupToken);
	element.setIndex(index);
	element.setType(source.getType());
	element.setElementId(source.getElementId());
	element.setRoles(source.getRoles());
	return element;
    }

    /**
     * Common logic for creating a batch operation based on an incoming request.
     * 
     * @param source
     * @param uuid
     * @return
     * @throws SiteWhereException
     */
    public static BatchOperation batchOperationCreateLogic(IBatchOperationCreateRequest source, String uuid)
	    throws SiteWhereException {
	BatchOperation batch = new BatchOperation();
	batch.setToken(uuid);
	batch.setOperationType(source.getOperationType());
	batch.getParameters().putAll(source.getParameters());

	SiteWherePersistence.initializeEntityMetadata(batch);
	MetadataProvider.copy(source.getMetadata(), batch);
	return batch;
    }

    /**
     * Common logic for updating batch operation information.
     * 
     * @param request
     * @param target
     * @throws SiteWhereException
     */
    public static void batchOperationUpdateLogic(IBatchOperationUpdateRequest request, BatchOperation target)
	    throws SiteWhereException {
	if (request.getProcessingStatus() != null) {
	    target.setProcessingStatus(request.getProcessingStatus());
	}
	if (request.getProcessingStartedDate() != null) {
	    target.setProcessingStartedDate(request.getProcessingStartedDate());
	}
	if (request.getProcessingEndedDate() != null) {
	    target.setProcessingEndedDate(request.getProcessingEndedDate());
	}

	if (request.getMetadata() != null) {
	    target.getMetadata().clear();
	    MetadataProvider.copy(request.getMetadata(), target);
	}
	SiteWherePersistence.setUpdatedEntityMetadata(target);
    }

    /**
     * Common logic for creating a batch operation element.
     * 
     * @param batchOperationToken
     * @param hardwareId
     * @param index
     * @return
     * @throws SiteWhereException
     */
    public static BatchElement batchElementCreateLogic(String batchOperationToken, String hardwareId, long index)
	    throws SiteWhereException {
	BatchElement element = new BatchElement();
	element.setBatchOperationToken(batchOperationToken);
	element.setHardwareId(hardwareId);
	element.setIndex(index);
	element.setProcessingStatus(ElementProcessingStatus.Unprocessed);
	element.setProcessedDate(null);
	return element;
    }

    /**
     * Common logic for updating a batch operation element.
     * 
     * @param request
     * @param element
     * @throws SiteWhereException
     */
    public static void batchElementUpdateLogic(IBatchElementUpdateRequest request, BatchElement element)
	    throws SiteWhereException {
	if (request.getProcessingStatus() != null) {
	    element.setProcessingStatus(request.getProcessingStatus());
	}
	if (request.getProcessedDate() != null) {
	    element.setProcessedDate(request.getProcessedDate());
	}
	if (request.getMetadata() != null) {
	    element.getMetadata().clear();
	    MetadataProvider.copy(request.getMetadata(), element);
	}
    }

    /**
     * Encodes batch command invocation parameters into the generic
     * {@link IBatchOperationCreateRequest} format.
     * 
     * @param request
     * @param uuid
     * @return
     * @throws SiteWhereException
     */
    public static IBatchOperationCreateRequest batchCommandInvocationCreateLogic(IBatchCommandInvocationRequest request,
	    String uuid) throws SiteWhereException {
	BatchOperationCreateRequest batch = new BatchOperationCreateRequest();
	batch.setToken(uuid);
	batch.setOperationType(OperationType.InvokeCommand);
	batch.setHardwareIds(request.getHardwareIds());
	batch.getParameters().put(IBatchCommandInvocationRequest.PARAM_COMMAND_TOKEN, request.getCommandToken());
	Map<String, String> params = new HashMap<String, String>();
	for (String key : request.getParameterValues().keySet()) {
	    params.put(key, request.getParameterValues().get(key));
	}
	batch.setMetadata(params);
	return batch;
    }

    /**
     * Common logic for creating a user based on an incoming request.
     * 
     * @param source
     * @param encodePassword
     * @return
     * @throws SiteWhereException
     */
    public static User userCreateLogic(IUserCreateRequest source, boolean encodePassword) throws SiteWhereException {
	String password = (encodePassword) ? passwordEncoder.encodePassword(source.getPassword(), null)
		: source.getPassword();

	User user = new User();
	user.setUsername(source.getUsername());
	user.setHashedPassword(password);
	user.setFirstName(source.getFirstName());
	user.setLastName(source.getLastName());
	user.setLastLogin(null);
	user.setStatus(source.getStatus());
	user.setAuthorities(source.getAuthorities());

	MetadataProvider.copy(source, user);
	SiteWherePersistence.initializeEntityMetadata(user);
	return user;
    }

    /**
     * Common code for copying information from an update request to an existing
     * user.
     * 
     * @param source
     * @param target
     * @param encodePassword
     * @throws SiteWhereException
     */
    public static void userUpdateLogic(IUserCreateRequest source, User target, boolean encodePassword)
	    throws SiteWhereException {
	if (source.getUsername() != null) {
	    target.setUsername(source.getUsername());
	}
	if (source.getPassword() != null) {
	    String password = (encodePassword) ? passwordEncoder.encodePassword(source.getPassword(), null)
		    : source.getPassword();
	    target.setHashedPassword(password);
	}
	if (source.getFirstName() != null) {
	    target.setFirstName(source.getFirstName());
	}
	if (source.getLastName() != null) {
	    target.setLastName(source.getLastName());
	}
	if (source.getStatus() != null) {
	    target.setStatus(source.getStatus());
	}
	if (source.getAuthorities() != null) {
	    target.setAuthorities(source.getAuthorities());
	}
	if (source.getMetadata() != null) {
	    target.getMetadata().clear();
	    MetadataProvider.copy(source, target);
	}
	SiteWherePersistence.setUpdatedEntityMetadata(target);
    }

    /**
     * Common logic for deleting a user. Takes care of related tasks such as
     * deleting user id from tenant authorized users.
     * 
     * @param username
     * @throws SiteWhereException
     */
    public static void userDeleteLogic(String username) throws SiteWhereException {
	ITenantManagement management = SiteWhere.getServer().getTenantManagement();
	ISearchResults<ITenant> tenants = management.listTenants(new TenantSearchCriteria(1, 0));
	for (ITenant tenant : tenants.getResults()) {
	    if (tenant.getAuthorizedUserIds().contains(username)) {
		TenantCreateRequest request = new TenantCreateRequest();
		List<String> ids = tenant.getAuthorizedUserIds();
		ids.remove(username);
		request.setAuthorizedUserIds(ids);
		management.updateTenant(tenant.getId(), request);
	    }
	}
    }

    /**
     * Common logic for creating a granted authority based on an incoming
     * request.
     * 
     * @param source
     * @return
     * @throws SiteWhereException
     */
    public static GrantedAuthority grantedAuthorityCreateLogic(IGrantedAuthorityCreateRequest source)
	    throws SiteWhereException {
	GrantedAuthority auth = new GrantedAuthority();
	auth.setAuthority(source.getAuthority());
	auth.setDescription(source.getDescription());
	auth.setParent(source.getParent());
	auth.setGroup(source.isGroup());
	return auth;
    }

    /**
     * Common logic for creating a tenant.
     * 
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static Tenant tenantCreateLogic(ITenantCreateRequest request) throws SiteWhereException {
	Tenant tenant = new Tenant();

	// Id is required.
	assureData(request.getId());
	tenant.setId(request.getId());

	// Name is required.
	assureData(request.getName());
	tenant.setName(request.getName());

	// Logo is required.
	assureData(request.getLogoUrl());
	tenant.setLogoUrl(request.getLogoUrl());

	// Auth token is required.
	assureData(request.getAuthenticationToken());
	tenant.setAuthenticationToken(request.getAuthenticationToken());

	// Tenant template is required.
	assureData(request.getTenantTemplateId());
	tenant.setTenantTemplateId(request.getTenantTemplateId());

	tenant.getAuthorizedUserIds().addAll(request.getAuthorizedUserIds());

	MetadataProvider.copy(request.getMetadata(), tenant);
	SiteWherePersistence.initializeEntityMetadata(tenant);

	return tenant;
    }

    /**
     * Common logic for updating an existing tenant.
     * 
     * @param request
     * @param existing
     * @return
     * @throws SiteWhereException
     */
    public static Tenant tenantUpdateLogic(ITenantCreateRequest request, Tenant existing) throws SiteWhereException {
	if ((request.getId() != null) && (!request.getId().equals(existing.getId()))) {
	    throw new SiteWhereException("Can not change the id of an existing tenant.");
	}

	if (request.getTenantTemplateId() != null) {
	    if (!request.getTenantTemplateId().equals(existing.getTenantTemplateId())) {
		throw new SiteWhereException("Can not change the template of an existing tenant.");
	    }
	}

	if (request.getName() != null) {
	    existing.setName(request.getName());
	}

	if (request.getLogoUrl() != null) {
	    existing.setLogoUrl(request.getLogoUrl());
	}

	if (request.getAuthenticationToken() != null) {
	    existing.setAuthenticationToken(request.getAuthenticationToken());
	}

	if (request.getAuthorizedUserIds() != null) {
	    existing.getAuthorizedUserIds().clear();
	    existing.getAuthorizedUserIds().addAll(request.getAuthorizedUserIds());
	}

	if (request.getMetadata() != null) {
	    existing.getMetadata().clear();
	    MetadataProvider.copy(request.getMetadata(), existing);
	}
	SiteWherePersistence.setUpdatedEntityMetadata(existing);

	return existing;
    }

    /**
     * Common tenant list logic.
     * 
     * @param results
     * @param criteria
     * @throws SiteWhereException
     */
    public static void tenantListLogic(List<ITenant> results, ITenantSearchCriteria criteria)
	    throws SiteWhereException {
	if (criteria.isIncludeRuntimeInfo()) {
	    for (ITenant tenant : results) {
		ISiteWhereTenantEngine engine = SiteWhere.getServer().getTenantEngine(tenant.getId());
		if (engine != null) {
		    ((Tenant) tenant).setEngineState(engine.getEngineState());
		}
	    }
	}
    }

    /**
     * Common logic for creating a tenant group.
     * 
     * @param token
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static TenantGroup tenantGroupCreateLogic(String token, ITenantGroupCreateRequest request)
	    throws SiteWhereException {
	TenantGroup group = new TenantGroup();
	group.setToken(token);

	// Name is required.
	assureData(request.getName());
	group.setName(request.getName());

	// Description is optional.
	group.setDescription(request.getDescription());

	// Image URL is optional.
	group.setImageUrl(request.getImageUrl());

	MetadataProvider.copy(request.getMetadata(), group);
	SiteWherePersistence.initializeEntityMetadata(group);

	return group;
    }

    /**
     * Common logic for updating an existing tenant group.
     * 
     * @param request
     * @param existing
     * @return
     * @throws SiteWhereException
     */
    public static TenantGroup tenantGroupUpdateLogic(ITenantGroupCreateRequest request, TenantGroup existing)
	    throws SiteWhereException {
	if (request.getName() != null) {
	    existing.setName(request.getName());
	}

	if (request.getDescription() != null) {
	    existing.setDescription(request.getDescription());
	}

	if (request.getImageUrl() != null) {
	    existing.setImageUrl(request.getImageUrl());
	}

	if (request.getMetadata() != null) {
	    existing.getMetadata().clear();
	    MetadataProvider.copy(request.getMetadata(), existing);
	}
	SiteWherePersistence.setUpdatedEntityMetadata(existing);

	return existing;
    }

    /**
     * Common logic for creating a new tenant group element.
     * 
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static TenantGroupElement tenantGroupElementCreateLogic(String groupToken,
	    ITenantGroupElementCreateRequest request) throws SiteWhereException {
	TenantGroupElement element = new TenantGroupElement();
	element.setTenantGroupToken(groupToken);
	element.setTenantId(request.getTenantId());
	return element;
    }

    /**
     * Common logic for creating an asset category.
     * 
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static AssetCategory assetCategoryCreateLogic(IAssetCategoryCreateRequest request)
	    throws SiteWhereException {
	AssetCategory category = new AssetCategory();

	assureData(request.getId());
	category.setId(request.getId());

	// Name is required.
	assureData(request.getName());
	category.setName(request.getName());

	// Type is required.
	assureData(request.getAssetType());
	category.setAssetType(request.getAssetType());

	return category;
    }

    /**
     * Common logic for updating an existing asset category.
     * 
     * @param request
     * @param existing
     * @return
     * @throws SiteWhereException
     */
    public static AssetCategory assetCategoryUpdateLogic(IAssetCategoryCreateRequest request, AssetCategory existing)
	    throws SiteWhereException {
	if (!request.getId().equals(existing.getId())) {
	    throw new SiteWhereException("Can not change the id of an existing asset category.");
	}

	if (request.getAssetType() != existing.getAssetType()) {
	    throw new SiteWhereException("Can not change the asset type of an existing asset category.");
	}

	if (request.getName() != null) {
	    existing.setName(request.getName());
	}

	return existing;
    }

    /**
     * Handle base logic common to all asset types.
     * 
     * @param category
     * @param request
     * @param asset
     * @throws SiteWhereException
     */
    public static void assetCreateLogic(IAssetCategory category, IAssetCreateRequest request, Asset asset)
	    throws SiteWhereException {
	asset.setType(category.getAssetType());

	assureData(category.getId());
	asset.setAssetCategoryId(category.getId());

	assureData(request.getId());
	asset.setId(request.getId());

	assureData(request.getName());
	asset.setName(request.getName());

	assureData(request.getImageUrl());
	asset.setImageUrl(request.getImageUrl());

	asset.getProperties().putAll(request.getProperties());
    }

    /**
     * Common logic for updating assets.
     * 
     * @param asset
     * @param request
     * @throws SiteWhereException
     */
    public static void assetUpdateLogic(Asset asset, IAssetCreateRequest request) throws SiteWhereException {
	if (!asset.getId().equals(request.getId())) {
	    throw new SiteWhereException("Asset id can not be changed.");
	}

	if (request.getName() != null) {
	    asset.setName(request.getName());
	}
	if (request.getImageUrl() != null) {
	    asset.setImageUrl(request.getImageUrl());
	}
	if (request.getProperties() != null) {
	    asset.getProperties().clear();
	    asset.getProperties().putAll(request.getProperties());
	}
    }

    /**
     * Handle common logic for creating a person asset.
     * 
     * @param category
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static PersonAsset personAssetCreateLogic(IAssetCategory category, IPersonAssetCreateRequest request)
	    throws SiteWhereException {
	if (category.getAssetType() != AssetType.Person) {
	    throw new SiteWhereSystemException(ErrorCode.AssetTypeNotAllowed, ErrorLevel.ERROR);
	}

	PersonAsset person = new PersonAsset();
	assetCreateLogic(category, request, person);

	person.setUserName(request.getUserName());
	person.setEmailAddress(request.getEmailAddress());
	person.getRoles().addAll(request.getRoles());

	return person;
    }

    /**
     * Handle common logic for updating a person asset.
     * 
     * @param person
     * @param request
     * @throws SiteWhereException
     */
    public static void personAssetUpdateLogic(PersonAsset person, IPersonAssetCreateRequest request)
	    throws SiteWhereException {
	assetUpdateLogic(person, request);

	if (request.getUserName() != null) {
	    person.setUserName(request.getUserName());
	}
	if (request.getEmailAddress() != null) {
	    person.setEmailAddress(request.getEmailAddress());
	}
	if (request.getRoles() != null) {
	    person.getRoles().clear();
	    person.getRoles().addAll(request.getRoles());
	}
    }

    /**
     * Handle common logic for creating a hardware asset.
     * 
     * @param category
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static HardwareAsset hardwareAssetCreateLogic(IAssetCategory category, IHardwareAssetCreateRequest request)
	    throws SiteWhereException {
	if ((category.getAssetType() != AssetType.Hardware) && (category.getAssetType() != AssetType.Device)) {
	    throw new SiteWhereSystemException(ErrorCode.AssetTypeNotAllowed, ErrorLevel.ERROR);
	}

	HardwareAsset hardware = new HardwareAsset();
	assetCreateLogic(category, request, hardware);

	hardware.setSku(request.getSku());
	hardware.setDescription(request.getDescription());

	return hardware;
    }

    /**
     * Handle common logic for updating a hardware asset.
     * 
     * @param hardware
     * @param request
     * @throws SiteWhereException
     */
    public static void hardwareAssetUpdateLogic(HardwareAsset hardware, IHardwareAssetCreateRequest request)
	    throws SiteWhereException {
	assetUpdateLogic(hardware, request);

	if (request.getSku() != null) {
	    hardware.setSku(request.getSku());
	}
	if (request.getDescription() != null) {
	    hardware.setDescription(request.getDescription());
	}
    }

    /**
     * Handle common logic for creating a location asset.
     * 
     * @param category
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static LocationAsset locationAssetCreateLogic(IAssetCategory category, ILocationAssetCreateRequest request)
	    throws SiteWhereException {
	if (category.getAssetType() != AssetType.Location) {
	    throw new SiteWhereSystemException(ErrorCode.AssetTypeNotAllowed, ErrorLevel.ERROR);
	}

	LocationAsset loc = new LocationAsset();
	assetCreateLogic(category, request, loc);

	loc.setLatitude(request.getLatitude());
	loc.setLongitude(request.getLongitude());
	loc.setElevation(request.getElevation());

	return loc;
    }

    /**
     * Handle common logic for updating a location asset.
     * 
     * @param location
     * @param request
     * @throws SiteWhereException
     */
    public static void locationAssetUpdateLogic(LocationAsset location, ILocationAssetCreateRequest request)
	    throws SiteWhereException {
	assetUpdateLogic(location, request);

	if (request.getLatitude() != null) {
	    location.setLatitude(request.getLatitude());
	}
	if (request.getLongitude() != null) {
	    location.setLongitude(request.getLongitude());
	}
	if (request.getElevation() != null) {
	    location.setElevation(request.getElevation());
	}
    }

    /**
     * Handle common logic for creating a schedule.
     * 
     * @param request
     * @param token
     * @return
     * @throws SiteWhereException
     */
    public static Schedule scheduleCreateLogic(IScheduleCreateRequest request, String token) throws SiteWhereException {
	Schedule schedule = new Schedule();

	// Unique token is required.
	if (token == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	schedule.setToken(token);

	// Name is required.
	if (request.getName() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	schedule.setName(request.getName());

	// Trigger type is required.
	if (request.getTriggerType() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	schedule.setTriggerType(request.getTriggerType());
	schedule.setTriggerConfiguration(request.getTriggerConfiguration());

	schedule.setStartDate(request.getStartDate());
	schedule.setEndDate(request.getEndDate());

	SiteWherePersistence.initializeEntityMetadata(schedule);
	MetadataProvider.copy(request.getMetadata(), schedule);

	return schedule;
    }

    /**
     * Handle common logic for updating a schedule.
     * 
     * @param schedule
     * @param request
     * @throws SiteWhereException
     */
    public static void scheduleUpdateLogic(Schedule schedule, IScheduleCreateRequest request)
	    throws SiteWhereException {
	if (request.getName() != null) {
	    schedule.setName(request.getName());
	}
	if (request.getTriggerType() != null) {
	    schedule.setTriggerType(request.getTriggerType());
	}
	if (request.getTriggerConfiguration() != null) {
	    schedule.getTriggerConfiguration().clear();
	    schedule.getTriggerConfiguration().putAll(request.getTriggerConfiguration());
	}

	schedule.setStartDate(request.getStartDate());
	schedule.setEndDate(request.getEndDate());

	SiteWherePersistence.setUpdatedEntityMetadata(schedule);
    }

    /**
     * Handle common logic for creating a scheduled job.
     * 
     * @param request
     * @param token
     * @return
     * @throws SiteWhereException
     */
    public static ScheduledJob scheduledJobCreateLogic(IScheduledJobCreateRequest request, String token)
	    throws SiteWhereException {
	ScheduledJob job = new ScheduledJob();

	// Unique token is required.
	if (token == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	job.setToken(token);

	// Schedule token is required.
	if (request.getScheduleToken() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	job.setScheduleToken(request.getScheduleToken());

	// Job type is required.
	if (request.getJobType() == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
	job.setJobType(request.getJobType());
	job.setJobConfiguration(request.getJobConfiguration());

	job.setJobState(ScheduledJobState.Unsubmitted);

	SiteWherePersistence.initializeEntityMetadata(job);
	MetadataProvider.copy(request.getMetadata(), job);

	return job;
    }

    /**
     * Handle common logic for updating a scheduled job.
     * 
     * @param job
     * @param request
     * @throws SiteWhereException
     */
    public static void scheduledJobUpdateLogic(ScheduledJob job, IScheduledJobCreateRequest request)
	    throws SiteWhereException {
	if (request.getScheduleToken() != null) {
	    job.setScheduleToken(request.getScheduleToken());
	}
	if (request.getJobType() != null) {
	    job.setJobType(request.getJobType());
	}
	if (request.getJobConfiguration() != null) {
	    job.getJobConfiguration().clear();
	    job.getJobConfiguration().putAll(request.getJobConfiguration());
	}
	if (request.getJobState() != null) {
	    job.setJobState(request.getJobState());
	}
	SiteWherePersistence.setUpdatedEntityMetadata(job);
    }

    /**
     * Throw an exception if data is missing.
     * 
     * @param data
     * @throws SiteWhereException
     */
    protected static void assureData(Object data) throws SiteWhereException {
	if (data == null) {
	    throw new SiteWhereSystemException(ErrorCode.IncompleteData, ErrorLevel.ERROR);
	}
    }

    /**
     * Common logic for encoding a plaintext password.
     * 
     * @param plaintext
     * @return
     */
    public static String encodePassword(String plaintext) {
	return passwordEncoder.encodePassword(plaintext, null);
    }
}