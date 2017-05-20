/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.device.communication;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sitewhere.server.SiteWhereServer;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.device.communication.IDecodedDeviceRequest;
import com.sitewhere.spi.device.communication.IInboundProcessingStrategy;
import com.sitewhere.spi.device.event.processor.IInboundEventProcessorChain;
import com.sitewhere.spi.device.event.request.IDeviceAlertCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceCommandResponseCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceLocationCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceMappingCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceMeasurementsCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceRegistrationRequest;
import com.sitewhere.spi.device.event.request.IDeviceStateChangeCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceStreamCreateRequest;
import com.sitewhere.spi.device.event.request.IDeviceStreamDataCreateRequest;
import com.sitewhere.spi.device.event.request.ISendDeviceStreamDataRequest;
import com.sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor;

/**
 * Implementation of {@link IInboundProcessingStrategy} that uses an
 * {@link ArrayBlockingQueue} to hold decoded events that are submitted into the
 * {@link IInboundEventProcessorChain}.
 * 
 * @author Derek
 */
public class BlockingQueueInboundProcessingStrategy extends InboundProcessingStrategy
	implements IInboundProcessingStrategy {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /** Maximum size of queues */
    private static final int MAX_QUEUE_SIZE = 10000;

    /** Number of threads used for event processing */
    private static final int EVENT_PROCESSOR_THREAD_COUNT = 100;

    /** Interval between monitoring log output messages */
    private static final int MONITORING_INTERVAL_SEC = 5;

    /** Maximum size of queue before blocking */
    private int maxQueueSize = MAX_QUEUE_SIZE;

    /** Number of thread used for event processing */
    private int eventProcessorThreadCount = EVENT_PROCESSOR_THREAD_COUNT;

    /** Indicates whether monitoring messages should be logged */
    private boolean enableMonitoring = false;

    /** Number of seconds between monitoring messages */
    private int monitoringIntervalSec = MONITORING_INTERVAL_SEC;

    /** Counter for number of events */
    private AtomicLong eventCount = new AtomicLong();

    /** Counter for number of errors */
    private AtomicLong errorCount = new AtomicLong();

    /** Total wait time */
    private AtomicLong totalWaitTime = new AtomicLong();

    /** Total processing time */
    private AtomicLong totalProcessingTime = new AtomicLong();

    /** Blocking queue of pending event create requests from event sources */
    private BlockingQueue<PerformanceWrapper> queue;

    /** Thread pool for processing events */
    private ExecutorService processorPool;

    /** Pool for monitoring thread */
    private ExecutorService monitorPool = Executors.newSingleThreadExecutor();

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.lifecycle.ILifecycleComponent#start(com.
     * sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor)
     */
    @Override
    public void start(ILifecycleProgressMonitor monitor) throws SiteWhereException {
	this.queue = new ArrayBlockingQueue<PerformanceWrapper>(getMaxQueueSize());
	processorPool = Executors.newFixedThreadPool(getEventProcessorThreadCount(), new ProcessorsThreadFactory());
	for (int i = 0; i < getEventProcessorThreadCount(); i++) {
	    processorPool.execute(new BlockingMessageProcessor(queue));
	}
	LOGGER.info("Started blocking queue inbound processing strategy with queue size of " + getMaxQueueSize()
		+ " and " + getEventProcessorThreadCount() + " threads.");

	// Only show monitoring data if enabled.
	if (isEnableMonitoring()) {
	    monitorPool.execute(new MonitorOutput());
	}
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

    /** Used for naming processor threads */
    private class ProcessorsThreadFactory implements ThreadFactory {

	/** Counts threads */
	private AtomicInteger counter = new AtomicInteger();

	public Thread newThread(Runnable r) {
	    return new Thread(r,
		    "SiteWhere BlockingQueueInboundProcessingStrategy Processor " + counter.incrementAndGet());
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.server.lifecycle.ILifecycleComponent#stop(com.sitewhere
     * .spi.server.lifecycle.ILifecycleProgressMonitor)
     */
    @Override
    public void stop(ILifecycleProgressMonitor monitor) throws SiteWhereException {
	if (processorPool != null) {
	    processorPool.shutdownNow();
	}
	if (monitorPool != null) {
	    monitorPool.shutdownNow();
	}
	LOGGER.info("Stopped blocking queue inbound processing strategy.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.communication.IInboundProcessingStrategy#
     * processRegistration
     * (com.sitewhere.spi.device.communication.IDecodedDeviceRequest)
     */
    @Override
    public void processRegistration(IDecodedDeviceRequest<IDeviceRegistrationRequest> request)
	    throws SiteWhereException {
	addRequestToQueue(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.communication.IInboundProcessingStrategy#
     * processDeviceCommandResponse
     * (com.sitewhere.spi.device.communication.IDecodedDeviceRequest)
     */
    @Override
    public void processDeviceCommandResponse(IDecodedDeviceRequest<IDeviceCommandResponseCreateRequest> request)
	    throws SiteWhereException {
	addRequestToQueue(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.communication.IInboundProcessingStrategy#
     * processDeviceMeasurements
     * (com.sitewhere.spi.device.communication.IDecodedDeviceEventRequest)
     */
    @Override
    public void processDeviceMeasurements(IDecodedDeviceRequest<IDeviceMeasurementsCreateRequest> request)
	    throws SiteWhereException {
	addRequestToQueue(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.communication.IInboundProcessingStrategy#
     * processDeviceLocation
     * (com.sitewhere.spi.device.communication.IDecodedDeviceEventRequest)
     */
    @Override
    public void processDeviceLocation(IDecodedDeviceRequest<IDeviceLocationCreateRequest> request)
	    throws SiteWhereException {
	addRequestToQueue(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.communication.IInboundProcessingStrategy#
     * processDeviceAlert
     * (com.sitewhere.spi.device.communication.IDecodedDeviceEventRequest)
     */
    @Override
    public void processDeviceAlert(IDecodedDeviceRequest<IDeviceAlertCreateRequest> request) throws SiteWhereException {
	addRequestToQueue(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.communication.IInboundProcessingStrategy#
     * processDeviceStateChange(com.sitewhere.spi.device.communication.
     * IDecodedDeviceRequest)
     */
    @Override
    public void processDeviceStateChange(IDecodedDeviceRequest<IDeviceStateChangeCreateRequest> request)
	    throws SiteWhereException {
	addRequestToQueue(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.communication.IInboundProcessingStrategy#
     * processDeviceStream
     * (com.sitewhere.spi.device.communication.IDecodedDeviceEventRequest)
     */
    @Override
    public void processDeviceStream(IDecodedDeviceRequest<IDeviceStreamCreateRequest> request)
	    throws SiteWhereException {
	addRequestToQueue(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.communication.IInboundProcessingStrategy#
     * processDeviceStreamData
     * (com.sitewhere.spi.device.communication.IDecodedDeviceEventRequest)
     */
    @Override
    public void processDeviceStreamData(IDecodedDeviceRequest<IDeviceStreamDataCreateRequest> request)
	    throws SiteWhereException {
	addRequestToQueue(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.communication.IInboundProcessingStrategy#
     * processSendDeviceStreamData
     * (com.sitewhere.spi.device.communication.IDecodedDeviceRequest)
     */
    @Override
    public void processSendDeviceStreamData(IDecodedDeviceRequest<ISendDeviceStreamDataRequest> request)
	    throws SiteWhereException {
	addRequestToQueue(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.communication.IInboundProcessingStrategy#
     * processCreateDeviceMapping(com.sitewhere.spi.device.communication.
     * IDecodedDeviceRequest)
     */
    @Override
    public void processCreateDeviceMapping(IDecodedDeviceRequest<IDeviceMappingCreateRequest> request)
	    throws SiteWhereException {
	addRequestToQueue(request);
    }

    /**
     * Adds an {@link IDecodedDeviceRequest} to the queue, blocking if no space
     * is available.
     * 
     * @param request
     * @throws SiteWhereException
     */
    protected void addRequestToQueue(IDecodedDeviceRequest<?> request) throws SiteWhereException {
	try {
	    eventCount.incrementAndGet();

	    PerformanceWrapper wrapper = new PerformanceWrapper();
	    wrapper.setRequest(request);
	    wrapper.setStartTime(System.currentTimeMillis());
	    queue.put(wrapper);
	} catch (InterruptedException e) {
	    errorCount.incrementAndGet();
	    throw new SiteWhereException(e);
	}
    }

    /**
     * Get the number of events processed.
     * 
     * @return
     */
    public long getEventCount() {
	return eventCount.get();
    }

    /**
     * Get the number of errors in processing.
     * 
     * @return
     */
    public long getErrorCount() {
	return errorCount.get();
    }

    /**
     * Get the number of backlogged requests.
     * 
     * @return
     */
    public long getBacklog() {
	return queue.size();
    }

    /**
     * Get the average wait time in milliseconds.
     * 
     * @return
     */
    public long getAverageProcessingWaitTime() {
	long total = totalWaitTime.get();
	long count = eventCount.get();
	if (count == 0) {
	    return 0;
	}
	return total / count;
    }

    /**
     * Get the average processing time in milliseconds.
     * 
     * @return
     */
    public long getAverageProcessingTime() {
	return 0;
    }

    /**
     * Get the average processing time of downstream components.
     * 
     * @return
     */
    public long getAverageDownstreamProcessingTime() {
	long total = totalProcessingTime.get();
	long count = eventCount.get();
	if (count == 0) {
	    return 0;
	}
	return total / count;
    }

    public int getMaxQueueSize() {
	return maxQueueSize;
    }

    public void setMaxQueueSize(int maxQueueSize) {
	this.maxQueueSize = maxQueueSize;
    }

    public int getEventProcessorThreadCount() {
	return eventProcessorThreadCount;
    }

    public void setEventProcessorThreadCount(int eventProcessorThreadCount) {
	this.eventProcessorThreadCount = eventProcessorThreadCount;
    }

    public boolean isEnableMonitoring() {
	return enableMonitoring;
    }

    public void setEnableMonitoring(boolean enableMonitoring) {
	this.enableMonitoring = enableMonitoring;
    }

    public int getMonitoringIntervalSec() {
	return monitoringIntervalSec;
    }

    public void setMonitoringIntervalSec(int monitoringIntervalSec) {
	this.monitoringIntervalSec = monitoringIntervalSec;
    }

    public class PerformanceWrapper {

	/** Start time for event processing */
	private long startTime;

	/** Event request */
	private IDecodedDeviceRequest<?> request;

	public long getStartTime() {
	    return startTime;
	}

	public void setStartTime(long startTime) {
	    this.startTime = startTime;
	}

	public IDecodedDeviceRequest<?> getRequest() {
	    return request;
	}

	public void setRequest(IDecodedDeviceRequest<?> request) {
	    this.request = request;
	}
    }

    /**
     * Logs monitor output at a given time interval.
     * 
     * @author Derek
     */
    public class MonitorOutput implements Runnable {

	@Override
	public void run() {
	    while (true) {
		try {
		    long eventCount = getEventCount();
		    long errorCount = getErrorCount();
		    long backlog = getBacklog();
		    long avgWaitTime = getAverageProcessingWaitTime();
		    long avgProcessingTime = getAverageProcessingTime();
		    long avgDownstreamTime = getAverageDownstreamProcessingTime();
		    String message = String.format(
			    "Count(%5d) Errors(%5d) Backlog(%5d) AvgWait(%5d ms) AvgProc(%5d ms) AvgDS(%5d ms)",
			    eventCount, errorCount, backlog, avgWaitTime, avgProcessingTime, avgDownstreamTime);
		    LOGGER.info(message);
		} catch (Throwable e) {
		    LOGGER.error(e);
		}
		try {
		    Thread.sleep(getMonitoringIntervalSec() * 1000);
		} catch (InterruptedException e) {
		}
	    }
	}
    }

    /**
     * Blocking thread that processes {@link IDecodedDeviceRequest} from a
     * queue.
     * 
     * @author Derek
     * 
     * @param <T>
     */
    private class BlockingMessageProcessor implements Runnable {

	/** Queue where messages are placed */
	private BlockingQueue<PerformanceWrapper> queue;

	public BlockingMessageProcessor(BlockingQueue<PerformanceWrapper> queue) {
	    this.queue = queue;
	}

	@Override
	public void run() {
	    // Event creation APIs expect an authenticated user in order to
	    // check permissions and log who creates events. When called in this
	    // context, the authenticated user will always be 'system'.
	    // TODO: Alternatively, we may want the client to authenticate on
	    // registration and pass a token on each request.
	    try {
		SecurityContextHolder.getContext().setAuthentication(SiteWhereServer.getSystemAuthentication());
	    } catch (SiteWhereException e) {
		throw new RuntimeException(
			"Unable to use system authentication for inbound device " + " event processor thread.", e);
	    }
	    while (true) {
		try {
		    PerformanceWrapper wrapper = queue.take();
		    long wait = System.currentTimeMillis() - wrapper.getStartTime();
		    totalWaitTime.addAndGet(wait);

		    long processingStart = System.currentTimeMillis();

		    sendToInboundProcessingChain(wrapper.getRequest());

		    long processingTime = System.currentTimeMillis() - processingStart;
		    totalProcessingTime.addAndGet(processingTime);
		} catch (SiteWhereException e) {
		    errorCount.incrementAndGet();
		    LOGGER.error("Error processing inbound device event.", e);
		} catch (InterruptedException e) {
		    break;
		} catch (Throwable e) {
		    errorCount.incrementAndGet();
		    LOGGER.error("Unhandled exception in inbound event processing.", e);
		}
	    }
	}
    }
}