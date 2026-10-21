/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.binding.nuvotuner.internal.communication;

import static org.openhab.binding.nuvotuner.internal.NuvoTunerBindingConstants.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nuvotuner.internal.NuvoTunerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for communicating with the Nuvo Tuner
 *
 * @author Laurent Garnier - Initial contribution
 * @author Michael Lobstein - Adapted for the Nuvo binding
 */
@NonNullByDefault
public abstract class NuvoTunerConnector {
    private static final String COMMAND_OK = "#OK";
    private static final String BEGIN_CMD = "*";
    private static final String END_CMD = "\r";
    private static final String QUERY = "?";

    private static final Pattern TUNER_VESION_PATTERN = Pattern.compile("#VER ?\"?NV-(T2[A-Z]{3})");
    private static final Pattern TUNER_ON_UPDATE_PATTERN = Pattern.compile("^#T'(\\w)'ON,(.*)$");
    private static final Pattern TUNER_SIRIUS_CH_LIST_PATTERN = Pattern.compile("^#T'(\\w)'_LISTSR(.*)$");
    private static final Pattern TUNER_DAB_LIST_PATTERN = Pattern.compile("^#T'(\\w)'DABLIST(.*)$");

    private final Logger logger = LoggerFactory.getLogger(NuvoTunerConnector.class);

    protected static final String COMMAND_ERROR = "#?";

    /** The output stream */
    protected @Nullable OutputStream dataOut;

    /** The input stream */
    protected @Nullable InputStream dataIn;

    /** true if the connection is established, false if not */
    private boolean connected;

    private @Nullable Thread readerThread;

    private List<NuvoTunerMessageEventListener> listeners = new ArrayList<>();

    private String tunerType = "";

    /**
     * Get whether the connection is established or not
     *
     * @return true if the connection is established
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Set whether the connection is established or not
     *
     * @param connected true if the connection is established
     */
    protected void setConnected(boolean connected) {
        this.connected = connected;
    }

    /**
     * Set the thread that handles the feedback messages
     *
     * @param readerThread the thread
     */
    protected void setReaderThread(Thread readerThread) {
        this.readerThread = readerThread;
    }

    /**
     * Open the connection with the Nuvo device
     *
     * @throws NuvoTunerException - In case of any problem
     */
    public abstract void open() throws NuvoTunerException;

    /**
     * Close the connection with the Nuvo device
     */
    public abstract void close();

    /**
     * Stop the thread that handles the feedback messages and close the opened input and output streams
     */
    protected void cleanup() {
        Thread readerThread = this.readerThread;
        OutputStream dataOut = this.dataOut;
        if (dataOut != null) {
            try {
                dataOut.close();
            } catch (IOException e) {
                logger.debug("Error closing dataOut: {}", e.getMessage());
            }
            this.dataOut = null;
        }
        InputStream dataIn = this.dataIn;
        if (dataIn != null) {
            try {
                dataIn.close();
            } catch (IOException e) {
                logger.debug("Error closing dataIn: {}", e.getMessage());
            }
            this.dataIn = null;
        }
        if (readerThread != null) {
            readerThread.interrupt();
            this.readerThread = null;
            try {
                readerThread.join(3000);
            } catch (InterruptedException e) {
                logger.warn("Error joining readerThread: {}", e.getMessage());
            }
        }
    }

    /**
     * Reads some number of bytes from the input stream and stores them into the buffer array b. The number of bytes
     * actually read is returned as an integer.
     *
     * @param dataBuffer the buffer into which the data is read.
     *
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the
     *         stream has been reached.
     *
     * @throws NuvoTunerException - If the input stream is null, if the first byte cannot be read for any reason
     *             other than the end of the file, if the input stream has been closed, or if some other I/O error
     *             occurs.
     */
    protected int readInput(byte[] dataBuffer) throws NuvoTunerException {
        InputStream dataIn = this.dataIn;
        if (dataIn == null) {
            throw new NuvoTunerException("readInput failed: input stream is null");
        }
        try {
            return dataIn.read(dataBuffer);
        } catch (IOException e) {
            throw new NuvoTunerException("readInput failed: " + e.getMessage(), e);
        }
    }

    /**
     * Request the Nuvo tuner to execute an inquiry command
     *
     * @param tuner the tuner for which the command is to be run
     * @param cmd the command to execute
     *
     * @throws NuvoTunerException - In case of any problem
     */
    public void sendQuery(NuvoTunerEnum tuner, NuvoTunerCommand cmd) throws NuvoTunerException {
        sendCommand(tuner.getId() + cmd.getValue() + QUERY);
    }

