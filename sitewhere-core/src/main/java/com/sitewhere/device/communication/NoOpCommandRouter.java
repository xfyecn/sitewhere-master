/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.device.communication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.device.IDeviceAssignment;
import com.sitewhere.spi.device.IDeviceNestingContext;
import com.sitewhere.spi.device.command.IDeviceCommandExecution;
import com.sitewhere.spi.device.command.ISystemCommand;
import com.sitewhere.spi.device.communication.IOutboundCommandRouter;

/**
 * Implementation of {@link IOutboundCommandRouter} that does nothing with
 * commands. Used as default for situations where command routing is not needed.
 * 
 * @author Derek
 */
public class NoOpCommandRouter extends OutboundCommandRouter {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.communication.IOutboundCommandRouter#
     * routeCommand(com. sitewhere.spi.device.command.IDeviceCommandExecution,
     * com.sitewhere.spi.device.IDeviceNestingContext,
     * com.sitewhere.spi.device.IDeviceAssignment)
     */
    @Override
    public void routeCommand(IDeviceCommandExecution execution, IDeviceNestingContext nesting,
	    IDeviceAssignment assignment) throws SiteWhereException {
	LOGGER.warn("Ignoring routing of command. Add a command router to allow commands to be sent to destinations.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.communication.IOutboundCommandRouter#
     * routeSystemCommand (com.sitewhere.spi.device.command.ISystemCommand,
     * com.sitewhere.spi.device.IDeviceNestingContext,
     * com.sitewhere.spi.device.IDeviceAssignment)
     */
    @Override
    public void routeSystemCommand(ISystemCommand command, IDeviceNestingContext nesting, IDeviceAssignment assignment)
	    throws SiteWhereException {
	LOGGER.warn(
		"Ignoring routing of system command. Add a command router to allow commands to be sent to destinations.");
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