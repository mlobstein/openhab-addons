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
package org.openhab.binding.lacrosse.internal.interceptor;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code GatewayInterceptorServlet} class acts as the registered end point to receive requests from the La Crosse
 * GW1000U ERF gateway.
 *
 * The communication routines were adapted from https://github.com/matthewwall/weewx-interceptor and translated from
 * Python using GitHub Copilot.
 *
 * This implementation also includes functionality to register and receive data from TX-60 temp/humidity sensors
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class LacrosseGatewayInterceptorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String UNREGISTERED_SERIAL = "0102030405060708";
    private static final String EMPTY_SERIAL = "0000000000000000";
    private static final String SERVER_NAME = "box.weatherdirect.com";

    private final int pingInterval = 240; // how often gateway should ping the server, in seconds
    private final int sensorInterval = 5; // minutes between data packets
    private final int historyInterval = 1; // value for history interval: 0x01 - 5 minutes
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

    /**
     * Handles PUT requests from the GW1000U ERF gateway on the '/request.breq' end point
     *
     * @param request The HttpServletRequest object
     * @param response The HttpServletResponse object
     */
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
            String gatewaySn = parts[0];
            String id1 = parts[1];
            String code = parts[2];
            String id2 = parts[3];

            String stationSerial = null;
            List<String> sensorSerials = null;

            // get the configMap for this gatewaySn
            final Optional<SimpleEntry<String, ?>> configMapContainer = interceptorService.getConfigMaps().stream()
                    .filter(map -> gatewaySn.equals(map.getKey())).findFirst();
            if (configMapContainer.isPresent()) {
                final SimpleEntry<String, ?> configMap = configMapContainer.get();

                if (configMap.getValue() instanceof String) {
                    stationSerial = configMap.getValue().toString().toLowerCase(Locale.ENGLISH);
                } else if (configMap.getValue() instanceof List<?>) {
                    sensorSerials = ((List<String>) configMap.getValue()).stream().map(String::toLowerCase)
                            .collect(Collectors.toList());
                }
            } else {
                logger.debug("gateway configMap not found for gatewaySn: {}", gatewaySn);
            }

            interceptorService.putGatewayIpAddress(gatewaySn, request.getRemoteAddr());

            final String pktType = String.format("%s:%s", id1, id2).toUpperCase(Locale.ENGLISH);
            logger.debug("pktType: {}", pktType);
            final String lengthStr = request.getHeader("Content-Length");
            int length = 0;
            String data = "";
            boolean isPing = false;
            boolean isGwCheck = false;

            if (lengthStr != null && lengthStr.chars().allMatch(Character::isDigit)) {
                length = Integer.parseInt(lengthStr);
            }

            if (length > 0) {
                // Read one character at a time from the request body (a short string of binary data from the
                // weather station or sensor) and add it to the string builder
                final StringBuilder sb = new StringBuilder();
                int ch;
                while ((ch = request.getReader().read()) != -1) {
                    sb.append((char) ch);
                }
                data = sb.toString();
            }

            logger.debug("recv: {}:{} {} {} {}", id1, id2, gatewaySn, code, LacrossePacketUtil.fmtBytes(data));

            switch (pktType) {
                case "00:10":
                    // gateway power up
                    logger.debug("power up from gateway with sn {}", gatewaySn);
                    flags = "10:00";
                    isPing = true;
                    isGwCheck = true;
                    break;
                case "00:14":
                    // received after response to 7f:10 packet.
                    // gateway sends 14 bytes.
                    logger.debug("registration confirmed for gatewaySn {} ({})", gatewaySn,
                            LacrossePacketUtil.fmtBytes(data));
                    flags = "1C:00";
                    break;
                case "00:20":
                    // gateway registration
                    logger.debug("registration from gateway with gatewaySn {}", gatewaySn);
                    flags = "20:00"; // sometimes replies with 20:01
                    isGwCheck = true;
                    responseStr = LacrossePacketUtil.createGatewayRegResponse(SERVER_NAME);
                    break;
                case "00:30":
                    // received after response to 00:20 packet
                    flags = "30:00"; // also observed 30:01
                    break;
                case "00:70":
                    // gateway ping
                    flags = "70:00"; // also observed 20:01
                    isPing = true;
                    isGwCheck = true;
                    responseStr = LacrossePacketUtil.createGatewayPingResponse(pingInterval);
                    break;
                case "01:00":
                case "02:00":
                case "03:00":
                case "04:00":
                case "05:00":
                    flags = "14:01";
                    isPing = true;
                    isGwCheck = true;

                    // determine if the configured device is a weather station or sensor for the proper ping response
                    if (stationSerial != null && !stationSerial.isBlank()) {
                        // station ping. gateway sends 5 bytes.
                        responseStr = LacrossePacketUtil.createStationPingResponse(stationSerial, sensorInterval,
                                historyInterval, lcdBrightness, interceptorService.getLastHistoryAddress(gatewaySn));
                    } else if (sensorSerials != null && !sensorSerials.isEmpty()) {
                        // sensor ping
                        final int sensorId = Integer.parseInt(pktType.substring(1, 2));
                        if (!sensorSerials.get(sensorId - 1).isBlank()) {
                            responseStr = LacrossePacketUtil.createSensorPingResponse(sensorId,
                                    sensorSerials.get(sensorId - 1), sensorInterval);
                        } else {
                            logger.debug("sensor serial number not found");
                        }
                    }
                    break;
                case "01:14":
                case "02:14":
                case "03:14":
                case "04:14":
                case "05:14":
                    // station or sensor registration confirmation. gateway sends 14 bytes.
                    // the first 8 bytes are the serial (7fffxxxxxxxx for a weather station)
                    if (!data.isBlank()) {
                        final String sn = LacrossePacketUtil.decodeSerial(data.substring(0, 8));
                        if (stationSerial == null || (sn.startsWith("7fff") && EMPTY_SERIAL.equals(stationSerial))) {
                            logger.debug("using serial {}", sn);
                            stationSerial = sn;
                        }
                        if (stationSerial.equals(sn)) {
                            flags = "1C:00";
                            logger.debug("responded to msg {}:14 gatewaySn={} sn={} ({})", id1, gatewaySn, sn,
                                    LacrossePacketUtil.fmtBytes(data));
                        } else {
                            logger.debug("ignored msg {}:14 gatewaySn={} sn={} ({})", id1, gatewaySn, sn,
                                    LacrossePacketUtil.fmtBytes(data));
                        }
                    } else {
                        logger.debug("ignored msg {}:14 with no serial gatewaySn={} ({})", id1, gatewaySn,
                                LacrossePacketUtil.fmtBytes(data));
                    }
                    break;
                case "7F:10":
                    boolean doReply = false;
                    // station or sensor registration. gateway sends 13 bytes.
                    // the first 8 bytes are the serial (7fffxxxxxxxx for a weather station)
                    if (data.length() >= 8) {
                        final String sn = LacrossePacketUtil.decodeSerial(data.substring(0, 8));

                        // station registration
                        if (stationSerial != null && !stationSerial.isBlank()) {
                            if (sn.startsWith("7fff") && EMPTY_SERIAL.equals(stationSerial)) {
                                logger.debug("using serial {}", sn);
                                stationSerial = sn;
                                doReply = false;
                            } else if (stationSerial.equals(sn)) {
                                doReply = true;
                            }
                            // sensor registration, verify this gateway owns the sensor sn
                        } else if (sensorSerials != null && sensorSerials.contains(sn)) {
                            doReply = true;
                        }
                        if (UNREGISTERED_SERIAL.equals(sn)) {
                            if (stationSerial.startsWith("7fff")) {
                                logger.debug("assigning serial {} to unregistered station sn={} ({})", stationSerial,
                                        gatewaySn, LacrossePacketUtil.fmtBytes(data));
                                doReply = true;
                            } else {
                                // FIXME: generate a new registration key
                                logger.debug("ignored unregistered station gatewaySn={} ({})", gatewaySn,
                                        LacrossePacketUtil.fmtBytes(data));
                            }
                        }
                        if (doReply) {
                            flags = "14:00";
                            if (stationSerial != null && !stationSerial.isBlank()) {
                                responseStr = LacrossePacketUtil.createStationRegResponse(sn, lcdBrightness);
                            } else if (sensorSerials != null && sensorSerials.indexOf(sn) != -1) {
                                responseStr = LacrossePacketUtil.createSensorPingResponse(sensorSerials.indexOf(sn) + 1,
                                        sn, sensorInterval);
                            } else {
                                logger.debug("response NOT sent for msg 7F:10 gatewaySn={} sn={} ({})", gatewaySn, sn,
                                        LacrossePacketUtil.fmtBytes(data));
                            }

                            logger.debug("responded to msg 7F:10 gatewaySn={} sn={} ({})", gatewaySn, sn,
                                    LacrossePacketUtil.fmtBytes(data));
                        } else {
                            logger.debug("ignored msg 7F:10 gatewaySn={} sn={} ({})", gatewaySn, sn,
                                    LacrossePacketUtil.fmtBytes(data));
                        }
                    } else {
                        logger.debug("ignored msg 7F:10 with no serial gatewaySn={} ({})", gatewaySn,
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
                    isPing = true;
                    if (!data.isBlank()) {
                        if ((data.charAt(0) & 0xFF) == 0x01) {
                            // this is a current conditions packet, process it
                            interceptorService.dispatchReceivedGatewayData(gatewaySn, pktType,
                                    LacrossePacketUtil.toHex(data));
                        } else if ((data.charAt(0) & 0xFF) == 0x21) {
                            // this is a history packet, get the history address
                            final int caddr = ((data.charAt(4) & 0xFF) * 256) + (data.charAt(5) & 0xFF);
                            final int naddr = ((data.charAt(6) & 0xFF) * 256) + (data.charAt(7) & 0xFF);

                            interceptorService.saveLastHistoryAddress(gatewaySn, caddr);
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

            if (isPing) {
                interceptorService.dispatchReceivedPing(gatewaySn);
            }

            // save connection information of gateways not configured for user page display and remove once configured
            if (isGwCheck) {
                if (!interceptorService.getConfigMaps().stream().filter(map -> gatewaySn.equals(map.getKey()))
                        .findFirst().isPresent()) {
                    interceptorService.putUnconfiguredGatewayInfo(gatewaySn, request.getRemoteAddr());
                } else if (interceptorService.getUnconfiguredGatewayMap().containsKey(gatewaySn)
                        && interceptorService.getConfigMaps().stream().filter(map -> gatewaySn.equals(map.getKey()))
                                .findFirst().isPresent()) {
                    interceptorService.removeUnconfiguredGatewayInfo(gatewaySn);
                }
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
        response.setHeader("Date", LacrossePacketUtil.putResponseDate());
        response.setHeader("Connection", "close");
        response.getWriter().print(responseStr);
    }

    /**
     * Handles GET requests from the end user on the /request.breq end point in order to provide troubleshooting
     * information
     *
     * @param request The HttpServletRequest object
     * @param response The HttpServletResponse object
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final StringBuilder html = new StringBuilder(
                "<!doctype html>\n<html>\n<head>\n\t<title>LacrosseGatewayInterceptorServlet</title>\n</head>\n<body>\n<h1>LacrosseGatewayInterceptorServlet - active</h1>\n");

        final Map<String, String> unconfiguredGateways = interceptorService.getUnconfiguredGatewayMap();
        if (!unconfiguredGateways.isEmpty()) {
            html.append("<h3>Detected Gateways not configured in openHAB:</h3>\n<ul>\n");
            for (Map.Entry<String, String> unconfiguredGeteway : unconfiguredGateways.entrySet()) {
                html.append("\t<li>Gateway s/n ");
                html.append(unconfiguredGeteway.getKey());
                html.append(" <span style=\"color: #18864b\">online!</span> - <a href=\"http://");
                html.append(unconfiguredGeteway.getValue());
                html.append("/\" target=\"_blank\">status page</a></li>\n");
            }
            html.append("</ul>\n");
        }

        html.append("<h3>Configured Gateways:</h3>\n<ul>\n");
        interceptorService.getConfigMaps().forEach(configMap -> {
            html.append("\t<li>Gateway s/n ");
            html.append(configMap.getKey());
            if (interceptorService.getGatewayIpAddress(configMap.getKey()) != null) {
                html.append(" <span style=\"color: #18864b\">online!</span> - <a href=\"http://");
                html.append(interceptorService.getGatewayIpAddress(configMap.getKey()));
                html.append("/\" target=\"_blank\">status page</a>\n");
            } else {
                html.append("\n");
            }

            if (configMap.getValue() instanceof String) {
                html.append("\t\t<ul><li>Weather Station s/n ");
                html.append(configMap.getValue().toString().toUpperCase(Locale.ENGLISH));
                html.append("</li></ul>\n\t</li>\n");
            }

            if ((configMap.getValue() instanceof List<?>)) {
                final List<String> sensorSerials = (List<String>) configMap.getValue();

                if (sensorSerials != null && !sensorSerials.isEmpty()) {
                    html.append("\t\t<ol>\n");
                    sensorSerials.forEach(sensor -> {
                        if (!sensor.isEmpty()) {
                            html.append("\t\t\t<li>Sensor s/n ");
                            html.append(sensor.toUpperCase(Locale.ENGLISH));
                            html.append("</li>\n");
                        }
                    });
                    html.append("\t\t</ol>\n\t</li>\n");
                }
            }
        });
        if (interceptorService.getConfigMaps().isEmpty()) {
            html.append("\t<li>None - check <a href=\"/settings/things/\" target=\"_blank\">openHAB things</a></li>\n");
        }
        html.append("</ul>\n");

        html.append(
                "<br/>\n<a href=\"https://github.com/mlobstein/openhab-addons/tree/lacrosse/bundles/org.openhab.binding.lacrosse#readme\" target=\"_blank\">documentation</a>\n</body>\n</html>");

        response.setContentType("text/html;");
        response.getWriter().println(html.toString());
    }
}
