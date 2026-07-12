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
package org.openhab.binding.panasonicprojector.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.panasonicprojector.internal.configuration.PanasonicProjectorConfiguration;
import org.openhab.binding.panasonicprojector.internal.connector.PanasonicProjectorConnector;
import org.openhab.binding.panasonicprojector.internal.connector.PanasonicProjectorSerialConnector;
import org.openhab.binding.panasonicprojector.internal.connector.PanasonicProjectorTcpConnector;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.library.types.OnOffType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide high level interface to Panasonic projector.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class PanasonicProjectorDevice {
    private static final int DEFAULT_TIMEOUT_MS = 5000;

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

    private synchronized String sendQuery(String query)
            throws PanasonicProjectorCommandException, PanasonicProjectorException {
        logger.debug("Query: '{}'", query);
        final String response = connection.sendMessage(query, DEFAULT_TIMEOUT_MS);

        if (response.isEmpty()) {
            throw new PanasonicProjectorException("No response received");
        }

        logger.debug("Response: '{}'", response);

        if (ERR.equals(response)) {
            throw new PanasonicProjectorCommandException("Error response received for command: " + query);
        }

        return response;
    }

    protected int queryInt(String query) throws PanasonicProjectorCommandException, PanasonicProjectorException {
        final String response = sendQuery(query);
        try {
            return Integer.parseInt(response);
        } catch (NumberFormatException nfe) {
            throw new PanasonicProjectorCommandException(
                    "Unable to parse response '" + response + "' as Integer for command: " + query);
        }
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
    public OnOffType getPower() throws PanasonicProjectorCommandException, PanasonicProjectorException {
        final String response = sendQuery(QPW);
        if (!POWER_ON.equals(response) && !POWER_OFF.equals(response)) {
            throw new PanasonicProjectorCommandException(
                    "Invalid response received for Power status inquiry: " + response);
        }
        return OnOffType.from(POWER_ON.equals(response));
    }

    public void setPower(OnOffType value) throws PanasonicProjectorCommandException, PanasonicProjectorException {
        sendQuery(value == OnOffType.ON ? PON : POF);
    }

    /*
     * Source
     */
    public String getSource() throws PanasonicProjectorCommandException, PanasonicProjectorException {
        // if the query is reflected back, throw exception
        final String response = sendQuery(QIN);
        if (QIN.equals(response)) {
            throw new PanasonicProjectorCommandException("Invalid response received for Input status inquiry");
        }
        return response;
    }

    public void setSource(String value) throws PanasonicProjectorCommandException, PanasonicProjectorException {
        sendQuery(String.format("IIS:%s", value));
    }

    /*
     * Picture Mode
     */
    public String getPictureMode() throws PanasonicProjectorCommandException, PanasonicProjectorException {
        // if the query is reflected back, throw exception
        final String response = sendQuery(QPM);
        if (QPM.equals(response)) {
            throw new PanasonicProjectorCommandException("Invalid response received for Picture Mode status inquiry");
        }
        return response;
    }

    public void setPictureMode(String value) throws PanasonicProjectorCommandException, PanasonicProjectorException {
        sendQuery(String.format("VPM:%s", value));
    }

    /*
     * Blank Screen
     */
    public OnOffType getBlank() throws PanasonicProjectorCommandException, PanasonicProjectorException {
        return OnOffType.from(queryInt(QSH) == 1);
    }

    public void toggleBlank() throws PanasonicProjectorCommandException, PanasonicProjectorException {
        sendQuery(OSH);
    }

    /*
     * Freeze
     */
    public OnOffType getFreeze() throws PanasonicProjectorCommandException, PanasonicProjectorException {
        return OnOffType.from(queryInt(QFZ) == 1);
    }

    public void setFreeze(OnOffType value) throws PanasonicProjectorCommandException, PanasonicProjectorException {
        sendQuery(String.format("OFZ:%s", (value == OnOffType.ON ? "1" : "0")));
    }

    /*
     * Button
     */
    public void sendButton(String value) throws PanasonicProjectorCommandException, PanasonicProjectorException {
        sendQuery(value);
    }
}
