/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.groovy.device.event.processor.routing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sitewhere.groovy.configuration.GroovyConfiguration;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.device.IDevice;
import com.sitewhere.spi.device.IDeviceAssignment;
import com.sitewhere.spi.device.event.IDeviceEvent;
import com.sitewhere.spi.device.event.processor.routing.IRouteBuilder;

import groovy.lang.Binding;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

/**
 * Uses Groovy script to build routes for event processor routing.
 * 
 * @author Derek
 */
public class GroovyRouteBuilder implements IRouteBuilder<String> {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /** Injected global Groovy configuration */
    private GroovyConfiguration configuration;

    /** Relative path to Groovy script */
    private String scriptPath;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.device.event.processor.routing.IRouteBuilder#build(com.
     * sitewhere .spi.device.event.IDeviceEvent,
     * com.sitewhere.spi.device.IDevice,
     * com.sitewhere.spi.device.IDeviceAssignment)
     */
    @Override
    public String build(IDeviceEvent event, IDevice device, IDeviceAssignment assignment) throws SiteWhereException {
	Binding binding = new Binding();
	binding.setVariable("logger", LOGGER);
	binding.setVariable("event", event);
	binding.setVariable("device", device);
	binding.setVariable("assignment", assignment);
	try {
	    Object result = getConfiguration().getGroovyScriptEngine().run(getScriptPath(), binding);
	    if (!(result instanceof String)) {
		throw new SiteWhereException("Groovy route builder expected script to return a String.");
	    }
	    return (String) result;
	} catch (ResourceException e) {
	    throw new SiteWhereException("Unable to access Groovy route builder script.", e);
	} catch (ScriptException e) {
	    throw new SiteWhereException("Unable to run Groovy route builder script.", e);
	}
    }

    public GroovyConfiguration getConfiguration() {
	return configuration;
    }

    public void setConfiguration(GroovyConfiguration configuration) {
	this.configuration = configuration;
    }

    public String getScriptPath() {
	return scriptPath;
    }

    public void setScriptPath(String scriptPath) {
	this.scriptPath = scriptPath;
    }
}