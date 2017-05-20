/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.rest.model.device;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sitewhere.rest.model.common.MetadataProviderEntity;
import com.sitewhere.spi.device.DeviceStatus;
import com.sitewhere.spi.device.IDevice;
import com.sitewhere.spi.device.IDeviceElementMapping;
import com.sitewhere.spi.device.element.IDeviceElementSchema;

/**
 * Model object for device information.
 * 
 * @author dadams
 */
@JsonInclude(Include.NON_NULL)
public class Device extends MetadataProviderEntity implements IDevice, Serializable {

    /** Serialization version identifier */
    private static final long serialVersionUID = 5558679468461313408L;

    /** Unique hardware id for device */
    private String hardwareId;

    /** Site token */
    private String siteToken;

    /** Specification token */
    private String specificationToken;

    /** Parent hardware id (if nested) */
    private String parentHardwareId;

    /** Mappings of {@link IDeviceElementSchema} paths to hardware ids */
    private List<DeviceElementMapping> deviceElementMappings = new ArrayList<DeviceElementMapping>();

    /** Comments */
    private String comments;

    /** Status indicator */
    private DeviceStatus status;

    /** Token for current assignment */
    private String assignmentToken;

    /** FIELDS BELOW DEPEND ON MARSHALING PARAMETERS */

    /** Device specification */
    private DeviceSpecification specification;

    /** Current device assignment */
    private DeviceAssignment assignment;

    /** Current site deployment */
    private Site site;

    /** Asset id from device specification (only for marshaling) */
    private String assetId;

    /** Asset name from device specification (only for marshaling) */
    private String assetName;

    /** Asset image url from device specification (only for marshaling) */
    private String assetImageUrl;

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.IDevice#getHardwareId()
     */
    public String getHardwareId() {
	return hardwareId;
    }

    public void setHardwareId(String hardwareId) {
	this.hardwareId = hardwareId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.IDevice#getSiteToken()
     */
    public String getSiteToken() {
	return siteToken;
    }

    public void setSiteToken(String siteToken) {
	this.siteToken = siteToken;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.IDevice#getSpecificationToken()
     */
    public String getSpecificationToken() {
	return specificationToken;
    }

    public void setSpecificationToken(String specificationToken) {
	this.specificationToken = specificationToken;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.IDevice#getParentHardwareId()
     */
    public String getParentHardwareId() {
	return parentHardwareId;
    }

    public void setParentHardwareId(String parentHardwareId) {
	this.parentHardwareId = parentHardwareId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.IDevice#getDeviceElementMappings()
     */
    @SuppressWarnings("unchecked")
    public List<IDeviceElementMapping> getDeviceElementMappings() {
	return (List<IDeviceElementMapping>) (List<? extends IDeviceElementMapping>) deviceElementMappings;
    }

    public void setDeviceElementMappings(List<DeviceElementMapping> deviceElementMappings) {
	this.deviceElementMappings = deviceElementMappings;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.IDevice#getComments()
     */
    public String getComments() {
	return comments;
    }

    public void setComments(String comments) {
	this.comments = comments;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.IDevice#getStatus()
     */
    public DeviceStatus getStatus() {
	return status;
    }

    public void setStatus(DeviceStatus status) {
	this.status = status;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.IDevice#getAssignmentToken()
     */
    public String getAssignmentToken() {
	return assignmentToken;
    }

    public void setAssignmentToken(String assignmentToken) {
	this.assignmentToken = assignmentToken;
    }

    public DeviceSpecification getSpecification() {
	return specification;
    }

    public void setSpecification(DeviceSpecification specification) {
	this.specification = specification;
    }

    public DeviceAssignment getAssignment() {
	return assignment;
    }

    public void setAssignment(DeviceAssignment assignment) {
	this.assignment = assignment;
    }

    public Site getSite() {
	return site;
    }

    public void setSite(Site site) {
	this.site = site;
    }

    public String getAssetId() {
	return assetId;
    }

    public void setAssetId(String assetId) {
	this.assetId = assetId;
    }

    public String getAssetName() {
	return assetName;
    }

    public void setAssetName(String assetName) {
	this.assetName = assetName;
    }

    public String getAssetImageUrl() {
	return assetImageUrl;
    }

    public void setAssetImageUrl(String assetImageUrl) {
	this.assetImageUrl = assetImageUrl;
    }
}