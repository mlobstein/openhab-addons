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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@code PacketUtil} class contains utility methods for processing packets from/to the GW1000U
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class LacrossePacketUtil {
    protected static String fmtBytes(String data) {
        if (data == null || data.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length(); i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(String.format("%02x", (int) data.charAt(i)));
        }
        return sb.toString();
    }

    protected static String createGatewayRegResponse(String server) {
        // 252-byte reply
        StringBuilder sb = new StringBuilder();

        // 8 bytes of 0
        for (int i = 0; i < 8; i++) {
            sb.append((char) 0);
        }

        // server left-justified to 0x98 (152) bytes, padded with 0s
        sb.append(leftJustify(server, 0x98, (char) 0));

        // ("%s%s%s" % (server, chr(0), server)) left-justified to 0x56 (86) bytes, padded with 0s
        String s0 = server + (char) 0 + server;
        sb.append(leftJustify(s0, 0x56, (char) 0));

        // 5 bytes of 0
        for (int i = 0; i < 5; i++) {
            sb.append((char) 0);
        }

        // 1 byte of 0xff
        sb.append((char) 0xff);

        return sb.toString();
    }

    protected static String leftJustify(String s, int width, char pad) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) {
            sb.append(pad);
        }
        if (sb.length() > width) {
            sb.setLength(width);
        }
        return sb.toString();
    }

    protected static String createGatewayPingResponse(int interval) {
        // 18-byte reply
        int hi = interval / 256;
        int lo = interval % 256;
        StringBuilder sb = new StringBuilder();

        // 16 bytes of 0
        for (int i = 0; i < 16; i++) {
            sb.append((char) 0);
        }

        // 1 byte: hi
        sb.append((char) hi);

        // 1 byte: lo
        sb.append((char) lo);

        return sb.toString();
    }

    protected static String createStationPingResponse(String serial, int sensorInterval, int historyInterval,
            int brightness, int lastHistoryAddress) {
        // 38-byte reply
        // sensor_interval is in minutes
        StringBuilder payload = new StringBuilder();

        int hi = lastHistoryAddress / 256;
        int lo = lastHistoryAddress % 256;

        payload.append((char) 0x01);
        payload.append(encodeSerial(serial)); // 8 bytes starting with 7fff

        payload.append((char) 0x00).append((char) 0x32).append((char) 0x00).append((char) 0x0b).append((char) 0x00)
                .append((char) 0x00).append((char) 0x00).append((char) 0x0f).append((char) 0x00).append((char) 0x00)
                .append((char) 0x00);

        payload.append((char) (sensorInterval - 1)); // byte 0x14 (0x3)
        payload.append((char) 0x00);
        payload.append((char) hi).append((char) lo); // last_history_address 2 bytes (0x3e 0xde)

        payload.append(encodeTs(new Date())); // 6 bytes

        payload.append((char) 0x53);
        payload.append((char) historyInterval); // byte 0x1f (0x7)
        payload.append((char) brightness - 1); // byte 0x20 (0x4)
        payload.append((char) 0x00).append((char) 0x00);
        payload.append((char) 0x00);

        int cs = checksum16p7(payload.toString());
        payload.append((char) cs >> 8).append((char) cs & 0xff);

        return payload.toString();
    }

    protected static String createSensorPingResponse(int sensorId, String sensorSerial, int sensorInterval) {
        // 19-byte reply
        // sensor_interval is in minutes
        StringBuilder payload = new StringBuilder();

        // 1 byte: sensor ID
        payload.append((char) sensorId);

        // 8 bytes: encoded serial
        payload.append(encodeSerial(sensorSerial));

        // 7 fixed bytes: 0x00 0x32 0x00 0x14 0x00 0x00 0x00
        payload.append((char) 0x00).append((char) 0x32).append((char) 0x00).append((char) 0x14).append((char) 0x00)
                .append((char) 0x00).append((char) 0x00);

        // 1 byte: sensor interval
        payload.append((char) sensorInterval);

        // 2 bytes: checksum16p7
        int cs = checksum16p7(payload.toString());
        payload.append((char) ((cs >> 8) & 0xFF));
        payload.append((char) (cs & 0xFF));

        return payload.toString();
    }

    protected static String createStationRegResponse(String serial, int brightness) {
        StringBuilder payload = new StringBuilder();

        // 38-byte reply
        // FIXME: this looks a lot like the ping response, with the checksum
        // the only difference. need more samples from lacrosse alerts to
        // see whether the last two bytes really should be calculated the
        // same way as those of the ping response.

        // chr(1)
        payload.append((char) 1);

        // encode_serial(serial) -- 8 bytes
        payload.append(encodeSerial(serial));

        // chr(0) + chr(0x30) + chr(0) + chr(0xf) + chr(0) + chr(0) + chr(0) + chr(0xf) + chr(0) + chr(0) + chr(0)
        payload.append((char) 0).append((char) 0x30).append((char) 0).append((char) 0x0f).append((char) 0)
                .append((char) 0).append((char) 0).append((char) 0x0f).append((char) 0).append((char) 0)
                .append((char) 0);

        // chr(0x77) -- FIXME: should be sensor interval minus one?
        payload.append((char) 0x77);

        // chr(0)
        payload.append((char) 0);

        // chr(0xe) + chr(0xff) -- FIXME: should be last history address?
        payload.append((char) 0x0e).append((char) 0xff);

        // encode_ts(current date/time) -- 6 bytes
        payload.append(encodeTs(new Date()));

        // chr(0x53)
        payload.append((char) 0x53);

        // chr(0x7) -- history interval?
        payload.append((char) 0x07);

        // chr(brightness - 1) -- LCD brightness
        payload.append((char) (brightness - 1));

        // chr(0) + chr(0) -- beep weather station
        payload.append((char) 0).append((char) 0);

        // chr(0) -- unknown
        payload.append((char) 0);

        // chr(0x7) -- unknown
        payload.append((char) 0x07);

        // Compute checksum8
        int cs = checksum8(payload.toString());
        payload.append((char) cs);

        return payload.toString();
    }

    protected static int checksum8(String input) {
        int n = 0;
        for (int i = 0; i < input.length(); i++) {
            n += input.charAt(i);
        }
        return n & 0xff;
    }

    protected static int checksum16p7(String input) {
        int n = 7;
        for (int i = 0; i < input.length(); i++) {
            n += input.charAt(i);
        }
        return n & 0xffff;
    }

    // Decodes a String containing raw bytes into a hex string
    protected static String decodeSerial(String data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length(); i++) {
            sb.append(String.format("%02x", (data.charAt(i) & 0xFF)));
        }
        return sb.toString();
    }

    // Assumes the serial number is a 16-character hex string, encoding it into 8 bytes
    protected static String encodeSerial(String sn) {
        if (sn.length() != 16)
            throw new IllegalArgumentException("Serial must be 16 hex characters");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sn.length(); i += 2) {
            String byteStr = sn.substring(i, i + 2);
            int value = Integer.parseInt(byteStr, 16);
            sb.append((char) value);
        }
        return sb.toString();
    }

    // Encodes a timestamp (seconds since epoch) into 6 bytes (as a String)
    protected static String encodeTs(Date date) {
        // Format: "HHmmssddMMyy", local time
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmssddMMyy", Locale.US);
        // If you want UTC instead of local, uncomment the next line:
        // sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String tstr = sdf.format(date);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            String part = tstr.substring(i * 2, i * 2 + 2);
            sb.append((char) encodeBcd(part));
        }
        return sb.toString();
    }

    // Encodes a two-digit decimal string into a BCD byte
    protected static int encodeBcd(String x) {
        int val = Integer.parseInt(x);
        int msb = val / 10;
        int lsb = val % 10;
        if (msb > 10) {
            msb = 10;
        }
        return ((msb << 4) | (lsb & 0xF));
    }

    protected static String toHex(String data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length(); i++) {
            sb.append(String.format("%02x", data.charAt(i) & 0xFF));
        }
        return sb.toString();
    }
}
