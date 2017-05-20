/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.spi.server.hazelcast;

/**
 * Constants for Hazelcast services used by SiteWhere.
 * 
 * @author Derek
 */
public interface ISiteWhereHazelcast {

    /** Topic name for newly added measurements */
    public static final String TOPIC_MEASUREMENTS_ADDED = "sitewhere.topic.measurements.added";

    /** Topic name for newly added locations */
    public static final String TOPIC_LOCATION_ADDED = "sitewhere.topic.location.added";

    /** Topic name for newly added alerts */
    public static final String TOPIC_ALERT_ADDED = "sitewhere.topic.alert.added";

    /** Topic name for newly added command invocations */
    public static final String TOPIC_COMMAND_INVOCATION_ADDED = "sitewhere.topic.command.invocation.added";

    /** Topic name for newly added command responses */
    public static final String TOPIC_COMMAND_RESPONSE_ADDED = "sitewhere.topic.command.response.added";

    /** Queue name for all SiteWhere events */
    public static final String QUEUE_ALL_EVENTS = "sitewhere.queue.all.events";
}