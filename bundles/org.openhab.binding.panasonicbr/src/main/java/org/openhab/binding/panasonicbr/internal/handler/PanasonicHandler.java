/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

    // pre-define the POST body for status update calls
    private static final Fields PST_POST_CMD = new Fields();
    static {
        PST_POST_CMD.add("cCMD_PST.x", "100");
        PST_POST_CMD.add("cCMD_PST.y", "100");
    }

    private static final Fields STATUS_POST_CMD = new Fields();
    static {
        STATUS_POST_CMD.add("cCMD_GET_STATUS.x", "100");
        STATUS_POST_CMD.add("cCMD_GET_STATUS.y", "100");
    }

    private static final Fields GET_NONCE_CMD = new Fields();
    static {
        GET_NONCE_CMD.add("SID", "1234ABCD");
    }

    private final Logger logger = LoggerFactory.getLogger(PanasonicHandler.class);
    private final HttpClient httpClient;

    private @Nullable ScheduledFuture<?> refreshJob;

    private String urlStr = "http://%host%/WAN/dvdr/dvdr_ctrl.cgi";
    private String nonceUrlStr = "http://%host%/cgi-bin/get_nonce.cgi";
    private int refreshInterval = DEFAULT_REFRESH_PERIOD_SEC;
    private String playMode = "";
    private String timeCode = "0";
    private String playerKey = "";
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

        @Nullable
        final String host = config.hostName;

        @Nullable
        final String playerKey = config.playerKey;

        if (host != null && !host.equals("")) {
            urlStr = urlStr.replace("%host%", host);
            nonceUrlStr = nonceUrlStr.replace("%host%", host);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Host Name must be specified");
            return;
        }

        if (playerKey != null && !playerKey.equals("")) {
            this.playerKey = playerKey;
            authEnabled = true;
        }

        if (config.refresh >= 10)
            refreshInterval = config.refresh;

        updateStatus(ThingStatus.UNKNOWN);
        startAutomaticRefresh();
    }

    /**
     * Start the job to periodically get a status update from the player
     */
    private void startAutomaticRefresh() {
        ScheduledFuture<?> refreshJob = this.refreshJob;
        if (refreshJob == null || refreshJob.isCancelled()) {
            Runnable runnable = () -> {
                final String[] statusLines = sendCommand(null, PST_POST_CMD, urlStr, false).split(CRLF);

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
            };
            this.refreshJob = scheduler.scheduleWithFixedDelay(runnable, 0, refreshInterval, TimeUnit.SECONDS);
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
            logger.debug("Unsupported refresh command: {}", command.toString());
        } else if (channelUID.getId().equals(BUTTON)) {
            synchronized (sequenceLock) {
                sendCommand(command.toString(), null, urlStr, authEnabled);
            }
        } else {
            logger.debug("Unsupported command: {}", command.toString());
        }
    }

    /**
     * Sends a command to the player (must send command string or pre-built post body, not both)
     *
     * @param String the command to be sent to the player
     * @param Fields a pre-built post body to send to the player
     * @param String the url to receive the command
     * @param boolan a flag to indicate if authentication should be used for the command
     * @return the response string from the player
     */
    private String sendCommand(@Nullable String command, @Nullable Fields fields, String url, Boolean isAuth) {
        String output = "";
        String authKey = "";

        if (isAuth) {
            String nonce = sendCommand(null, GET_NONCE_CMD, nonceUrlStr, false).trim();
            try {
                authKey = getAuthKey(playerKey + nonce);
            } catch (NoSuchAlgorithmException e) {
                logger.debug("Error creating auth key: {}", e.getMessage());
                return "";
            }
        }

        // if we were not sent the fields to post, build them from the string
        if (fields == null) {
            fields = new Fields();
            fields.add("cCMD_" + command + ".x", "100");
            fields.add("cCMD_" + command + ".y", "100");
            if (isAuth) {
                fields.add("cAUTH_FORM", "C4");
                fields.add("cAUTH_VALUE", authKey);
            }
        }
        logger.debug("Blu-ray command: {}", command != null ? command : fields.getNames().iterator().next());

        try {
            ContentResponse response = httpClient.POST(url).agent(USER_AGENT).method(HttpMethod.POST)
                    .content(new FormContentProvider(fields)).send();

            output = response.getContentAsString();
            logger.debug("Blu-ray response: {}", output);

            if (response.getStatus() != OK_200) {
                throw new PanasonicHttpException("Player response: " + response.getStatus() + " - " + output);
            }

        } catch (PanasonicHttpException | InterruptedException | TimeoutException | ExecutionException e) {
            logger.debug("Error executing player command: {}, {}", command, e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    "Error communicating with the player");
        }

        return output;
    }

    // secondary call to get additional playback status info
    private void updatePlaybackStatus() {
        final String[] statusLines = sendCommand(null, STATUS_POST_CMD, urlStr, false).split(CRLF);

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

    String getAuthKey(String input) throws NoSuchAlgorithmException {
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
