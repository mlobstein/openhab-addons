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
 * The {@link LacrosseSensorData} pojo contains the data retrieved from the La Crosse TX60-U sensor
 *
 * @author Michael Lobstein - Initial contribution
 */
public class LacrosseSensorData extends LacrosseDto {

    public LacrosseSensorData() {
    }

    String sensorId;
    String batteryStatus;
    Double tempProbe;
    Integer humidity;
    Double temperature;
    Double heatIndex;
    Double dewPoint;

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public String getBatteryStatus() {
        return batteryStatus;
    }

    public void setBatteryStatus(String batteryStatus) {
        this.batteryStatus = batteryStatus;
    }

    public Double getTempProbe() {
        return tempProbe;
    }

    public void setTempProbe(Double tempProbe) {
        this.tempProbe = tempProbe;
    }

    public Integer getHumidity() {
        return humidity;
    }

    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getHeatIndex() {
        return heatIndex;
    }

    public void setHeatIndex(Double heatIndex) {
        this.heatIndex = heatIndex;
    }

    public Double getDewPoint() {
        return dewPoint;
    }

    public void setDewPoint(Double dewPoint) {
        this.dewPoint = dewPoint;
    }
}
