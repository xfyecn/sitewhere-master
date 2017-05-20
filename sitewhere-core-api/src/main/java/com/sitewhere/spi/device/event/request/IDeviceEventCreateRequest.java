/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.spi.device.event.request;

import java.util.Date;
import java.util.Map;

/**
 * Interface for arguments needed to create a device event.
 * 
 * @author Derek
 */
public interface IDeviceEventCreateRequest {

    /**
     * Get the date on which the event occurred.
     * 
     * @return
     */
    public Date getEventDate();

    /**
     * Indicates whether state information on the device assignment should be
     * updated to reflect new event.
     * 
     * @return
     */
    public boolean isUpdateState();

    /**
     * Set indicator for whether state information on the device assignment
     * should be updated.
     * 
     * @param update
     */
    public void setUpdateState(boolean update);

    /**
     * Get metadata values.
     * 
     * @return
     */
    public Map<String, String> getMetadata();
}