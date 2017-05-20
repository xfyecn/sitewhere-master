/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.device.marshaling;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sitewhere.SiteWhere;
import com.sitewhere.rest.model.device.command.DeviceCommand;
import com.sitewhere.rest.model.device.event.DeviceCommandInvocation;
import com.sitewhere.rest.model.device.event.DeviceEvent;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.device.IDeviceManagement;
import com.sitewhere.spi.device.command.IDeviceCommand;
import com.sitewhere.spi.device.event.IDeviceCommandInvocation;
import com.sitewhere.spi.tenant.ITenant;

/**
 * Configurable helper class that allows {@link DeviceCommandInvocation} model
 * objects to be created from {@link IDeviceCommandInvocation} SPI objects.
 * 
 * @author dadams
 */
public class DeviceCommandInvocationMarshalHelper {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /** Tenant */
    private ITenant tenant;

    /** Indicates whether to include command information */
    private boolean includeCommand = false;

    /** Cache to prevent repeated command lookups */
    private Map<String, DeviceCommand> commandsByToken = new HashMap<String, DeviceCommand>();

    public DeviceCommandInvocationMarshalHelper(ITenant tenant) {
	this(tenant, false);
    }

    public DeviceCommandInvocationMarshalHelper(ITenant tenant, boolean includeCommand) {
	this.tenant = tenant;
	this.includeCommand = includeCommand;
    }

    /**
     * Convert an {@link IDeviceCommandInvocation} to a
     * {@link DeviceCommandInvocation}, populating command information if
     * requested so the marshaled data includes it.
     * 
     * @param source
     * @return
     * @throws SiteWhereException
     */
    public DeviceCommandInvocation convert(IDeviceCommandInvocation source) throws SiteWhereException {
	DeviceCommandInvocation result = new DeviceCommandInvocation();
	DeviceEvent.copy(source, result);
	result.setInitiator(source.getInitiator());
	result.setInitiatorId(source.getInitiatorId());
	result.setTarget(source.getTarget());
	result.setTargetId(source.getTargetId());
	result.setCommandToken(source.getCommandToken());
	result.setStatus(source.getStatus());
	result.setParameterValues(source.getParameterValues());
	if (isIncludeCommand()) {
	    if ((source.getCommandToken() == null) || (source.getCommandToken().isEmpty())) {
		LOGGER.warn("Device invocation is missing command token.");
		return result;
	    }
	    DeviceCommand command = commandsByToken.get(source.getCommandToken());
	    if (command == null) {
		IDeviceCommand found = getDeviceManagement(tenant).getDeviceCommandByToken(source.getCommandToken());
		if (found == null) {
		    LOGGER.warn("Device invocation references a non-existent command token.");
		    return result;
		}
		command = DeviceCommand.copy(found);
		commandsByToken.put(command.getToken(), command);
	    }
	    if (command != null) {
		result.setCommand(command);
		result.setAsHtml(CommandHtmlHelper.getHtml(result));
	    }
	}
	return result;
    }

    protected IDeviceManagement getDeviceManagement(ITenant tenant) throws SiteWhereException {
	return SiteWhere.getServer().getDeviceManagement(tenant);
    }

    public boolean isIncludeCommand() {
	return includeCommand;
    }

    public void setIncludeCommand(boolean includeCommand) {
	this.includeCommand = includeCommand;
    }
}