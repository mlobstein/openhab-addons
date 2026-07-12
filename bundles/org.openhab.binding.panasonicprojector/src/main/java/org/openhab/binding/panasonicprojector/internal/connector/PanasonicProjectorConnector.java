/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.panasonicprojector.internal.connector;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.panasonicprojector.internal.PanasonicProjectorException;

/**
 * Base class for Panasonic projector communication.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public interface PanasonicProjectorConnector {

    public static final String STX = "\u0002";
    public static final String ETX = "\u0003";

    /**
     * Procedure for connecting to projector.
     *
     * @throws PanasonicProjectorException
     */
    void connect() throws PanasonicProjectorException;

    /**
     * Procedure for disconnecting to projector controller.
     *
     * @throws PanasonicProjectorException
     */
    void disconnect() throws PanasonicProjectorException;

    /**
     * Procedure for send raw data to projector.
     *
     * @param data
     *            Message to send.
     *
     * @param timeout
     *            timeout to wait response in milliseconds.
     *
     * @throws PanasonicProjectorException
     */
    String sendMessage(String data, int timeout) throws PanasonicProjectorException;
}
