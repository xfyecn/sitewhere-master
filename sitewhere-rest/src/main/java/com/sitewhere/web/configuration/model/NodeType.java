/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.web.configuration.model;

/**
 * Type indicator for nodes in the configuration hierarchy.
 * 
 * @author Derek
 */
public enum NodeType {

    /** Container node (no XML representation) */
    Container,

    /** Corresponds to an XML element */
    Element,

    /** Corresponds to an XML attribute */
    Attribute
}