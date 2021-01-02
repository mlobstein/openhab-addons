/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.panasonicbr.internal.handler;

import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.openhab.binding.panasonicbr.internal.PanasonicBindingConstants.*;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.Fields;
import org.openhab.binding.panasonicbr.internal.PanasonicConfiguration;
import org.openhab.binding.panasonicbr.internal.PanasonicHttpException;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PanasonicHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class PanasonicHandler extends BaseThingHandler {
    private static final int DEFAULT_REFRESH_PERIOD_SEC = 10;

    private final Logger logger = LoggerFactory.getLogger(PanasonicHandler.class);
    private final HttpClient httpClient;

    private @Nullable ScheduledFuture<?> refreshJob;

    private String urlStr = "http://%host%/WAN/dvdr/dvdr_ctrl.cgi";
    private String nonceUrlStr = "http://%host%/cgi-bin/get_nonce.cgi";
    private int refreshInterval = DEFAULT_REFRESH_PERIOD_SEC;
    private String playMode = EMPTY;
    private String timeCode = ZERO;
    private String playerKey = EMPTY;
    private boolean authEnabled = false;
    private Object sequenceLock = new Object();
    private ThingTypeUID thingTypeUID = THING_TYPE_BD_PLAYER;

    public PanasonicHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Panasonic Blu-ray Player handler.");
        PanasonicConfiguration config = getConfigAs(PanasonicConfiguration.class);

        this.thingTypeUID = thing.getThingTypeUID();

        final @Nullable String host = config.hostName;
        final @Nullable String playerKey = config.playerKey;

        if (host != null && !EMPTY.equals(host)) {
            urlStr = urlStr.replace("%host%", host);
            nonceUrlStr = nonceUrlStr.replace("%host%", host);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Host Name must be specified");
            return;
        }

        if (playerKey != null && !EMPTY.equals(playerKey)) {
            this.playerKey = playerKey;
            authEnabled = true;
        }

        if (config.refresh >= 10) {
            refreshInterval = config.refresh;
        }

        updateStatus(ThingStatus.UNKNOWN);
        startAutomaticRefresh();
    }

    /**
     * Start the job to periodically get a status update from the player
     */
    private void startAutomaticRefresh() {
        ScheduledFuture<?> refreshJob = this.refreshJob;
        if (refreshJob == null || refreshJob.isCancelled()) {
            this.refreshJob = scheduler.scheduleWithFixedDelay(this::refreshPlayerStatus, 0, refreshInterval,
                    TimeUnit.SECONDS);
        }
    }

    /**
     * Sends commands to the player to get status information and updates the channels
     */
    private void refreshPlayerStatus() {
        final String[] statusLines = sendCommand(PST_POST_CMD, urlStr).split(CRLF);

        // a valid response will have at least two lines
        if (statusLines.length >= 2) {
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);

            // statusLines second line: 1,1543,0,00000000 (play mode, current time, ?, ?)
            final String statusArr[] = statusLines[1].split(COMMA);

            if (statusArr.length >= 2) {
                // update play mode if different
                if (!playMode.equals(statusArr[0])) {
                    playMode = statusArr[0];

                    switch (playMode) {
                        case ZERO:
                            updateState(PLAY_MODE, new StringType(STOP));
                            updateState(TIME_ELAPSED, UnDefType.UNDEF);
                            updateState(TIME_TOTAL, UnDefType.UNDEF);
                            updateState(CHAPTER_CURRENT, UnDefType.UNDEF);
                            updateState(CHAPTER_TOTAL, UnDefType.UNDEF);
                            // update cached time code with current time code so update below will not occur
                            // necessary because the player does not clear reported time code when stopped
                            timeCode = statusArr[1];
                            break;
                        case ONE:
                            updateState(PLAY_MODE, new StringType(PLAY));
                            break;
                        case TWO:
                            updateState(PLAY_MODE, new StringType(PAUSE));
                            break;
                        default:
                            logger.debug("Unknown playMode type: {}", playMode);
                            updateState(PLAY_MODE, new StringType(UNKNOWN));
                            updateState(TIME_ELAPSED, UnDefType.UNDEF);
                            updateState(TIME_TOTAL, UnDefType.UNDEF);
                            updateState(CHAPTER_CURRENT, UnDefType.UNDEF);
                            updateState(CHAPTER_TOTAL, UnDefType.UNDEF);
                            return;
                    }
                }

                // update time code and playback status if time code changes
                // it stops changing when paused or stopped, preventing the second http call running needlessly
                if (!timeCode.equals(statusArr[1])) {
                    timeCode = statusArr[1];
                    updateState(TIME_ELAPSED, new QuantityType<>(Integer.parseInt(timeCode), API_SECONDS_UNIT));

                    // UHD players do not provide extended playback info
                    if (thingTypeUID.equals(THING_TYPE_BD_PLAYER)) {
                        updatePlaybackStatus();
                    }
                }
            }
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing the Panasonic Blu-ray Player handler.");

        ScheduledFuture<?> refreshJob = this.refreshJob;
        if (refreshJob != null) {
            refreshJob.cancel(true);
            this.refreshJob = null;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            logger.debug("Unsupported refresh command: {}", command);
        } else if (channelUID.getId().equals(BUTTON)) {
            synchronized (sequenceLock) {
                sendCommand(command.toString(), urlStr, authEnabled);
            }
        } else {
            logger.debug("Unsupported command: {}", command);
        }
    }

    /**
     * Sends a command to the player by building a POST with the command string embedded
     *
     * @param String the command to be sent to the player
     * @param String the url to receive the command
     * @param boolan a flag to indicate if authentication should be used for the command
     * @return the response string from the player
     */
    private String sendCommand(String command, String url, Boolean isAuth) {
        String authKey = EMPTY;
        if (isAuth) {
            String nonce = sendCommand(GET_NONCE_CMD, nonceUrlStr).trim();
            try {
                authKey = getAuthKey(playerKey + nonce);
            } catch (NoSuchAlgorithmException e) {
                logger.debug("Error creating auth key: {}", e.getMessage());
                return EMPTY;
            }
        }

        // build the fields to POST from the command string
        Fields fields = new Fields();
        fields.add("cCMD_" + command + ".x", "100");
        fields.add("cCMD_" + command + ".y", "100");
        if (isAuth) {
            fields.add("cAUTH_FORM", "C4");
            fields.add("cAUTH_VALUE", authKey);
        }
        return sendCommand(fields, url);
    }

    /**
     * Sends a command to the player using a pre-built post body
     *
     * @param Fields a pre-built post body to send to the player
     * @param String the url to receive the command
     * @return the response string from the player
     */
    private String sendCommand(Fields fields, String url) {
        logger.debug("Blu-ray command: {}", fields.getNames().iterator().next());

        try {
            ContentResponse response = httpClient.POST(url).agent(USER_AGENT).method(HttpMethod.POST)
                    .content(new FormContentProvider(fields)).send();

            String output = response.getContentAsString();
            logger.debug("Blu-ray response: {}", output);

            if (response.getStatus() != OK_200) {
                throw new PanasonicHttpException("Player response: " + response.getStatus() + " - " + output);
            }
            return output;

        } catch (PanasonicHttpException | InterruptedException | TimeoutException | ExecutionException e) {
            logger.debug("Error executing player command: {}, {}", fields.getNames().iterator().next(), e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    "Error communicating with the player");
        }
        return EMPTY;
    }

    /**
     * Secondary call to get additional playback status info
     */
    private void updatePlaybackStatus() {
        final String[] statusLines = sendCommand(STATUS_POST_CMD, urlStr).split(CRLF);

        // get the second line of the status message
        // 1,0,0,1,5999,61440,500,1,16,00000000 (?, ?, ?, cur time, total time, title#?, ?, chapt #, total chapt, ?)
        if (statusLines.length >= 2) {
            final String statusArr[] = statusLines[1].split(COMMA);
            if (statusArr.length >= 10) {
                updateState(TIME_TOTAL, new QuantityType<>(Integer.parseInt(statusArr[4]), API_SECONDS_UNIT));
                updateState(CHAPTER_CURRENT, new DecimalType(Integer.parseInt(statusArr[7])));
                updateState(CHAPTER_TOTAL, new DecimalType(Integer.parseInt(statusArr[8])));
            }
        }
    }

    /**
     * Returns a SHA-256 hash of the input string
     *
     * @param String the input string to generate the hash from
     * @return the 256 bit hash string
     */
    private String getAuthKey(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(SHA_256_ALGORITHM);
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        byte[] hash = md.digest(inputBytes);

        // Convert byte array into signum representation
        BigInteger number = new BigInteger(1, hash);

        // Convert message digest into hex value
        StringBuilder hexString = new StringBuilder(number.toString(16));

        // Pad with leading zeros
        while (hexString.length() < 32) {
            hexString.insert(0, ZERO);
        }

        return hexString.toString().toUpperCase();
    }
}
