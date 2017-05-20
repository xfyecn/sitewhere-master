/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.spi.device;

/**
 * Status indicator for device.
 * 
 * @author Derek
 */
public enum DeviceStatus {

    /** Device is ok */
    Ok,

    /** Device hardware failure */
    HardwareFailure,

    /** Device not available */
    Unavailable;
}