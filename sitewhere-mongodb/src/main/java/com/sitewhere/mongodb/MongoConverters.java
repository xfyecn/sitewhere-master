/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.mongodb;

import java.util.HashMap;
import java.util.Map;

import com.sitewhere.mongodb.asset.MongoAsset;
import com.sitewhere.mongodb.asset.MongoAssetCategory;
import com.sitewhere.mongodb.asset.MongoHardwareAsset;
import com.sitewhere.mongodb.asset.MongoLocationAsset;
import com.sitewhere.mongodb.asset.MongoPersonAsset;
import com.sitewhere.mongodb.device.MongoBatchElement;
import com.sitewhere.mongodb.device.MongoBatchOperation;
import com.sitewhere.mongodb.device.MongoDevice;
import com.sitewhere.mongodb.device.MongoDeviceAlert;
import com.sitewhere.mongodb.device.MongoDeviceAssignment;
import com.sitewhere.mongodb.device.MongoDeviceCommand;
import com.sitewhere.mongodb.device.MongoDeviceCommandInvocation;
import com.sitewhere.mongodb.device.MongoDeviceCommandResponse;
import com.sitewhere.mongodb.device.MongoDeviceGroup;
import com.sitewhere.mongodb.device.MongoDeviceGroupElement;
import com.sitewhere.mongodb.device.MongoDeviceLocation;
import com.sitewhere.mongodb.device.MongoDeviceMeasurements;
import com.sitewhere.mongodb.device.MongoDeviceSpecification;
import com.sitewhere.mongodb.device.MongoDeviceStateChange;
import com.sitewhere.mongodb.device.MongoDeviceStream;
import com.sitewhere.mongodb.device.MongoDeviceStreamData;
import com.sitewhere.mongodb.device.MongoSite;
import com.sitewhere.mongodb.device.MongoZone;
import com.sitewhere.mongodb.scheduling.MongoSchedule;
import com.sitewhere.mongodb.scheduling.MongoScheduledJob;
import com.sitewhere.mongodb.tenant.MongoTenant;
import com.sitewhere.mongodb.tenant.MongoTenantGroup;
import com.sitewhere.mongodb.tenant.MongoTenantGroupElement;
import com.sitewhere.spi.asset.IAsset;
import com.sitewhere.spi.asset.IAssetCategory;
import com.sitewhere.spi.asset.IHardwareAsset;
import com.sitewhere.spi.asset.ILocationAsset;
import com.sitewhere.spi.asset.IPersonAsset;
import com.sitewhere.spi.device.IDevice;
import com.sitewhere.spi.device.IDeviceAssignment;
import com.sitewhere.spi.device.IDeviceSpecification;
import com.sitewhere.spi.device.ISite;
import com.sitewhere.spi.device.IZone;
import com.sitewhere.spi.device.batch.IBatchElement;
import com.sitewhere.spi.device.batch.IBatchOperation;
import com.sitewhere.spi.device.command.IDeviceCommand;
import com.sitewhere.spi.device.event.IDeviceAlert;
import com.sitewhere.spi.device.event.IDeviceCommandInvocation;
import com.sitewhere.spi.device.event.IDeviceCommandResponse;
import com.sitewhere.spi.device.event.IDeviceLocation;
import com.sitewhere.spi.device.event.IDeviceMeasurements;
import com.sitewhere.spi.device.event.IDeviceStateChange;
import com.sitewhere.spi.device.event.IDeviceStreamData;
import com.sitewhere.spi.device.group.IDeviceGroup;
import com.sitewhere.spi.device.group.IDeviceGroupElement;
import com.sitewhere.spi.device.streaming.IDeviceStream;
import com.sitewhere.spi.scheduling.ISchedule;
import com.sitewhere.spi.scheduling.IScheduledJob;
import com.sitewhere.spi.tenant.ITenant;
import com.sitewhere.spi.tenant.ITenantGroup;
import com.sitewhere.spi.tenant.ITenantGroupElement;

/**
 * Manages classes used to convert between Mongo and SPI objects.
 * 
 * @author Derek
 */
public class MongoConverters implements IMongoConverterLookup {

    /** Maps interface classes to their associated converters */
    private static Map<Class<?>, MongoConverter<?>> CONVERTERS = new HashMap<Class<?>, MongoConverter<?>>();

    /** Create a list of converters for various types */
    static {
	// Converters for device management.
	CONVERTERS.put(IDeviceSpecification.class, new MongoDeviceSpecification());
	CONVERTERS.put(IDeviceCommand.class, new MongoDeviceCommand());
	CONVERTERS.put(IDevice.class, new MongoDevice());
	CONVERTERS.put(IDeviceAssignment.class, new MongoDeviceAssignment());
	CONVERTERS.put(IDeviceMeasurements.class, new MongoDeviceMeasurements());
	CONVERTERS.put(IDeviceAlert.class, new MongoDeviceAlert());
	CONVERTERS.put(IDeviceLocation.class, new MongoDeviceLocation());
	CONVERTERS.put(IDeviceStream.class, new MongoDeviceStream());
	CONVERTERS.put(IDeviceStreamData.class, new MongoDeviceStreamData());
	CONVERTERS.put(IDeviceCommandInvocation.class, new MongoDeviceCommandInvocation());
	CONVERTERS.put(IDeviceCommandResponse.class, new MongoDeviceCommandResponse());
	CONVERTERS.put(IDeviceStateChange.class, new MongoDeviceStateChange());
	CONVERTERS.put(ISite.class, new MongoSite());
	CONVERTERS.put(IZone.class, new MongoZone());
	CONVERTERS.put(IDeviceGroup.class, new MongoDeviceGroup());
	CONVERTERS.put(IDeviceGroupElement.class, new MongoDeviceGroupElement());
	CONVERTERS.put(IBatchOperation.class, new MongoBatchOperation());
	CONVERTERS.put(IBatchElement.class, new MongoBatchElement());

	// Converters for tenant management.
	CONVERTERS.put(ITenant.class, new MongoTenant());
	CONVERTERS.put(ITenantGroup.class, new MongoTenantGroup());
	CONVERTERS.put(ITenantGroupElement.class, new MongoTenantGroupElement());

	// Converters for asset management.
	CONVERTERS.put(IAssetCategory.class, new MongoAssetCategory());
	CONVERTERS.put(IHardwareAsset.class, new MongoHardwareAsset());
	CONVERTERS.put(IPersonAsset.class, new MongoPersonAsset());
	CONVERTERS.put(ILocationAsset.class, new MongoLocationAsset());
	CONVERTERS.put(IAsset.class, new MongoAsset());

	// Converters for schedule management.
	CONVERTERS.put(ISchedule.class, new MongoSchedule());
	CONVERTERS.put(IScheduledJob.class, new MongoScheduledJob());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.mongodb.IMongoConverterLookup#getConverterFor(java.lang.
     * Class)
     */
    @SuppressWarnings("unchecked")
    public <T> MongoConverter<T> getConverterFor(Class<T> api) {
	MongoConverter<T> result = (MongoConverter<T>) CONVERTERS.get(api);
	if (result == null) {
	    throw new RuntimeException("No Mongo converter registered for " + api.getName());
	}
	return result;
    }
}