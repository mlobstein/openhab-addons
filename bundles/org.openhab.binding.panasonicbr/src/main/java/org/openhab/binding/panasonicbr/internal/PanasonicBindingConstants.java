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
package org.openhab.binding.panasonicbr.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.measure.Unit;
import javax.measure.quantity.Time;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.util.Fields;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link PanasonicBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class PanasonicBindingConstants {
    public static final String BINDING_ID = "panasonicbr";
    public static final String PROPERTY_UUID = "uuid";
    public static final String PROPERTY_HOST_NAME = "hostName";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BD_PLAYER = new ThingTypeUID(BINDING_ID, "bd_player");
    public static final ThingTypeUID THING_TYPE_UHD_PLAYER = new ThingTypeUID(BINDING_ID, "uhd_player");

    // List of all Channel id's
    public static final String POWER = "power";
    public static final String BUTTON = "button";
    public static final String PLAYER_STATUS = "playerStatus";
    public static final String TIME_ELAPSED = "timeElapsed";
    public static final String TIME_TOTAL = "timeTotal";
    public static final String CHAPTER_CURRENT = "chapterCurrent";
    public static final String CHAPTER_TOTAL = "chapterTotal";

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Stream.of(THING_TYPE_BD_PLAYER, THING_TYPE_UHD_PLAYER).collect(Collectors.toSet()));

    // Units of measurement of the data delivered by the API
    public static final Unit<Time> API_SECONDS_UNIT = Units.SECOND;

    public static final String USER_AGENT = "MEI-LAN-REMOTE-CALL";
    public static final String SHA_256_ALGORITHM = "SHA-256";
    public static final String CRLF = "\r\n";
    public static final String COMMA = ",";
    public static final String COLON = ":";
    public static final String ZERO = "0";
    public static final String OFF_STATUS = "07";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String EMPTY = "";

    public static final Map<String, String> STATUS_MAP = new HashMap<>(9);
    static {
        STATUS_MAP.put("00", "STOPPED");
        STATUS_MAP.put("01", "TRAY OPEN");
        STATUS_MAP.put("02", "REV PLAYBACK");
        STATUS_MAP.put("05", "CUE PLAYBACK");
        STATUS_MAP.put("06", "SLOW FORWARD PLAYBACK");
        STATUS_MAP.put("07", "POWER OFF");
        STATUS_MAP.put("08", "PLAYBACK");
        STATUS_MAP.put("09", "PAUSE PLAYBACK");
        STATUS_MAP.put("86", "SLOW BACKWARD PLAYBACK");
    }

    // pre-define the POST body for status update calls
    public static final Fields PST_POST_CMD = new Fields();
    static {
        PST_POST_CMD.add("cCMD_PST.x", "100");
        PST_POST_CMD.add("cCMD_PST.y", "100");
    }

    public static final Fields STATUS_POST_CMD = new Fields();
    static {
        STATUS_POST_CMD.add("cCMD_GET_STATUS.x", "100");
        STATUS_POST_CMD.add("cCMD_GET_STATUS.y", "100");
    }

    public static final Fields REVIEW_POST_CMD = new Fields();
    static {
        REVIEW_POST_CMD.add("cCMD_REVIEW.x", "100");
        REVIEW_POST_CMD.add("cCMD_REVIEW.y", "100");
    }

    public static final Fields GET_NONCE_CMD = new Fields();
    static {
        GET_NONCE_CMD.add("SID", "1234ABCD");
    }
}
