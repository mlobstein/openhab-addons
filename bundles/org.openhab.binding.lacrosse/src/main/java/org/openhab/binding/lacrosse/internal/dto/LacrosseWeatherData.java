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
package org.openhab.binding.lacrosse.internal.dto;

/**
 * The {@link LacrosseWeatherData} pojo contains the data retrieved from the La Crosse C84612 Weather Station
 *
 * @author Michael Lobstein - Initial contribution
 */
public class LacrosseWeatherData extends LacrosseDto {

    public LacrosseWeatherData() {
    }

    private String recordType;
    private String status;
    private String forecast;
    private Double temperatureIn;
    private Double temperatureOut;
    private Double windChill;
    private Integer humidityIn;
    private Integer humidityOut;
    private Double rainTotal;
    private Double rain;
    private Double windSpeed;
    private Integer windDir;
    private Double gustSpeed;
    private Integer gustDir;
    private Double barometer;

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getForecast() {
        return forecast;
    }

    public void setForecast(String forecast) {
        this.forecast = forecast;
    }

    public Double getTemperatureIn() {
        return temperatureIn;
    }

    public void setTemperatureIn(Double temperatureIn) {
        this.temperatureIn = temperatureIn;
    }

    public Double getTemperatureOut() {
        return temperatureOut;
    }

    public void setTemperatureOut(Double temperatureOut) {
        this.temperatureOut = temperatureOut;
    }

    public Double getWindChill() {
        return windChill;
    }

    public void setWindChill(Double windChill) {
        this.windChill = windChill;
    }

    public Integer getHumidityIn() {
        return humidityIn;
    }

    public void setHumidityIn(Integer humidityIn) {
        this.humidityIn = humidityIn;
    }

    public Integer getHumidityOut() {
        return humidityOut;
    }

    public void setHumidityOut(Integer humidityOut) {
        this.humidityOut = humidityOut;
    }

    public Double getRainTotal() {
        return rainTotal;
    }

    public void setRainTotal(Double rainTotal) {
        this.rainTotal = rainTotal;
    }

    public Double getRain() {
        return rain;
    }

    public void setRain(Double rain) {
        this.rain = rain;
    }

    public Double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(Double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public Integer getWindDir() {
        return windDir;
    }

    public void setWindDir(Integer windDir) {
        this.windDir = windDir;
    }

    public Double getGustSpeed() {
        return gustSpeed;
    }

    public void setGustSpeed(Double gustSpeed) {
        this.gustSpeed = gustSpeed;
    }

    public Integer getGustDir() {
        return gustDir;
    }

    public void setGustDir(Integer gustDir) {
        this.gustDir = gustDir;
    }

    public Double getBarometer() {
        return barometer;
    }

    public void setBarometer(Double barometer) {
        this.barometer = barometer;
    }
}
