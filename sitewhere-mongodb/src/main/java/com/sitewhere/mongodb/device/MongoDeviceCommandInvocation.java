/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.mongodb.device;

import java.util.HashMap;
import java.util.Map;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sitewhere.mongodb.MongoConverter;
import com.sitewhere.rest.model.device.event.DeviceCommandInvocation;
import com.sitewhere.spi.device.event.CommandInitiator;
import com.sitewhere.spi.device.event.CommandStatus;
import com.sitewhere.spi.device.event.CommandTarget;
import com.sitewhere.spi.device.event.IDeviceCommandInvocation;

/**
 * Used to load or save device command invocation data to MongoDB.
 * 
 * @author dadams
 */
public class MongoDeviceCommandInvocation implements MongoConverter<IDeviceCommandInvocation> {

    /** Property for initiator */
    public static final String PROP_INITIATOR = "initiator";

    /** Property for initiator id */
    public static final String PROP_INITIATOR_ID = "initiatorId";

    /** Property for target */
    public static final String PROP_TARGET = "target";

    /** Property for target id */
    public static final String PROP_TARGET_ID = "targetId";

    /** Property for command token */
    public static final String PROP_COMMAND_TOKEN = "commandToken";

    /** Property for parameter values */
    public static final String PROP_PARAMETER_VALUES = "parameterValues";

    /** Property for status */
    public static final String PROP_STATUS = "status";

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.mongodb.MongoConverter#convert(java.lang.Object)
     */
    @Override
    public BasicDBObject convert(IDeviceCommandInvocation source) {
	return MongoDeviceCommandInvocation.toDBObject(source);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.mongodb.MongoConverter#convert(com.mongodb.DBObject)
     */
    @Override
    public IDeviceCommandInvocation convert(DBObject source) {
	return MongoDeviceCommandInvocation.fromDBObject(source);
    }

    /**
     * Copy information from SPI into Mongo DBObject.
     * 
     * @param source
     * @param target
     */
    public static void toDBObject(IDeviceCommandInvocation source, BasicDBObject target) {
	MongoDeviceEvent.toDBObject(source, target, false);

	target.append(PROP_INITIATOR, source.getInitiator().name());
	target.append(PROP_INITIATOR_ID, source.getInitiatorId());
	target.append(PROP_TARGET, source.getTarget().name());
	target.append(PROP_TARGET_ID, source.getTargetId());
	target.append(PROP_COMMAND_TOKEN, source.getCommandToken());
	target.append(PROP_STATUS, source.getStatus().name());

	BasicDBObject params = new BasicDBObject();
	for (String key : source.getParameterValues().keySet()) {
	    params.append(key, source.getParameterValues().get(key));
	}
	target.append(PROP_PARAMETER_VALUES, params);
    }

    /**
     * Copy information from Mongo DBObject to model object.
     * 
     * @param source
     * @param target
     */
    public static void fromDBObject(DBObject source, DeviceCommandInvocation target) {
	MongoDeviceEvent.fromDBObject(source, target, false);

	String initiatorName = (String) source.get(PROP_INITIATOR);
	String initiatorId = (String) source.get(PROP_INITIATOR_ID);
	String targetName = (String) source.get(PROP_TARGET);
	String targetId = (String) source.get(PROP_TARGET_ID);
	String commandToken = (String) source.get(PROP_COMMAND_TOKEN);
	String statusName = (String) source.get(PROP_STATUS);

	if (initiatorName != null) {
	    target.setInitiator(CommandInitiator.valueOf(initiatorName));
	}
	if (targetName != null) {
	    target.setTarget(CommandTarget.valueOf(targetName));
	}
	if (statusName != null) {
	    target.setStatus(CommandStatus.valueOf(statusName));
	}
	target.setInitiatorId(initiatorId);
	target.setTargetId(targetId);
	target.setCommandToken(commandToken);

	Map<String, String> params = new HashMap<String, String>();
	DBObject dbparams = (DBObject) source.get(PROP_PARAMETER_VALUES);
	if (dbparams != null) {
	    for (String key : dbparams.keySet()) {
		params.put(key, (String) dbparams.get(key));
	    }
	}
	target.setParameterValues(params);
    }

    /**
     * Convert SPI object to Mongo DBObject.
     * 
     * @param source
     * @return
     */
    public static BasicDBObject toDBObject(IDeviceCommandInvocation source) {
	BasicDBObject result = new BasicDBObject();
	MongoDeviceCommandInvocation.toDBObject(source, result);
	return result;
    }

    /**
     * Convert a DBObject into the SPI equivalent.
     * 
     * @param source
     * @return
     */
    public static DeviceCommandInvocation fromDBObject(DBObject source) {
	DeviceCommandInvocation result = new DeviceCommandInvocation();
	MongoDeviceCommandInvocation.fromDBObject(source, result);
	return result;
    }
}