    /**
     * Request the Nuvo tuner to execute a command for a tuner that takes no arguments (ie power on, power off,
     * etc.)
     *
     * @param tuner the tuner for which the command is to be run
     * @param cmd the command to execute
     *
     * @throws NuvoTunerException - In case of any problem
     */
    public void sendCommand(NuvoTunerEnum tuner, NuvoTunerCommand cmd) throws NuvoTunerException {
        sendCommand(tuner.getId() + cmd.getValue());
    }

    /**
     * Request the Nuvo tuner to execute a command for a tuner and pass in a value
     *
     * @param tune the particular tuner for which the command is to be run (A or B)
     * @param cmd the command to execute
     * @param value the command value string
     *
     * @throws NuvoTunerException - In case of any problem
     */
    public void sendCommand(NuvoTunerEnum tuner, NuvoTunerCommand cmd, @Nullable String value)
            throws NuvoTunerException {
        sendCommand(tuner.getId() + cmd.getValue() + value);
    }

    /**
     * Request the Nuvo tuner to execute a system command the does not specify a tuner or value
     *
     * @param cmd the command to execute
     *
     * @throws NuvoTunerException - In case of any problem
     */
    public void sendCommand(NuvoTunerCommand cmd) throws NuvoTunerException {
        sendCommand(cmd.getValue());
    }

    /**
     * Request the Nuvo tuner to execute a raw command string
     *
     * @param command the command string to run
     *
     * @throws NuvoTunerException - In case of any problem
     */
    public void sendCommand(@Nullable String command) throws NuvoTunerException {
        final String messageStr = BEGIN_CMD + command + END_CMD;

        logger.debug("sending command: {}", messageStr);

        OutputStream dataOut = this.dataOut;
        if (dataOut == null) {
            throw new NuvoTunerException("Send command \"" + messageStr + "\" failed: output stream is null");
        }
        try {
            dataOut.write(messageStr.getBytes(StandardCharsets.US_ASCII));
            dataOut.flush();
        } catch (IOException e) {
            throw new NuvoTunerException("Send command \"" + command + "\" failed: " + e.getMessage(), e);
        }
    }

    /**
     * Add a listener to the list of listeners to be notified with events
     *
     * @param listener the listener
     */
    public void addEventListener(NuvoTunerMessageEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener from the list of listeners to be notified with events
     *
     * @param listener the listener
     */
    public void removeEventListener(NuvoTunerMessageEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Analyze an incoming message and dispatch corresponding (type, key, value) to the event listeners
     *
     * @param incomingMessage the received message
     */
    public void handleIncomingMessage(byte[] incomingMessage) {
        final String message = new String(incomingMessage, StandardCharsets.US_ASCII).trim();

        if (COMMAND_ERROR.equals(message) || COMMAND_OK.equals(message)) {
            // ignore
            return;
        }

        // version ping response ie: #VER"NV-T2SIR FWv1.18 HWv40"
        if (message.startsWith("#VER\"")) {
            logger.debug("version response: {}", message);
            if ("".equals(tunerType)) {
                final Matcher versionMatcher = TUNER_VESION_PATTERN.matcher(message);
                if (versionMatcher.find()) {
                    tunerType = versionMatcher.group(1);
                    dispatchKeyValue(TYPE_TUNER_UPDATE, "", versionMatcher.group(1));
                }
            }
            return;
        }

        // Tuner update ie: #T'B'ON,SR13
        Matcher matcher = TUNER_ON_UPDATE_PATTERN.matcher(message);
        if (matcher.find()) {
            // pull out the tuner id and the remainder of the message after ON,
            dispatchKeyValue(TYPE_TUNER_UPDATE, matcher.group(1), matcher.group(2));
            return;
        }

        // Sirius channel list item ie: #T'B'_LISTSR2,2,"Hits 1"
        matcher = TUNER_SIRIUS_CH_LIST_PATTERN.matcher(message);
        if (matcher.find()) {
            // pull out the tuner id and the remainder of the message after _LISTSR
            dispatchKeyValue(TYPE_SIRIUS_CHANNEL_LIST, matcher.group(1), matcher.group(2));
            return;
        }

        // DAB channel list item ie: #T'B'DABLISTy1,z1,"s1"
        matcher = TUNER_DAB_LIST_PATTERN.matcher(message);
        if (matcher.find()) {
            // pull out the tuner id and the remainder of the message after DABLIST
            dispatchKeyValue(TYPE_DAB_CHANNEL_LIST, matcher.group(1), matcher.group(2));
            return;
        }

        // logger.debug("unhandled message: {}", message);
    }

    /**
     * Dispatch an event (type, key, value) to the event listeners
     *
     * @param type the type
     * @param key the key
     * @param value the value
     */
    private void dispatchKeyValue(String type, String key, String value) {
        NuvoTunerMessageEvent event = new NuvoTunerMessageEvent(this, type, key, value);
        listeners.forEach(l -> l.onNewMessageEvent(event));
    }
}
