/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.spi.device.event.state;

/**
 * Enumerates the types of state changes known to the system.
 * 
 * @author Derek
 */
public enum StateChangeType {

    /** Device assignment was created */
    Assignment_Created,

    /** Device assignment was updated */
    Assignment_Updated,

    /** Device assignment was released */
    Assignment_Released,

    /** Presence status was updated */
    Presence_Updated,

    /** Device registration request */
    Registration_Requested,
}