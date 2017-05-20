/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.device.communication.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sitewhere.core.DataUtils;
import com.sitewhere.server.lifecycle.TenantLifecycleComponent;
import com.sitewhere.spi.device.communication.EventDecodeException;
import com.sitewhere.spi.device.communication.IDecodedDeviceRequest;
import com.sitewhere.spi.device.communication.IDeviceEventDecoder;
import com.sitewhere.spi.server.lifecycle.LifecycleComponentType;

/**
 * Implementation of {@link IDeviceEventDecoder} that logs the event payload but
 * does not actually produce any events. This is useful for debugging when
 * implementing decoders for hardware sending human-readable commands across the
 * wire.
 * 
 * @author Derek
 */
public class PayloadLoggerEventDecoder extends TenantLifecycleComponent implements IDeviceEventDecoder<byte[]> {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    public PayloadLoggerEventDecoder() {
	super(LifecycleComponentType.DeviceEventDecoder);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.device.communication.IDeviceEventDecoder#decode(java.
     * lang.Object, java.util.Map)
     */
    @Override
    public List<IDecodedDeviceRequest<?>> decode(byte[] payload, Map<String, Object> metadata)
	    throws EventDecodeException {
	LOGGER.info("=== EVENT DATA BEGIN ===");
	LOGGER.info(new String(payload));
	LOGGER.info("(hex) " + DataUtils.bytesToHex(payload));
	LOGGER.info("=== EVENT DATA END ===");
	return new ArrayList<IDecodedDeviceRequest<?>>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.lifecycle.ILifecycleComponent#getLogger()
     */
    @Override
    public Logger getLogger() {
	return LOGGER;
    }
}