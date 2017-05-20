/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.server.jvm;

import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.server.ISiteWhereServer;
import com.sitewhere.spi.server.ISiteWhereServerRuntime;

/**
 * Monitors JVM history values over time.
 * 
 * @author Derek
 */
public class JvmHistoryMonitor implements Runnable {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /** Number of measurements in history */
    private static final int HISTORY_LENGTH = 300;

    /** Update interval in milliseconds */
    private static final int UPDATE_INTERVAL = 3 * 1000;

    /** Server being monitored */
    private ISiteWhereServer server;

    /** Buffer for history of JVM total memory */
    private LinkedList<Long> totalMemory = new LinkedList<Long>();

    /** Buffer for history of JVM available memory */
    private LinkedList<Long> freeMemory = new LinkedList<Long>();

    public JvmHistoryMonitor(ISiteWhereServer server) {
	this.server = server;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
	LOGGER.info("Starting JVM history monitor.");
	while (true) {
	    try {
		// Get current server state.
		ISiteWhereServerRuntime runtime = getServer().getServerRuntimeInformation(false);

		// Store the latest entry and remove oldest if at limit.
		totalMemory.addLast(runtime.getJava().getJvmTotalMemory());
		if (totalMemory.size() > HISTORY_LENGTH) {
		    totalMemory.removeFirst();
		}

		// Store the latest entry and remove oldest if at limit.
		freeMemory.addLast(runtime.getJava().getJvmFreeMemory());
		if (freeMemory.size() > HISTORY_LENGTH) {
		    freeMemory.removeFirst();
		}
	    } catch (SiteWhereException e) {
		LOGGER.error("Unable to query server state.", e);
	    }

	    try {
		Thread.sleep(UPDATE_INTERVAL);
	    } catch (InterruptedException e) {
		LOGGER.error("JVM history monitoring interrupted.");
		return;
	    }
	}
    }

    public ISiteWhereServer getServer() {
	return server;
    }

    public void setServer(ISiteWhereServer server) {
	this.server = server;
    }

    public LinkedList<Long> getTotalMemory() {
	return totalMemory;
    }

    public void setTotalMemory(LinkedList<Long> totalMemory) {
	this.totalMemory = totalMemory;
    }

    public LinkedList<Long> getFreeMemory() {
	return freeMemory;
    }

    public void setFreeMemory(LinkedList<Long> freeMemory) {
	this.freeMemory = freeMemory;
    }
}