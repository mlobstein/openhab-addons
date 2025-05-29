/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.binding.lacrosse.internal.interceptor;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code GatewayInterceptorServlet} class acts as the registered end point to receive requests from the Lacrosse
 * GW1000U gateway.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class LacrosseGatewayInterceptorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String UNREGISTERED_SERIAL = "0102030405060708";
    private static final String EMPTY_SERIAL = "0000000000000000";
    private static final String SERVER_NAME = "box.weatherdirect.com";

    private static final SimpleDateFormat HEADER_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
    static {
        HEADER_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private String stationSerial = EMPTY_SERIAL; // serial from lacrosse, starts with 7fff
    private final int pingInterval = 240; // how often gateway should ping the server, in seconds
    private final int sensorInterval = 5; // minutes between data packets
    private final int historyInterval = 5; // minutes between data packets
    private final int lcdBrightness = 4;

    private LacrosseGatewayInterceptorService interceptorService;
    private final Logger logger = LoggerFactory.getLogger(LacrosseGatewayInterceptorServlet.class);

    /**
     * Constructs a {@code LacrosseGatewayInterceptorServlet} associated to the given
     * {@link LacrosseGatewayInterceptorService} service
     *
     * @param interceptorService The service to associate to the servlet
     */
    public LacrosseGatewayInterceptorServlet(LacrosseGatewayInterceptorService interceptorService) {
        this.interceptorService = interceptorService;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;");
        response.getWriter().println("<h1>Hello world! this is the LacrosseGatewayInterceptorServlet</h1>");
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {

        final String header = request.getHeader("HTTP_IDENTIFY");

        String[] parts = new String[] { "" };
        String flags = "00:00";
        String responseStr = "";

        if (header != null) {
            parts = header.split(":");
        } else {
            logger.debug("no HTTP_IDENTIFY in headers");
        }

        if (parts.length == 4) {
            String mac = parts[0];
            String id1 = parts[1];
            String code = parts[2];
            String id2 = parts[3];

            String pktType = String.format("%s:%s", id1, id2).toUpperCase();
            logger.debug("pktType: {}", pktType);
            String lengthStr = request.getHeader("Content-Length");
            int length = 0;
            String data = "";

            if (lengthStr != null && lengthStr.chars().allMatch(Character::isDigit)) {
                length = Integer.parseInt(lengthStr);
            }

            if (length > 0) {
                data = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            }

            logger.debug("recv: {}:{} {} {} {}", id1, id2, mac, code, LacrossePacketUtil.fmtBytes(data));

            switch (pktType) {
                case "00:10":
                    // gateway power up
                    logger.debug("power up from gateway with mac {}", mac);
                    flags = "10:00";
                    break;
                case "00:14":
                    // received after response to 7f:10 packet.
                    // gateway sends 14 bytes.
                    logger.debug("registration confirmed for mac {} ({})", mac, LacrossePacketUtil.fmtBytes(data));
                    flags = "1C:00";
                    break;
                case "00:20":
                    // gateway registration
                    logger.debug("registration from gateway with mac {}", mac);
                    flags = "20:00"; // sometimes replies with 20:01
                    responseStr = LacrossePacketUtil.createGatewayRegResponse(SERVER_NAME);
                    break;
                case "00:30":
                    // received after response to 00:20 packet
                    flags = "30:00"; // also observed 30:01
                    break;
                case "00:70":
                    // gateway ping
                    flags = "70:00"; // also observed 20:01
                    responseStr = LacrossePacketUtil.createGatewayPingResponse(pingInterval);
                    break;
                case "01:00":
                case "02:00":
                case "03:00":
                case "04:00":
                case "05:00":
                    flags = "14:01";
                    // get the configMap for this mac
                    final Optional<SimpleEntry<String, Object>> configMapContainer = interceptorService.getConfigMaps()
                            .stream().filter(map -> mac.equals(map.getKey())).findFirst();

                    if (configMapContainer.isPresent()) {
                        final SimpleEntry<String, Object> configMap = configMapContainer.get();

                        // determine if gateway is for a Weather Station or Sensor in order to properly respond to pings
                        if (configMap.getValue() instanceof String) {
                            // station ping. gateway sends 5 bytes.
                            final String stationSerial = (String) configMap.getValue();

                            // TODO: fix hardcode
                            int lastHistoryAddress = 1;

                            if (stationSerial != null && !stationSerial.isEmpty()) {
                                responseStr = LacrossePacketUtil.createStationPingResponse(stationSerial,
                                        sensorInterval, historyInterval, lcdBrightness, lastHistoryAddress);
                            } else {
                                logger.debug("station serial number not found");
                            }
                        } else if (configMap.getValue() instanceof List<?>) {
                            // sensor ping
                            final int sensorId = Integer.parseInt(pktType.substring(1, 2));
                            final List<String> sensorSerials = (List<String>) configMap.getValue();
                            if (!sensorSerials.get(sensorId).isEmpty()) {
                                responseStr = LacrossePacketUtil.createSensorPingResponse(sensorId,
                                        sensorSerials.get(sensorId), sensorInterval);
                            } else {
                                logger.debug("sensor serial number not found");
                            }
                        } else {
                            logger.debug("gateway configMap not found for mac: {}", mac);
                        }
                    }
                    break;
                case "01:14":
                    // unknown. gateway sends 14 bytes.
                    // the first 8 bytes are the serial 7fffxxxxxxxx
                    if (!data.isEmpty()) {
                        String sn = LacrossePacketUtil.decodeSerial(data.substring(0, 8));
                        if (sn.startsWith("7fff") && EMPTY_SERIAL.equals(stationSerial)) {
                            logger.debug("using serial {}", sn);
                            stationSerial = sn;
                        }
                        if (stationSerial.equals(sn)) {
                            flags = "1C:00";
                            logger.debug("responded to msg 01:14 mac={} sn={} ({})", mac, sn,
                                    LacrossePacketUtil.fmtBytes(data));
                        } else {
                            logger.debug("ignored msg 01:14 mac={} sn={} ({})", mac, sn,
                                    LacrossePacketUtil.fmtBytes(data));
                        }
                    } else {
                        logger.debug("ignored msg 01:14 with no serial mac={} ({})", mac,
                                LacrossePacketUtil.fmtBytes(data));
                    }
                    break;
                case "7F:10":
                    boolean doReply = false;
                    // station registration. gateway sends 13 bytes.
                    // the first 8 bytes are the serial 7fffxxxxxxxx
                    if (data.length() >= 8) {
                        String sn = LacrossePacketUtil.decodeSerial(data.substring(0, 8));
                        if (sn.startsWith("7fff") && EMPTY_SERIAL.equals(stationSerial)) {
                            logger.debug("using serial {}", sn);
                            stationSerial = sn;
                            doReply = false;
                        }
                        if (stationSerial.equals(sn)) {
                            doReply = true;
                        }
                        if (UNREGISTERED_SERIAL.equals(sn)) {
                            if (stationSerial.startsWith("7fff")) {
                                logger.debug("assigning serial {} to unregistered station mac={} ({})", stationSerial,
                                        mac, LacrossePacketUtil.fmtBytes(data));
                                doReply = true;
                            } else {
                                // FIXME: generate a new registration key
                                logger.debug("ignored unregistered station mac={} ({})", mac,
                                        LacrossePacketUtil.fmtBytes(data));
                            }
                        }
                        if (doReply) {
                            flags = "14:00";
                            responseStr = LacrossePacketUtil.createStationRegResponse(sn, lcdBrightness);
                            logger.debug("responded to msg 7F:10 mac={} sn={} ({})", mac, sn,
                                    LacrossePacketUtil.fmtBytes(data));
                        } else {
                            logger.debug("ignored msg 7F:10 mac={} sn={} ({})", mac, sn,
                                    LacrossePacketUtil.fmtBytes(data));
                        }
                    } else {
                        logger.debug("ignored msg 7F:10 with no serial mac={} ({})", mac,
                                LacrossePacketUtil.fmtBytes(data));
                    }
                    break;
                case "01:01":
                case "02:01":
                case "03:01":
                case "04:01":
                case "05:01":
                    // data packet
                    flags = "00:00"; // also observed 00:01
                    if (data != null && data.length() > 0) {
                        if ((data.charAt(0) & 0xFF) == 0x01) {
                            // this is a current conditions packet, process it
                            interceptorService.dispatchReceivedGatewayData(mac, pktType,
                                    LacrossePacketUtil.toHex(data));
                        } else if ((data.charAt(0) & 0xFF) == 0x21) {
                            // this is a history packet, get the history address
                            int caddr = ((data.charAt(4) & 0xFF) * 256) + (data.charAt(5) & 0xFF);
                            int naddr = ((data.charAt(6) & 0xFF) * 256) + (data.charAt(7) & 0xFF);

                            // lastHistoryAddress = caddr; TODO: What is the history packet address ever needed for?
                            logger.debug("current_addr=0x{}x next_addr=0x{}x", String.format("%04", caddr),
                                    String.format("%04", naddr));
                        } else {
                            logger.debug("unknown data packet type: {}", LacrossePacketUtil.fmtBytes(data));
                        }
                    } else {
                        logger.debug("empty data packet, packet type: {}", pktType);
                    }
                    break;
                default:
                    logger.debug("unknown data packet type: {}", pktType);
            }
        } else {
            logger.debug("unknown format for HTTP_IDENTIFY: {}", header);
        }

        logger.debug("send: {} {}", flags, LacrossePacketUtil.fmtBytes(responseStr));
        logger.debug("length: {} ", responseStr.length());

        response.setStatus(200);
        response.setHeader("Cache-Control", "private");
        response.setHeader("Content-Length", String.valueOf(responseStr.length()));
        response.setHeader("Content-Type", "application/octet-stream");
        response.setHeader("Server", "Microsoft-IIS/8.0");
        response.setHeader("X-ApsNet-Version", "2.0.50727");
        response.setHeader("HTTP_FLAGS", flags);
        response.setHeader("X-Powered-By", "ASP.NET");
        response.setHeader("Date", HEADER_DATE_FORMAT.format(new Date()));
        response.setHeader("Connection", "close");
        response.getWriter().println(responseStr);
    }
}
