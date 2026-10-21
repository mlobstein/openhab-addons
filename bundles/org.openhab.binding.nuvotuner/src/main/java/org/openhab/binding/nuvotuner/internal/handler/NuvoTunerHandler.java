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
package org.openhab.binding.nuvotuner.internal.handler;

import static org.openhab.binding.nuvotuner.internal.NuvoTunerBindingConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nuvotuner.internal.NuvoTunerException;
import org.openhab.binding.nuvotuner.internal.NuvoTunerStateDescriptionOptionProvider;
import org.openhab.binding.nuvotuner.internal.communication.NuvoTunerCommand;
import org.openhab.binding.nuvotuner.internal.communication.NuvoTunerConnector;
import org.openhab.binding.nuvotuner.internal.communication.NuvoTunerDefaultConnector;
import org.openhab.binding.nuvotuner.internal.communication.NuvoTunerEnum;
import org.openhab.binding.nuvotuner.internal.communication.NuvoTunerIpConnector;
import org.openhab.binding.nuvotuner.internal.communication.NuvoTunerMessageEvent;
import org.openhab.binding.nuvotuner.internal.communication.NuvoTunerMessageEventListener;
import org.openhab.binding.nuvotuner.internal.communication.NuvoTunerSerialConnector;
import org.openhab.binding.nuvotuner.internal.configuration.NuvoTunerThingConfiguration;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.StateOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NuvoTunerHandler} is responsible for handling commands, which are sent to one of the channels.
 *
 * Based on the Rotel binding by Laurent Garnier
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class NuvoTunerHandler extends BaseThingHandler implements NuvoTunerMessageEventListener {
    private static final long RECON_POLLING_INTERVAL_SEC = 60;
    private static final long POLLING_INTERVAL_SEC = 30;
    private static final long INITIAL_POLLING_DELAY_SEC = 30;
    private static final long BINDING_START_REFRESH_CHANNELS_DELAY_SEC = 10;
    private static final long REFRESH_CHANNELS_DELAY_SEC = 180;

    private static final String CHANNEL_DELIMIT = "#";

    private static final Pattern AM_FREQ_PATTERN = Pattern.compile("^AM(\\d{3,4})$");
    private static final Pattern FM_FREQ_PATTERN = Pattern.compile("^FM(\\d{2,3}.\\d{1})$");
    private static final Pattern SIRIUS_CH_PATTERN = Pattern.compile("^SR(\\d{1,3})$");
    private static final Pattern DAB_CH_PATTERN = Pattern.compile("^DAB(.*)$");

    private final Logger logger = LoggerFactory.getLogger(NuvoTunerHandler.class);
    private final NuvoTunerStateDescriptionOptionProvider stateDescriptionProvider;
    private final SerialPortManager serialPortManager;

    private @Nullable ScheduledFuture<?> reconnectJob;
    private @Nullable ScheduledFuture<?> pollingJob;
    private @Nullable ScheduledFuture<?> refreshChannelsJob;

    private NuvoTunerConnector connector = new NuvoTunerDefaultConnector();
    private Object sequenceLock = new Object();

    List<StateOption> amFrequencyStateOptions = new ArrayList<>();
    List<StateOption> fmFrequencyStateOptions = new ArrayList<>();

    List<StateOption> siriusChannelStateOptionsA = new ArrayList<>();
    List<StateOption> siriusChannelStateOptionsB = new ArrayList<>();

    List<StateOption> dabChannelStateOptionsA = new ArrayList<>();
    List<StateOption> dabChannelStateOptionsB = new ArrayList<>();

    String tunerType = "";

    /**
     * Constructor
     */
    public NuvoTunerHandler(Thing thing, NuvoTunerStateDescriptionOptionProvider stateDescriptionProvider,
            SerialPortManager serialPortManager) {
        super(thing);
        this.stateDescriptionProvider = stateDescriptionProvider;
        this.serialPortManager = serialPortManager;
    }

    @Override
    public void initialize() {
        final String uid = this.getThing().getUID().getAsString();
        final NuvoTunerThingConfiguration config = getConfigAs(NuvoTunerThingConfiguration.class);
        final String serialPort = config.serialPort;
        final String host = config.host;
        final Integer port = config.port;
        final @Nullable String amFreqMap = config.amFreqMap;
        final @Nullable String fmFreqMap = config.fmFreqMap;

        // Check configuration settings
        String configError = null;
        if ((serialPort == null || serialPort.isEmpty()) && (host == null || host.isEmpty())) {
            configError = "Undefined Serial port and host configuration settings; please set one of them";
        } else if (serialPort != null && (host == null || host.isEmpty())) {
            if (serialPort.toLowerCase(Locale.ENGLISH).startsWith("rfc2217")) {
                configError = "Use Host and Port configuration settings for a serial over IP connection";
            }
        } else {
            if (port == null) {
                configError = "Undefined port configuration setting";
            } else if (port <= 0) {
                configError = "Invalid port configuration setting";
            }
        }

        if (configError != null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, configError);
            return;
        }

        if (serialPort != null) {
            connector = new NuvoTunerSerialConnector(serialPortManager, serialPort, uid);
        } else if (host != null && port != null) {
            connector = new NuvoTunerIpConnector(host, port, uid);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Either Serial port or Host & Port must be specifed");
            return;
        }

        if (amFreqMap != null) {
            final String amTunerA = NuvoTunerEnum.TUNER_A.name().toLowerCase(Locale.ENGLISH) + CHANNEL_DELIMIT
                    + CHANNEL_TYPE_AM_FREQUENCY;
            final String amTunerB = NuvoTunerEnum.TUNER_B.name().toLowerCase(Locale.ENGLISH) + CHANNEL_DELIMIT
                    + CHANNEL_TYPE_AM_FREQUENCY;

            if (isLinked(amTunerA)) {
                logger.debug("setting tuner_a#am_frequency state options");
                setRadioStateOptions(amTunerA, amFreqMap);
            }

            if (isLinked(amTunerB)) {
                logger.debug("setting tuner_b#am_frequency state options");
                setRadioStateOptions(amTunerB, amFreqMap);
            }
        } else {
            logger.debug("amFreqMap was null");
        }

        if (fmFreqMap != null) {
            final String fmTunerA = NuvoTunerEnum.TUNER_A.name().toLowerCase(Locale.ENGLISH) + CHANNEL_DELIMIT
                    + CHANNEL_TYPE_FM_FREQUENCY;
            final String fmTunerB = NuvoTunerEnum.TUNER_B.name().toLowerCase(Locale.ENGLISH) + CHANNEL_DELIMIT
                    + CHANNEL_TYPE_FM_FREQUENCY;

            if (isLinked(fmTunerA)) {
                logger.debug("setting tuner_a#fm_frequency state options");
                setRadioStateOptions(fmTunerA, fmFreqMap);
            }

            if (isLinked(fmTunerB)) {
                logger.debug("setting tuner_b#fm_frequency state options");
                setRadioStateOptions(fmTunerB, fmFreqMap);
            }
        } else {
            logger.debug("fmFreqMap was null");
        }

        long delaySeconds = BINDING_START_REFRESH_CHANNELS_DELAY_SEC;
        if (isLinked(NuvoTunerEnum.TUNER_A.name().toLowerCase(Locale.ENGLISH) + CHANNEL_DELIMIT
                + CHANNEL_TYPE_SIRIUS_CHANNEL)
                || isLinked(NuvoTunerEnum.TUNER_A.name().toLowerCase(Locale.ENGLISH) + CHANNEL_DELIMIT
                        + CHANNEL_TYPE_DAB_CHANNEL)) {
            scheduleRefreshChannelsJob(NuvoTunerEnum.TUNER_A, delaySeconds);
            delaySeconds = 60L;
        }

        if (isLinked(NuvoTunerEnum.TUNER_B.name().toLowerCase(Locale.ENGLISH) + CHANNEL_DELIMIT
                + CHANNEL_TYPE_SIRIUS_CHANNEL)
                || isLinked(NuvoTunerEnum.TUNER_B.name().toLowerCase(Locale.ENGLISH) + CHANNEL_DELIMIT
                        + CHANNEL_TYPE_DAB_CHANNEL)) {
            scheduleRefreshChannelsJob(NuvoTunerEnum.TUNER_B, delaySeconds);
        }

        scheduleReconnectJob();
        schedulePollingJob();
        updateStatus(ThingStatus.UNKNOWN);
    }

    private void setRadioStateOptions(String channel, String freqMap) {
        final List<StateOption> stateOptions = new ArrayList<>();

        for (final String line : freqMap.split("\\r?\\n")) {
            if (line.contains("=")) {
                final String entry[] = line.split("=");
                stateOptions.add(new StateOption(entry[0], entry[0] + " - " + entry[1]));
            }
        }

        stateDescriptionProvider.setStateOptions(new ChannelUID(getThing().getUID(), channel), stateOptions);
    }

    @Override
    public void dispose() {
        cancelRefreshChannelsJob();
        cancelReconnectJob();
        cancelPollingJob();
        closeConnection();
    }

    /**
     * Open the connection with the Nuvo tuner
     *
     * @return true if the connection is opened successfully or false if not
     */
    private synchronized boolean openConnection() {
        connector.addEventListener(this);
        try {
            connector.open();
        } catch (NuvoTunerException e) {
            logger.debug("openConnection() failed: {}", e.getMessage());
        }
        logger.debug("openConnection(): {}", connector.isConnected() ? "connected" : "disconnected");
        return connector.isConnected();
    }

    /**
     * Close the connection with the Nuvo device
     */
    private synchronized void closeConnection() {
        connector.removeEventListener(this);

        if (connector.isConnected()) {
            connector.close();
            logger.debug("closeConnection(): disconnected");
        }
    }

    /**
     * Handle a command the UI
     *
     * @param channelUID the channel sending the command
     * @param command the command received
     *
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        final String channel = channelUID.getId();
        final String[] channelSplit = channelUID.getId().split(CHANNEL_DELIMIT);

        final NuvoTunerEnum tuner = NuvoTunerEnum.valueOf(channelSplit[0].toUpperCase(Locale.ENGLISH));
        final String channelType = channelSplit[1];

        if (getThing().getStatus() != ThingStatus.ONLINE) {
            logger.debug("Thing is not ONLINE; command {} from channel {} is ignored", command, channel);
            return;
        }

        synchronized (sequenceLock) {
            if (!connector.isConnected()) {
                logger.warn("Command {} from channel {} is ignored: connection not established", command, channel);
                return;
            }

            try {
                switch (channelType) {
                    case CHANNEL_TYPE_AM_FREQUENCY:
                        if (command instanceof DecimalType) {
                            final int value = ((DecimalType) command).intValue();
                            logger.debug("Got AM frequency channel command {} for {}", value, tuner);
                            connector.sendCommand(tuner, NuvoTunerCommand.SET_AM_FREQ, String.valueOf(value));
                        }
                        break;
                    case CHANNEL_TYPE_FM_FREQUENCY:
                        if (command instanceof DecimalType) {
                            final double value = ((DecimalType) command).doubleValue();
                            logger.debug("Got FM frequency channel command {} for {}", value, tuner);
                            connector.sendCommand(tuner, NuvoTunerCommand.SET_FM_FREQ, String.format("%.1f", value));
                        }
                        break;
                    case CHANNEL_TYPE_SIRIUS_CHANNEL:
                        if (command instanceof DecimalType) {
                            final int value = ((DecimalType) command).intValue();
                            logger.debug("Got Sirius channel command {} for {}", value, tuner);
                            connector.sendCommand(tuner, NuvoTunerCommand.SET_SIRIUS_CHANNEL, String.valueOf(value));
                        }
                        break;
                    case CHANNEL_TYPE_DAB_CHANNEL:
                        if (command instanceof StringType) {
                            logger.debug("Got DAB channel command {} for {}", command, tuner);
                            connector.sendCommand(tuner, NuvoTunerCommand.SET_DAB_CHANNEL, command.toString());
                        }
                        break;
                    case CHANNEL_TYPE_REFRESH_CHANNELS_SW:
                        if ("T2SIR".equals(tunerType) || "T2DAB".equals(tunerType)) {
                            if ("T2DAB".equals(tunerType)) {
                                if (tuner == NuvoTunerEnum.TUNER_A) {
                                    dabChannelStateOptionsA.clear();
                                }
                                if (tuner == NuvoTunerEnum.TUNER_B) {
                                    dabChannelStateOptionsB.clear();
                                }
                            }
                            scheduleRefreshChannelsJob(tuner, REFRESH_CHANNELS_DELAY_SEC);
                        } else {
                            logger.debug("Refreshing channels N/A on T2FAM tuner");
                        }
                        break;
                }
            } catch (NuvoTunerException e) {
                logger.warn("Command {} from channel {} failed: {}", command, channel, e.getMessage());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Sending command failed");
                closeConnection();
                scheduleReconnectJob();
            }
        }
    }

    /**
     * Handle an event received from the Nuvo device
     *
     * @param event the event to process
     */
    @Override
    public void onNewMessageEvent(NuvoTunerMessageEvent evt) {
        final String type = evt.getType();
        final String key = evt.getKey();
        final String updateData = evt.getValue().trim();
        if (this.getThing().getStatus() == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.ONLINE);
        }

        switch (type) {
            case TYPE_TUNER_UPDATE:
                if ("".equals(key) && updateData.startsWith("T2")) {
                    logger.debug("Setting tunerType: {}", updateData);
                    tunerType = updateData;
                    return;
                }

                logger.debug("Tuner update: tuner: {} - Value: {}", key, updateData);
                // example : FM103.5 or
                // AM700 or
                // SR14 or
                // DAB12C or
                // AM DISABLED or
                // SR UNAVAILABLE or
                // DAB???

                if (updateData.contains("DISABLED") || updateData.contains("UNAVAILABLE")) {
                    return;
                }

                final NuvoTunerEnum targetTuner = NuvoTunerEnum.valueOf("TUNER_" + key);
                final String bandType = BAND_DAB.equals(updateData.substring(0, 3)) ? BAND_DAB
                        : updateData.substring(0, 2);

                switch (bandType) {
                    case BAND_AM:
                        final Matcher amMatcher = AM_FREQ_PATTERN.matcher(updateData);
                        if (amMatcher.find()) {
                            updateChannelState(targetTuner, CHANNEL_TYPE_AM_FREQUENCY, amMatcher.group(1));
                            updateChannelState(targetTuner, CHANNEL_TYPE_ACTIVE_BAND, BAND_AM);
                        } else {
                            logger.debug("no AM frequency match on message: {}", updateData);
                        }
                        break;
                    case BAND_FM:
                        final Matcher fmMatcher = FM_FREQ_PATTERN.matcher(updateData);
                        if (fmMatcher.find()) {
                            updateChannelState(targetTuner, CHANNEL_TYPE_FM_FREQUENCY, fmMatcher.group(1));
                            updateChannelState(targetTuner, CHANNEL_TYPE_ACTIVE_BAND, BAND_FM);
                        } else {
                            logger.debug("no FM frequency match on message: {}", updateData);
                        }
                        break;
                    case BAND_SR:
                        final Matcher srMatcher = SIRIUS_CH_PATTERN.matcher(updateData);
                        if (srMatcher.find()) {
                            updateChannelState(targetTuner, CHANNEL_TYPE_SIRIUS_CHANNEL, srMatcher.group(1));
                            updateChannelState(targetTuner, CHANNEL_TYPE_ACTIVE_BAND, BAND_SR);
                        } else {
                            logger.debug("no Sirius channel match on message: {}", updateData);
                        }
                        break;
                    case BAND_DAB:
                        final Matcher dabMatcher = DAB_CH_PATTERN.matcher(updateData);
                        if (dabMatcher.find()) {
                            logger.debug("DAB channel message: {}", dabMatcher.group(1));
                            // updateChannelState(targetTuner, CHANNEL_TYPE_DAB_CHANNEL, dabMatcher.group(1));
                            updateChannelState(targetTuner, CHANNEL_TYPE_ACTIVE_BAND, BAND_DAB);
                        } else {
                            logger.debug("no DAB channel match on message: {}", updateData);
                        }
                        break;
                }
                break;
            case TYPE_SIRIUS_CHANNEL_LIST:
                logger.debug("Sirius channel list update: tuner: {} - Value: {}", key, updateData);
                if ("A".equals(key)) {
                    handleSiriusChannelListUpdate(NuvoTunerEnum.TUNER_A, updateData, siriusChannelStateOptionsA);
                }

                if ("B".equals(key)) {
                    handleSiriusChannelListUpdate(NuvoTunerEnum.TUNER_B, updateData, siriusChannelStateOptionsB);
                }
                break;
            case TYPE_DAB_CHANNEL_LIST:
                logger.debug("DAB channel list update: tuner: {} - Value: {}", key, updateData);
                if ("A".equals(key)) {
                    handleDabChannelListUpdate(NuvoTunerEnum.TUNER_A, updateData, dabChannelStateOptionsA);
                }

                if ("B".equals(key)) {
                    handleDabChannelListUpdate(NuvoTunerEnum.TUNER_B, updateData, dabChannelStateOptionsB);
                }
                break;
        }
    }

    /**
     * Update the state of a channel
     *
     * @param target the channel group
     * @param channelType the channel group item
     * @param value the value to be updated
     */
    private void updateChannelState(NuvoTunerEnum target, String channelType, String value) {
        final String channel = target.name().toLowerCase(Locale.ENGLISH) + CHANNEL_DELIMIT + channelType;

        if (!isLinked(channel)) {
            return;
        }

        switch (channelType) {
            case CHANNEL_TYPE_AM_FREQUENCY:
            case CHANNEL_TYPE_FM_FREQUENCY:
            case CHANNEL_TYPE_SIRIUS_CHANNEL:
                updateState(channel, new DecimalType(value));
                break;
            case CHANNEL_TYPE_DAB_CHANNEL:
            case CHANNEL_TYPE_ACTIVE_BAND:
                updateState(channel, new StringType(value));
            default:
                break;
        }
    }

    private void handleSiriusChannelListUpdate(NuvoTunerEnum tuner, String updateData, List<StateOption> stateOptions) {
        if ("0,0,\"Sirius ID\"".equals(updateData)) {
            stateOptions.clear();
        } else if ("0,0,\"\"".equals(updateData)) {
            stateDescriptionProvider.setStateOptions(
                    new ChannelUID(getThing().getUID(),
                            tuner.name().toLowerCase(Locale.ENGLISH) + CHANNEL_DELIMIT + CHANNEL_TYPE_SIRIUS_CHANNEL),
                    stateOptions);
            logger.info("Sirius {} channels list updated with {} entries", tuner, stateOptions.size());
        } else {
            // 2,2,"Hits 1"
            final String[] channelInfo = updateData.split(",");
            stateOptions
                    .add(new StateOption(channelInfo[0], channelInfo[0] + " - " + channelInfo[2].replace("\"", "")));
        }
    }

    private void handleDabChannelListUpdate(NuvoTunerEnum tuner, String updateData, List<StateOption> stateOptions) {
        if ("0,0,\"\"".equals(updateData)) {
            stateDescriptionProvider.setStateOptions(
                    new ChannelUID(getThing().getUID(),
                            tuner.name().toLowerCase(Locale.ENGLISH) + CHANNEL_DELIMIT + CHANNEL_TYPE_SIRIUS_CHANNEL),
                    stateOptions);
            logger.info("DAB {} channels list updated with {} entries", tuner, stateOptions.size());
        } else {
            // y1,z1,"s1"
            // y1 = DAB Ensemble ID: 1 to 65535
            // z1 = DAB Is Secondary: 0 or 1 (primary or secondary designation)
            // "s1" = String containing DAB component name (max 80 character length)
            stateOptions.add(new StateOption(updateData, updateData.replace("\"", "").replace(",", "")));
        }
    }

    /**
     * Schedule the reconnection job
     */
    private void scheduleReconnectJob() {
        logger.debug("Schedule reconnect job");
        cancelReconnectJob();
        reconnectJob = scheduler.scheduleWithFixedDelay(() -> {
            if (!connector.isConnected()) {
                logger.debug("Trying to reconnect...");
                closeConnection();
                String error = null;
                if (openConnection()) {
                    logger.debug("Reconnected");
                } else {
                    error = "Reconnection failed";
                }
                if (error != null) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, error);
                    closeConnection();
                } else {
                    updateStatus(ThingStatus.ONLINE);
                    try {
                        connector.sendCommand(NuvoTunerCommand.GET_CONTROLLER_VERSION);
                        Thread.sleep(100);
                        connector.sendCommand(NuvoTunerEnum.TUNER_A, NuvoTunerCommand.GET_STATUS);
                        Thread.sleep(100);
                        connector.sendCommand(NuvoTunerEnum.TUNER_B, NuvoTunerCommand.GET_STATUS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (NuvoTunerException e) {
                        logger.warn("Initial commands failed: {}", e.getMessage());
                    }

                }
            }
        }, 1, RECON_POLLING_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    /**
     * Cancel the reconnection job
     */
    private void cancelReconnectJob() {
        ScheduledFuture<?> reconnectJob = this.reconnectJob;
        if (reconnectJob != null) {
            reconnectJob.cancel(true);
            this.reconnectJob = null;
        }
    }

    /**
     * Schedule the polling job
     */
    private void schedulePollingJob() {
        cancelPollingJob();
        logger.debug("Schedule polling job");

        // will try to keep the connection (esp Serial over IP) alive and detect if the connection goes down
        pollingJob = scheduler.scheduleWithFixedDelay(() -> {
            if (connector.isConnected()) {
                logger.debug("Polling the tuner...");

                synchronized (sequenceLock) {
                    try {
                        connector.sendCommand(NuvoTunerCommand.GET_CONTROLLER_VERSION);
                    } catch (NuvoTunerException e) {
                        logger.debug("Polling error: {}", e.getMessage());
                        closeConnection();
                        scheduleReconnectJob();
                    }
                }
            }
        }, INITIAL_POLLING_DELAY_SEC, POLLING_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    /**
     * Cancel the polling job
     */
    private void cancelPollingJob() {
        ScheduledFuture<?> pollingJob = this.pollingJob;
        if (pollingJob != null) {
            pollingJob.cancel(true);
            this.pollingJob = null;
        }
    }

    /**
     * Schedule the refresh channels job
     */
    private void scheduleRefreshChannelsJob(NuvoTunerEnum tuner, long delaySeconds) {
        cancelRefreshChannelsJob();
        logger.debug("Schedule refresh channels job");

        refreshChannelsJob = scheduler.schedule(() -> {
            if (connector.isConnected()) {
                synchronized (sequenceLock) {
                    try {
                        if ("T2SIR".equals(tunerType)) {
                            logger.info("Refreshing Sirius channels list for {}...", tuner);
                            connector.sendCommand(tuner, NuvoTunerCommand.GET_SIRIUS_CHANNEL_LIST);
                        } else if ("T2DAB".equals(tunerType)) {
                            logger.info("Refreshing DAB channels list for {}...", tuner);
                            connector.sendCommand(tuner, NuvoTunerCommand.GET_DAB_CHANNEL_LIST);
                        } else {
                            logger.debug("Refreshing channels N/A on T2FAM tuner");
                        }
                    } catch (NuvoTunerException e) {
                        logger.debug("Error refreshing channels list for {}: {}", tuner, e.getMessage());
                    }
                }
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * Cancel the refresh channels job
     */
    private void cancelRefreshChannelsJob() {
        ScheduledFuture<?> refreshChannelsJob = this.refreshChannelsJob;
        if (refreshChannelsJob != null) {
            refreshChannelsJob.cancel(true);
            this.refreshChannelsJob = null;
        }
    }
}
