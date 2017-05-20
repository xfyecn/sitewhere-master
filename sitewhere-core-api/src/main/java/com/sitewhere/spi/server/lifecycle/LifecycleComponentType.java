/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.spi.server.lifecycle;

/**
 * Enumerates types of components in the system.
 * 
 * @author Derek
 */
public enum LifecycleComponentType {

    /** Includes the entire system */
    System,

    /** Engine for a single tenant */
    TenantEngine,

    /** Data store management */
    DataStore,

    /** Cache provider */
    CacheProvider,

    /** Asset module manager */
    AssetModuleManager,

    /** Asset module */
    AssetModule,

    /** Search provider manager */
    SearchProviderManager,

    /** Search provider */
    SearchProvider,

    /** Outbound processor chain */
    OutboundProcessorChain,

    /** Outbound event processor */
    OutboundEventProcessor,

    /** Outbound event processor filter */
    OutboundEventProcessorFilter,

    /** Inbound processor chain */
    InboundProcessorChain,

    /** Inbound event processor */
    InboundEventProcessor,

    /** Device communication subsystem */
    DeviceCommunication,

    /** Event processing subsystem */
    EventProcessing,

    /** Command processing strategy */
    CommandProcessingStrategy,

    /** Command destination */
    CommandDestination,

    /** Command execution builder */
    CommandExecutionBuilder,

    /** Command execution encoder */
    CommandExecutionEncoder,

    /** Command target resolver */
    CommandTargetResolver,

    /** Command delivery provider */
    CommandDeliveryProvider,

    /** Command parameter extractor */
    CommandParameterExtractor,

    /** Command router */
    CommandRouter,

    /** Outbound processing strategy */
    OutboundProcessingStrategy,

    /** Registration manager */
    RegistrationManager,

    /** Symbol generator manager */
    SymbolGeneratorManager,

    /** Symbol generator */
    SymbolGenerator,

    /** Batch operation manager */
    BatchOperationManager,

    /** Device presence manager */
    DevicePresenceManager,

    /** Device stream manager */
    DeviceStreamManager,

    /** Schedule manager */
    ScheduleManager,

    /** Inbound processing strategy */
    InboundProcessingStrategy,

    /** Event source */
    InboundEventSource,

    /** Device event decoder */
    DeviceEventDecoder,

    /** Event receiver */
    InboundEventReceiver,

    /** Resource manager */
    ResourceManager,

    /** Tenant template manager */
    TenantTemplateManager,

    /** Unclassified component */
    Other,
}