/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.hbase.device;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sitewhere.SiteWhere;
import com.sitewhere.Tracer;
import com.sitewhere.core.SiteWherePersistence;
import com.sitewhere.device.marshaling.DeviceSpecificationMarshalHelper;
import com.sitewhere.hbase.IHBaseContext;
import com.sitewhere.hbase.ISiteWhereHBase;
import com.sitewhere.hbase.common.HBaseUtils;
import com.sitewhere.hbase.common.IRowKeyBuilder;
import com.sitewhere.hbase.uid.UniqueIdCounterMap;
import com.sitewhere.hbase.uid.UniqueIdCounterMapRowKeyBuilder;
import com.sitewhere.rest.model.device.DeviceSpecification;
import com.sitewhere.rest.model.search.SearchResults;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.SiteWhereSystemException;
import com.sitewhere.spi.common.IFilter;
import com.sitewhere.spi.device.IDeviceSpecification;
import com.sitewhere.spi.device.request.IDeviceSpecificationCreateRequest;
import com.sitewhere.spi.error.ErrorCode;
import com.sitewhere.spi.error.ErrorLevel;
import com.sitewhere.spi.search.ISearchCriteria;
import com.sitewhere.spi.server.debug.TracerCategory;

/**
 * HBase specifics for dealing with SiteWhere device specifications.
 * 
 * @author Derek
 */
public class HBaseDeviceSpecification {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /** Length of specification identifier (subset of 8 byte long) */
    public static final int SPEC_IDENTIFIER_LENGTH = 4;

    /** Length of command identifier (subset of 8 byte long) */
    public static final int COMMAND_IDENTIFIER_LENGTH = 4;

    /** Column qualifier for command counter */
    public static final byte[] COMMAND_COUNTER = Bytes.toBytes("commandctr");

    /** Used to look up row keys from tokens */
    public static IRowKeyBuilder KEY_BUILDER = new UniqueIdCounterMapRowKeyBuilder() {

	@Override
	public UniqueIdCounterMap getMap(IHBaseContext context) {
	    return context.getDeviceIdManager().getSpecificationKeys();
	}

	@Override
	public byte getTypeIdentifier() {
	    return DeviceRecordType.DeviceSpecification.getType();
	}

	@Override
	public byte getPrimaryIdentifier() {
	    return DeviceSpecificationRecordType.DeviceSpecification.getType();
	}

	@Override
	public int getKeyIdLength() {
	    return 4;
	}

	@Override
	public void throwInvalidKey() throws SiteWhereException {
	    throw new SiteWhereSystemException(ErrorCode.InvalidDeviceSpecificationToken, ErrorLevel.ERROR);
	}
    };

    /**
     * Create a device specification.
     * 
     * @param context
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static IDeviceSpecification createDeviceSpecification(IHBaseContext context,
	    IDeviceSpecificationCreateRequest request) throws SiteWhereException {
	Tracer.push(TracerCategory.DeviceManagementApiCall, "createDeviceSpecification (HBase)", LOGGER);
	try {
	    String uuid = null;
	    if (request.getToken() != null) {
		uuid = context.getDeviceIdManager().getSpecificationKeys().useExistingId(request.getToken());
	    } else {
		uuid = context.getDeviceIdManager().getSpecificationKeys().createUniqueId();
	    }

	    // Use common logic so all backend implementations work the same.
	    DeviceSpecification specification = SiteWherePersistence.deviceSpecificationCreateLogic(request, uuid);

	    Map<byte[], byte[]> qualifiers = new HashMap<byte[], byte[]>();
	    byte[] maxLong = Bytes.toBytes(Long.MAX_VALUE);
	    qualifiers.put(COMMAND_COUNTER, maxLong);
	    IDeviceSpecification created = HBaseUtils.createOrUpdate(context, context.getPayloadMarshaler(),
		    ISiteWhereHBase.DEVICES_TABLE_NAME, specification, uuid, KEY_BUILDER, qualifiers);
	    if (context.getCacheProvider() != null) {
		context.getCacheProvider().getDeviceSpecificationCache().put(uuid, created);
	    }
	    return created;
	} finally {
	    Tracer.pop(LOGGER);
	}
    }

    /**
     * Get a device specification by unique token.
     * 
     * @param context
     * @param token
     * @return
     * @throws SiteWhereException
     */
    public static DeviceSpecification getDeviceSpecificationByToken(IHBaseContext context, String token)
	    throws SiteWhereException {
	Tracer.push(TracerCategory.DeviceManagementApiCall, "getDeviceSpecificationByToken (HBase) " + token, LOGGER);
	try {
	    if (context.getCacheProvider() != null) {
		IDeviceSpecification result = context.getCacheProvider().getDeviceSpecificationCache().get(token);
		if (result != null) {
		    Tracer.info("Returning cached device specification.", LOGGER);
		    DeviceSpecificationMarshalHelper helper = new DeviceSpecificationMarshalHelper(context.getTenant())
			    .setIncludeAsset(true);
		    return helper.convert(result, SiteWhere.getServer().getAssetModuleManager(context.getTenant()));
		}
	    }
	    DeviceSpecification found = HBaseUtils.get(context, ISiteWhereHBase.DEVICES_TABLE_NAME, token, KEY_BUILDER,
		    DeviceSpecification.class);
	    if ((context.getCacheProvider() != null) && (found != null)) {
		context.getCacheProvider().getDeviceSpecificationCache().put(token, found);
	    }
	    return found;
	} finally {
	    Tracer.pop(LOGGER);
	}
    }

