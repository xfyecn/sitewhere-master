/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.spi.cache;

public enum CacheType {

    /** Caches site information */
    SiteCache,

    /** Caches device specification information */
    DeviceSpecificationCache,

    /** Caches device information */
    DeviceCache,

    /** Caches device assignment information */
    DeviceAssignmentCache,

    /** Caches asset category information */
    AssetCategoryCache;
}