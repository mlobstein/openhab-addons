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

import static org.openhab.binding.lacrosse.internal.LacrosseBindingConstants.*;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lacrosse.internal.dto.LacrosseDto;
import org.openhab.binding.lacrosse.internal.dto.LacrosseSensorData;
import org.openhab.binding.lacrosse.internal.dto.LacrosseWeatherData;
import org.openhab.binding.lacrosse.internal.interceptor.LacrosseDataHandler;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.UnDefType;

/**
 * The {@link LacrosseHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class LacrosseHandler extends BaseThingHandler {

    private @Nullable LacrosseConfiguration config;
    private LacrosseDataHandler dataHandler;
    private String gatewaySn = "";
    private SimpleEntry<String, ?> configMap = new AbstractMap.SimpleEntry<String, String>("", "");

    public LacrosseHandler(Thing thing) {
        super(thing);
        dataHandler = new LacrosseDataHandler();
    }

    @Override
    public void initialize() {
        config = getConfigAs(LacrosseConfiguration.class);
        final LacrosseConfiguration configLocal = config;

        if (configLocal != null) {
            this.gatewaySn = configLocal.gatewaySn;

            if (THING_TYPE_WEATHER_STATION.equals(this.getThing().getThingTypeUID())) {
                this.configMap = new AbstractMap.SimpleEntry<String, String>(configLocal.gatewaySn,
                        configLocal.stationSn);
            } else if (THING_TYPE_SENSOR.equals(this.getThing().getThingTypeUID())) {
                this.configMap = new AbstractMap.SimpleEntry<>(configLocal.gatewaySn, List.of(configLocal.sensor1sn,
                        configLocal.sensor2sn, configLocal.sensor3sn, configLocal.sensor4sn, configLocal.sensor5sn));
            }
        }

        updateStatus(ThingStatus.UNKNOWN);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Do nothing - all channels are read-only
    }

    public SimpleEntry<String, ?> getConfigMap() {
        return this.configMap;
    }

    public void handlePing(String gatewaySn) {
        // ignore packets intended for other Lacrosse things
        if (!this.gatewaySn.equals(gatewaySn)) {
            return;
        }
        // Any pings received for this Thing's stationSn causes it to go online.
        // We do this so the Thing goes online quicker as the data packets arrive less often.
        updateStatus(ThingStatus.ONLINE);
    }

    public void handleDataPacket(String gatewaySn, String pktType, String data) {
        // ignore packets intended for other Lacrosse things
        if (!this.gatewaySn.equals(gatewaySn)) {
            return;
        }

        final Optional<LacrosseDto> dataDto = dataHandler.processDataPacket(gatewaySn, pktType, data);
        // ignore packets that could not be parsed
        if (!dataDto.isPresent()) {
            return;
        } else if (dataDto.get() instanceof LacrosseWeatherData) {
            final LacrosseWeatherData weatherData = (LacrosseWeatherData) dataDto.get();

            doUpdate(TEMPERATURE_IN, weatherData.getTemperatureIn(), SIUnits.CELSIUS);
            doUpdate(HUMIDITY_IN, weatherData.getHumidityIn(), Units.PERCENT);
            doUpdate(TEMPERATURE_OUT, weatherData.getTemperatureOut(), SIUnits.CELSIUS);
            doUpdate(HUMIDITY_OUT, weatherData.getHumidityOut(), Units.PERCENT);
            doUpdate(WIND_CHILL, weatherData.getWindChill(), SIUnits.CELSIUS);
            doUpdate(RAIN_TOTAL, weatherData.getRainTotal(), MetricPrefix.CENTI(SIUnits.METRE));
            doUpdate(RAIN, weatherData.getRain(), MetricPrefix.CENTI(SIUnits.METRE));
            doUpdate(WIND_DIRECTION, weatherData.getWindDir(), Units.DEGREE_ANGLE);
            doUpdate(WIND_SPEED, weatherData.getWindSpeed(), SIUnits.KILOMETRE_PER_HOUR);
            doUpdate(GUST_DIRECTION, weatherData.getGustDir(), Units.DEGREE_ANGLE);
            doUpdate(GUST_SPEED, weatherData.getGustSpeed(), SIUnits.KILOMETRE_PER_HOUR);
            doUpdate(BAROMETER, weatherData.getBarometer(), Units.MILLIBAR);
            doUpdate(STATUS, weatherData.getStatus());
            doUpdate(FORECAST, weatherData.getForecast());
            doUpdate(RF_SIGNAL_STRENGTH, weatherData.getRfSignalStrength(), Units.PERCENT);
            doUpdate(LAST_SEEN_DATE_TIME, weatherData.getLastSeen());
        } else if (dataDto.get() instanceof LacrosseSensorData) {
            final LacrosseSensorData sensorData = (LacrosseSensorData) dataDto.get();

            doUpdate(getChannel(sensorData, TEMPERATURE), sensorData.getTemperature(), ImperialUnits.FAHRENHEIT);
            doUpdate(getChannel(sensorData, TEMPERATURE_PROBE), sensorData.getTempProbe(), ImperialUnits.FAHRENHEIT);
            doUpdate(getChannel(sensorData, HUMIDITY), sensorData.getHumidity(), Units.PERCENT);
            doUpdate(getChannel(sensorData, HEAT_INDEX), sensorData.getHeatIndex(), ImperialUnits.FAHRENHEIT);
            doUpdate(getChannel(sensorData, DEW_POINT), sensorData.getDewPoint(), ImperialUnits.FAHRENHEIT);
            doUpdate(getChannel(sensorData, BATTERY_STATUS), sensorData.getBatteryStatus());
            doUpdate(getChannel(sensorData, RF_SIGNAL_STRENGTH), sensorData.getRfSignalStrength(), Units.PERCENT);
            doUpdate(getChannel(sensorData, LAST_SEEN_DATE_TIME), sensorData.getLastSeen());
        }

        // A packet processed by this Thing will also cause it to go online
        updateStatus(ThingStatus.ONLINE);
    }

    // Gets the group channel id based on the sensor id and channel name, i.e. 'sensor1#temperature'
    private String getChannel(LacrosseSensorData sensorData, String channel) {
        return SENSOR + sensorData.getSensorId() + "#" + channel;
    }

    // Convenience methods to abstract calling updateState() and sends UNDEF state for null values
    private void doUpdate(String channel, @Nullable Double data, Unit<?> unit) {
        updateState(channel, data == null ? UnDefType.UNDEF : new QuantityType<>(data, unit));
    }

    private void doUpdate(String channel, @Nullable Integer data, Unit<?> unit) {
        updateState(channel, data == null ? UnDefType.UNDEF : new QuantityType<>(data, unit));
    }

    private void doUpdate(String channel, @Nullable String data) {
        updateState(channel, data == null ? UnDefType.UNDEF : new StringType(data));
    }

    private void doUpdate(String channel, @Nullable Date data) {
        updateState(channel, data == null ? UnDefType.UNDEF : new DateTimeType(data.toInstant()));
    }
}