    /**
     * Update an existing device specification.
     * 
     * @param context
     * @param token
     * @param request
     * @return
     * @throws SiteWhereException
     */
    public static IDeviceSpecification updateDeviceSpecification(IHBaseContext context, String token,
	    IDeviceSpecificationCreateRequest request) throws SiteWhereException {
	Tracer.push(TracerCategory.DeviceManagementApiCall, "updateDeviceSpecification (HBase) " + token, LOGGER);
	try {
	    DeviceSpecification updated = assertDeviceSpecification(context, token);
	    SiteWherePersistence.deviceSpecificationUpdateLogic(request, updated);
	    if (context.getCacheProvider() != null) {
		context.getCacheProvider().getDeviceSpecificationCache().put(token, updated);
	    }
	    return HBaseUtils.put(context, context.getPayloadMarshaler(), ISiteWhereHBase.DEVICES_TABLE_NAME, updated,
		    token, KEY_BUILDER);
	} finally {
	    Tracer.pop(LOGGER);
	}
    }

    /**
     * List device specifications that match the given search criteria.
     * 
     * @param context
     * @param includeDeleted
     * @param criteria
     * @return
     * @throws SiteWhereException
     */
    public static SearchResults<IDeviceSpecification> listDeviceSpecifications(IHBaseContext context,
	    boolean includeDeleted, ISearchCriteria criteria) throws SiteWhereException {
	Tracer.push(TracerCategory.DeviceManagementApiCall, "listDeviceSpecifications (HBase)", LOGGER);
	try {
	    Comparator<DeviceSpecification> comparator = new Comparator<DeviceSpecification>() {

		public int compare(DeviceSpecification a, DeviceSpecification b) {
		    return -1 * (a.getCreatedDate().compareTo(b.getCreatedDate()));
		}

	    };
	    IFilter<DeviceSpecification> filter = new IFilter<DeviceSpecification>() {

		public boolean isExcluded(DeviceSpecification item) {
		    return false;
		}
	    };
	    return HBaseUtils.getFilteredList(context, ISiteWhereHBase.DEVICES_TABLE_NAME, KEY_BUILDER, includeDeleted,
		    IDeviceSpecification.class, DeviceSpecification.class, filter, criteria, comparator);
	} finally {
	    Tracer.pop(LOGGER);
	}
    }

    /**
     * Either deletes a device specification or marks it as deleted.
     * 
     * @param context
     * @param token
     * @param force
     * @return
     * @throws SiteWhereException
     */
    public static IDeviceSpecification deleteDeviceSpecification(IHBaseContext context, String token, boolean force)
	    throws SiteWhereException {
	Tracer.push(TracerCategory.DeviceManagementApiCall, "deleteDeviceSpecification (HBase) " + token, LOGGER);
	try {
	    if (context.getCacheProvider() != null) {
		context.getCacheProvider().getDeviceSpecificationCache().remove(token);
	    }
	    return HBaseUtils.delete(context, context.getPayloadMarshaler(), ISiteWhereHBase.DEVICES_TABLE_NAME, token,
		    force, KEY_BUILDER, DeviceSpecification.class);
	} finally {
	    Tracer.pop(LOGGER);
	}
    }

    /**
     * Get a device specification by token or throw an exception if not found.
     * 
     * @param context
     * @param token
     * @return
     * @throws SiteWhereException
     */
    public static DeviceSpecification assertDeviceSpecification(IHBaseContext context, String token)
	    throws SiteWhereException {
	DeviceSpecification existing = getDeviceSpecificationByToken(context, token);
	if (existing == null) {
	    throw new SiteWhereSystemException(ErrorCode.InvalidDeviceSpecificationToken, ErrorLevel.ERROR);
	}
	return existing;
    }

