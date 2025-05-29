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
package org.openhab.binding.lacrosse.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link LacrosseBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class LacrosseBindingConstants {

    private static final String BINDING_ID = "lacrosse";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_SENSOR = new ThingTypeUID(BINDING_ID, "sensor");
    public static final ThingTypeUID THING_TYPE_WEATHER_STATION = new ThingTypeUID(BINDING_ID, "weatherstation");

    // List of all Channel ids

    // common
    public static final String RF_SIGNAL_STRENGTH = "rfSignalStrength";
    public static final String LAST_SEEN_DATE_TIME = "lastSeenDateTime";

    // TX60-U sensor channels
    public static final String SENSOR = "sensor";
    public static final String TEMPERATURE = "temperature";
    public static final String TEMPERATURE_PROBE = "temperatureProbe";
    public static final String HUMIDITY = "humidity";
    public static final String HEAT_INDEX = "heatIndex";
    public static final String DEW_POINT = "dewPoint";
    public static final String BATTERY_STATUS = "batteryStatus";

    // C84612 weather station channels
    public static final String TEMPERATURE_IN = "temperatureIn";
    public static final String HUMIDITY_IN = "humidityIn";
    public static final String TEMPERATURE_OUT = "temperatureOut";
    public static final String HUMIDITY_OUT = "humidityOut";
    public static final String WIND_CHILL = "windChill";
    public static final String RAIN_TOTAL = "rainTotal";
    public static final String RAIN = "rain";
    public static final String WIND_DIRECTION = "windDirection";
    public static final String WIND_SPEED = "windSpeed";
    public static final String GUST_DIRECTION = "gustDirection";
    public static final String GUST_SPEED = "gustSpeed";
    public static final String BAROMETER = "barometer";
    public static final String STATUS = "status";
    public static final String FORECAST = "forecast";
}
