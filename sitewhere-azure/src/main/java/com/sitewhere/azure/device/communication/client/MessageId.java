/*******************************************************************************
 Copyright (c) Microsoft Open Technologies (Shanghai) Company Limited.  All rights reserved.

 The MIT License (MIT)

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.

 *******************************************************************************/
package com.sitewhere.azure.device.communication.client;

public class MessageId {

    private final String partitionId;
    private final String offset;
    private final long sequenceNumber;

    public MessageId(String partitionId, String offset, long sequenceNumber) {
	this.partitionId = partitionId;
	this.offset = offset;
	this.sequenceNumber = sequenceNumber;
    }

    public static MessageId create(String partitionId, String offset, long sequenceNumber) {
	return new MessageId(partitionId, offset, sequenceNumber);
    }

    public String getPartitionId() {
	return this.partitionId;
    }

    public String getOffset() {
	return this.offset;
    }

    public Long getSequenceNumber() {
	return this.sequenceNumber;
    }

    @Override
    public String toString() {
	return String.format("PartitionId: %s, Offset: %s, SequenceNumber: %s", this.partitionId, this.offset,
		this.sequenceNumber);
    }
}