    /**
     * Get the unique device identifier based on the long value associated with
     * the device UUID. This will be a subset of the full 8-bit long value.
     * 
     * @param value
     * @return
     */
    public static byte[] getTruncatedIdentifier(Long value) {
	byte[] bytes = Bytes.toBytes(value);
	byte[] result = new byte[SPEC_IDENTIFIER_LENGTH];
	System.arraycopy(bytes, bytes.length - SPEC_IDENTIFIER_LENGTH, result, 0, SPEC_IDENTIFIER_LENGTH);
	return result;
    }

    /**
     * Allocate the next command id and return the new value. (Each id is less
     * than the last)
     * 
     * @param context
     * @param specId
     * @return
     * @throws SiteWhereException
     */
    public static Long allocateNextCommandId(IHBaseContext context, Long specId) throws SiteWhereException {
	Tracer.push(TracerCategory.DeviceManagementApiCall, "allocateNextCommandId (HBase)", LOGGER);
	try {
	    byte[] primary = getPrimaryRowKey(specId);
	    Table devices = null;
	    try {
		devices = getDeviceTableInterface(context);
		Increment increment = new Increment(primary);
		increment.addColumn(ISiteWhereHBase.FAMILY_ID, COMMAND_COUNTER, -1);
		Result result = devices.increment(increment);
		return Bytes.toLong(result.value());
	    } catch (IOException e) {
		throw new SiteWhereException("Unable to allocate next command id.", e);
	    } finally {
		HBaseUtils.closeCleanly(devices);
	    }
	} finally {
	    Tracer.pop(LOGGER);
	}
    }

    /**
     * Get row key for a device specification with the given id.
     * 
     * @param specificationId
     * @return
     */
    public static byte[] getPrimaryRowKey(Long specificationId) {
	ByteBuffer buffer = ByteBuffer.allocate(SPEC_IDENTIFIER_LENGTH + 2);
	buffer.put(DeviceRecordType.DeviceSpecification.getType());
	buffer.put(getTruncatedIdentifier(specificationId));
	buffer.put(DeviceSpecificationRecordType.DeviceSpecification.getType());
	return buffer.array();
    }

    /**
     * Get the unique command identifier based on the long value associated with
     * the command UUID. This will be a subset of the full 8-bit long value.
     * 
     * @param value
     * @return
     */
    public static byte[] getCommandIdentifier(Long value) {
	byte[] bytes = Bytes.toBytes(value);
	byte[] result = new byte[COMMAND_IDENTIFIER_LENGTH];
	System.arraycopy(bytes, bytes.length - COMMAND_IDENTIFIER_LENGTH, result, 0, COMMAND_IDENTIFIER_LENGTH);
	return result;
    }

    /**
     * Get prefix for a device command row for the given specification.
     * 
     * @param specificationId
     * @return
     */
    public static byte[] getDeviceCommandRowPrefix(Long specificationId) {
	ByteBuffer buffer = ByteBuffer.allocate(1 + SPEC_IDENTIFIER_LENGTH + 1);
	buffer.put(DeviceRecordType.DeviceSpecification.getType());
	buffer.put(getTruncatedIdentifier(specificationId));
	buffer.put(DeviceSpecificationRecordType.DeviceCommand.getType());
	return buffer.array();
    }

    /**
     * Get prefix after the last defined specification record type.
     * 
     * @param specificationId
     * @return
     */
    public static byte[] getEndRowPrefix(Long specificationId) {
	ByteBuffer buffer = ByteBuffer.allocate(1 + SPEC_IDENTIFIER_LENGTH + 1);
	buffer.put(DeviceRecordType.DeviceSpecification.getType());
	buffer.put(getTruncatedIdentifier(specificationId));
	buffer.put(DeviceSpecificationRecordType.End.getType());
	return buffer.array();
    }

    /**
     * Get row key for a device command based on spec and unique id.
     * 
     * @param specId
     * @param value
     * @return
     */
    public static byte[] getDeviceCommandRowKey(Long specId, Long value) {
	byte[] prefix = getDeviceCommandRowPrefix(specId);
	byte[] uid = getCommandIdentifier(value);
	ByteBuffer buffer = ByteBuffer.allocate(prefix.length + uid.length);
	buffer.put(prefix);
	buffer.put(uid);
	return buffer.array();
    }

    /**
     * Get device table based on context.
     * 
     * @param context
     * @return
     * @throws SiteWhereException
     */
    protected static Table getDeviceTableInterface(IHBaseContext context) throws SiteWhereException {
	return context.getClient().getTableInterface(context.getTenant(), ISiteWhereHBase.DEVICES_TABLE_NAME);
    }
}