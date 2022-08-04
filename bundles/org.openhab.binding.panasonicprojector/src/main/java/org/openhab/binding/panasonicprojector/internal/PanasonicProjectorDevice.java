/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.panasonicprojector.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.panasonicprojector.internal.configuration.PanasonicProjectorConfiguration;
import org.openhab.binding.panasonicprojector.internal.connector.PanasonicProjectorConnector;
import org.openhab.binding.panasonicprojector.internal.connector.PanasonicProjectorSerialConnector;
import org.openhab.binding.panasonicprojector.internal.connector.PanasonicProjectorTcpConnector;
import org.openhab.binding.panasonicprojector.internal.enums.Switch;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide high level interface to Panasonic projector.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class PanasonicProjectorDevice {
    private static final int DEFAULT_TIMEOUT_MS = 5 * 1000;

    private static final String ERR = "ER401";
    private static final String POWER_ON = "001";
    private static final String POWER_OFF = "000";

    private static final String PON = "PON";
    private static final String POF = "POF";
    private static final String QPW = "QPW";
    private static final String QIN = "QIN";
    private static final String QPM = "QPM";
    private static final String OSH = "OSH";
    private static final String QSH = "QSH";
    private static final String QFZ = "QFZ";

    private final Logger logger = LoggerFactory.getLogger(PanasonicProjectorDevice.class);

    private PanasonicProjectorConnector connection;
    private boolean connected = false;

    public PanasonicProjectorDevice(SerialPortManager serialPortManager, PanasonicProjectorConfiguration config) {
        connection = new PanasonicProjectorSerialConnector(serialPortManager, config.serialPort);
    }

    public PanasonicProjectorDevice(PanasonicProjectorConfiguration config) {
        connection = new PanasonicProjectorTcpConnector(config.host, config.port);
    }

    private synchronized String sendQuery(String query, int timeout)
            throws PanasonicProjectorCommandException, PanasonicProjectorException {
        logger.debug("Query: '{}'", query);
        String response = connection.sendMessage(query, timeout);

        if (response.isEmpty()) {
            throw new PanasonicProjectorException("No response received");
        }

        logger.debug("Response: '{}'", response);

        if (ERR.equals(response)) {
            throw new PanasonicProjectorCommandException("Error response received for command: " + query);
        }

        return response;
    }

    protected void sendCommand(String command, int timeout)
            throws PanasonicProjectorCommandException, PanasonicProjectorException {
        sendQuery(command, timeout);
    }

    protected void sendCommand(String command) throws PanasonicProjectorCommandException, PanasonicProjectorException {
        sendCommand(command, DEFAULT_TIMEOUT_MS);
    }

    protected int queryInt(String query) throws PanasonicProjectorCommandException, PanasonicProjectorException {
        String response = sendQuery(query, DEFAULT_TIMEOUT_MS);
        return Integer.parseInt(response);
    }

    protected String queryString(String query) throws PanasonicProjectorCommandException, PanasonicProjectorException {
        return sendQuery(query, DEFAULT_TIMEOUT_MS);
    }

    public void connect() throws PanasonicProjectorException {
        connection.connect();
        connected = true;
    }

    public void disconnect() throws PanasonicProjectorException {
        connection.disconnect();
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }

    /*
     * Power
     */
    public Switch getPowerStatus() throws PanasonicProjectorCommandException, PanasonicProjectorException {
        String response = queryString(QPW);
        if (POWER_ON.equals(response)) {
            return Switch.ON;
        } else if (POWER_OFF.equals(response)) {
            return Switch.OFF;
        } else {
            throw new PanasonicProjectorCommandException(
                    "Invalid response received for Power status inquiry: " + response);
        }
    }

    public void setPower(Switch value) throws PanasonicProjectorCommandException, PanasonicProjectorException {
        sendCommand(value == Switch.ON ? PON : POF);
    }

    /*
     * Source
     */
    public @Nullable String getSource() throws PanasonicProjectorCommandException, PanasonicProjectorException {
        String response = queryString(QIN);
        // if the query is reflected back, return null
        return !QIN.equals(response) ? response : null;
    }

    public void setSource(String value) throws PanasonicProjectorCommandException, PanasonicProjectorException {
        sendCommand(String.format("IIS:%s", value));
    }

    /*
     * Picture Mode
     */
    public @Nullable String getPictureMode() throws PanasonicProjectorCommandException, PanasonicProjectorException {
        String response = queryString(QPM);
        // if the query is reflected back, return null
        return !QPM.equals(response) ? response : null;
    }

    public void setPictureMode(String value) throws PanasonicProjectorCommandException, PanasonicProjectorException {
        sendCommand(String.format("VPM:%s", value));
    }

    /*
     * Blank Screen
     */
    public Switch getBlank() throws PanasonicProjectorCommandException, PanasonicProjectorException {
        int val = queryInt(QSH);
        return val == 1 ? Switch.ON : Switch.OFF;
    }

    public void setBlank() throws PanasonicProjectorCommandException, PanasonicProjectorException {
        sendCommand(OSH);
    }

    /*
     * Freeze
     */
    public Switch getFreeze() throws PanasonicProjectorCommandException, PanasonicProjectorException {
        int val = queryInt(QFZ);
        return val == 1 ? Switch.ON : Switch.OFF;
    }

    public void setFreeze(Switch value) throws PanasonicProjectorCommandException, PanasonicProjectorException {
        sendCommand(String.format("OFZ:%s", (value == Switch.ON ? "1" : "0")), DEFAULT_TIMEOUT_MS);
    }

    /*
     * Button
     */
    public void sendButton(String value) throws PanasonicProjectorCommandException, PanasonicProjectorException {
        sendCommand(value);
    }
}
