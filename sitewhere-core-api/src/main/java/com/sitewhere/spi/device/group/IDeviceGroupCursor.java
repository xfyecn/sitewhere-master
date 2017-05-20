/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.spi.device.group;

/**
 * Provides a stateful cursor for processing entries in an {@link IDeviceGroup}.
 * 
 * @author Derek
 */
public interface IDeviceGroupCursor {

    /**
     * Get unique network token.
     * 
     * @return
     */
    public String getGroupToken();

    /**
     * Get index of current device in list.
     * 
     * @return
     */
    public long getDeviceIndex();
}