package com.sitewhere.spi.monitoring;

/**
 * Message sent to indicate progress for a long-running task.
 * 
 * @author Derek
 */
public interface IProgressMessage {

    /**
     * Get name of overall task being monitored.
     * 
     * @return
     */
    public String getTaskName();

    /**
     * Get count of all operations to be performed.
     * 
     * @return
     */
    public int getTotalOperations();

    /**
     * Get index of current operation.
     * 
     * @return
     */
    public int getCurrentOperation();

    /**
     * Get message shown for current operation.
     * 
     * @return
     */
    public String getMessage();
}