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

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lacrosse.internal.dto.LacrosseDto;
import org.openhab.binding.lacrosse.internal.dto.LacrosseSensorData;
import org.openhab.binding.lacrosse.internal.dto.LacrosseWeatherData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LacrosseDataHandler} is responsible for processing the weather and sensor data packets received from the
 * GW1000U ERF gateway
 *
 * The communication routines were adapted from https://github.com/matthewwall/weewx-interceptor and translated from
 * Python using GitHub Copilot.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class LacrosseDataHandler {
    private final Logger logger = LoggerFactory.getLogger(LacrosseDataHandler.class);

    private static final int[] HIST_PKT_LENGS = new int[] { 60, 96, 132, 168, 204, 240, 276, 312, 348, 384, 420 };

    @Nullable
    private Double lastRain = null;

    public Optional<LacrosseDto> processDataPacket(String mac, String pktType, String data) {
        logger.debug("Processing packet-> mac: {}, pktType: {}, data: {}", mac, pktType, data);

        if (data.length() == 394 && "01".equals(data.substring(0, 2))) {
            return Optional.of(processWeatherData(data));
        } else if (data.length() == 46 && "01".equals(data.substring(0, 2))) {
            return Optional.of(processSensorData(pktType.substring(1, 2), data));
        } else if (Arrays.stream(HIST_PKT_LENGS).anyMatch(i -> i == data.length())
                && "21".equals(data.substring(0, 2))) {
            // pkt = self.parse_history(s)
            logger.debug("History data packet processing not implemented");
        } else {
            logger.debug("unhandled data len={} ({})", data.length(), data);
        }
        return Optional.empty();
    }

    private LacrosseWeatherData processWeatherData(String data) {
        final LacrosseWeatherData weatherData = new LacrosseWeatherData();

        // this expects a string of hex characters. the data packet length
        // is 197, so the hex string should be 394 characters.
        weatherData.setRfSignalStrength(Integer.parseInt(data.substring(2, 4), 16)); // %
        weatherData.setStatus(data.substring(4, 6)); // 0x10, 0x20, 0x30
        weatherData.setForecast(data.substring(6, 8)); // 0x11, 0x12, 0x20, 0x21
        weatherData.setTemperatureIn(getTemperature(data, 39)); // C
        weatherData.setTemperatureOut(getTemperature(data, 75)); // C

        // check if wind chill received, 0=ok, 0xa=err
        if (Integer.parseInt(data.substring(114), 16) == 0) {
            weatherData.setWindChill(getTemperature(data, 111)); // C
        }

        weatherData.setHumidityIn(getHum(data, 140, false)); // %
        weatherData.setHumidityOut(getHum(data, 166, false)); // %
        weatherData.setRainTotal(getRainfall(data, 267) / 10.0); // cm
        weatherData.setRain(deltaRain(weatherData.getRainTotal(), lastRain));
        lastRain = weatherData.getRainTotal();

        // check if wind data received, 0=ok, 5=err
        if (Integer.parseInt(data.substring(297), 16) == 0) {
            weatherData.setWindSpeed(getWindspeed(data, 290)); // kph
            weatherData.setWindDir(getWinddir(data, 298)); // degrees
            weatherData.setGustSpeed(getWindspeed(data, 320)); // kph
            weatherData.setGustDir(getWinddir(data, 328)); // degrees
        }
        weatherData.setBarometer(getPressure(data, 339)); // mbar
        weatherData.setLastSeen(new Date());

        return weatherData;
    }

    private LacrosseSensorData processSensorData(String sensorId, String data) {
        final LacrosseSensorData sensorData = new LacrosseSensorData();

        // this expects a string of hex characters. the data packet length
        // is 23, so the hex string should be 46 characters.
        sensorData.setSensorId(sensorId); // 1 thru 5
        sensorData.setRfSignalStrength(Integer.parseInt(data.substring(2, 4), 16)); // %
        sensorData.setBatteryStatus(getBatteryStatus(data.substring(4, 6)));
        sensorData.setTempProbe(getTemperatureTx60(data, 10)); // F
        sensorData.setHumidity(getHum(data, 14, true)); // %
        sensorData.setTemperature(getTemperatureTx60(data, 16)); // F
        sensorData.setHeatIndex(getHeatIndex(sensorData.getTemperature(), sensorData.getHumidity())); // F
        sensorData.setDewPoint(getDewPoint(sensorData.getTemperature(), sensorData.getHumidity())); // F
        sensorData.setLastSeen(new Date());

        return sensorData;
    }

    private String getBatteryStatus(String x) {
        if (("11").equals(x) || ("21").equals(x) || ("31").equals(x)) {
            return "Good";
        } else {
            return "Low";
        }
    }

    // Returns temperature in degree C, or null if input is invalid
    private @Nullable Double getTemperature(String x, int idx) {
        final String s = x.substring(idx, idx + 3);
        if (("aaa").equalsIgnoreCase(s) || ("aa3").equalsIgnoreCase(s) || ("aa6").equalsIgnoreCase(s)
                || ("aa0").equalsIgnoreCase(s)) {
            return null;
        }
        return bcd2int(s) / 10.0 - 40.0;
    }

    // Converts a hexadecimal temperature string from TX60 format to Fahrenheit (rounded to 1 decimal place)
    private @Nullable Double getTemperatureTx60(String x, int idx) {
        final String a = x.substring(idx, idx + 2);
        final String d = x.substring(idx + 2, idx + 4);
        if (("aa").equalsIgnoreCase(a) && ("0a").equalsIgnoreCase(d)) {
            return null;
        }
        final double value = (bin2int(a) + Integer.parseInt(d, 16) / 10.0) - 40.0;
        // Round to 1 decimal place
        return Math.round(value * 10.0) / 10.0;
    }

    // Returns humidity in percent, or null if value is invalid
    private @Nullable Integer getHum(String x, int idx, boolean isBin) {
        final String s = x.substring(idx, idx + 2);
        if (("aa").equalsIgnoreCase(s)) {
            return null;
        }
        if (isBin) {
            return bin2int(s);
        } else {
            return bcd2int(s);
        }
    }

    // Returns the heat index in degrees F, or null if T < 80 or RH < 40
    private @Nullable Double getHeatIndex(double t, double rh) {
        // valid for T>80 F and RH>40%
        if (t < 80 || rh < 40) {
            return null;
        }
        // Constants from NOAA
        final double[] c = { 0, -42.379, 2.04901523, 10.14333127, -0.22475541, -6.83783E-3, -5.481717E-2, 1.22874E-3,
                8.5282E-4, -1.99E-6 };
        final double t2 = Math.pow(t, 2);
        final double rh2 = Math.pow(rh, 2);
        final double hi = c[1] + c[2] * t + c[3] * rh + c[4] * t * rh + c[5] * t2 + c[6] * rh2 + c[7] * t2 * rh
                + c[8] * t * rh2 + c[9] * t2 * rh2;
        return Math.round(hi * 10.0) / 10.0;
    }

    // Returns the dew point in degrees Fahrenheit, rounded to 1 decimal place
    private static Double getDewPoint(double t, double rh) {
        // Constants from wikipedia.org
        final double b = 17.67;
        final double c = 243.5;
        final double gamma = Math.log(rh / 100.0) + ((b * t) / (c + t));
        final double dewPoint = (c * gamma) / (b - gamma);
        return Math.round(dewPoint * 10.0) / 10.0;
    }

    // Returns rainfall total in mm, given a BCD string, starting index, and optional length n (default 7)
    private double getRainfall(String x, int idx) {
        final int n = 7;

        final int v = bcd2int(x.substring(idx, idx + n));
        // if (n == 6) {
        // return v / 100.0;
        // } else {
        // return v / 1000.0;
        // }
        return v / 1000.0;
    }

    // Calculates the delta rainfall, handling wrap-around and null last value
    private @Nullable Double deltaRain(Double rain, @Nullable Double lastRain) {
        if (lastRain == null) {
            logger.debug("skipping rain measurement of {}: no last rain", rain);
            return null;
        }
        if (rain < lastRain) {
            logger.debug("rain counter wraparound detected: new={} last={}", rain, lastRain);
            return rain;
        }
        return rain - lastRain;
    }

    // Returns windspeed in km per hour
    private double getWindspeed(String x, int idx) {
        return bin2int(x.substring(idx, idx + 4)) / 100.0;
    }

    // Returns wind direction in compass degrees [0, 360]
    private int getWinddir(String x, int idx) {
        return (int) (Integer.parseInt(x.substring(idx, idx + 1), 16) * 22.5);
    }

    // Returns barometric pressure in mbar
    private double getPressure(String x, int idx) {
        return bcd2int(x.substring(idx, idx + 5)) / 10.0;
    }

    // Converts a hex string to integer (similar to the Python bin2int)
    private int bin2int(String x) {
        int v = 0;
        for (int i = 0; i < x.length(); i++) {
            v = (v << 4) + Integer.parseInt(String.valueOf(x.charAt(i)), 16);
        }
        return v;
    }

    // Converts BCD-encoded hex string (length 2) to int
    private int bcd2int(String x) {
        final int val = Integer.parseInt(x, 16);
        final int msb = (val >> 4) & 0xF;
        final int lsb = val & 0xF;
        return msb * 10 + lsb;
    }
}
