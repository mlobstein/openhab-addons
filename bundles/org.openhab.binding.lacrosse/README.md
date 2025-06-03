# La Crosse Binding

This binding receives data from La Crosse C84612 weather stations or TX60-U temperature sensors via the hardwired Ethernet GW1000U ERF gateway.

**Please note that this binding is still under development and may not work correctly or at all!**
**The La Crosse C84612 weather station has not yet been tested with this binding.**

The GW1000U must be configured to use the IP address and web server port of the openHAB server as its Proxyserver in order to use this binding.
The Gateway Advanced Setup (GAS) utility from La Crosse is used to change the gateway's settings.

After this setting is changed in the gateway all data transmitted will be re-directed to openHAB.
Note that this also prevents any of these devices used with openHAB from using La Crosse's online services (lacrossealertsmobile.com).

If you have any intention of using La Crosse's alerts service, you should register your weather station with La Crosse before using this binding.

The routines used to allow communication with the gateway were adapted from [weewx-interceptor](https://github.com/matthewwall/weewx-interceptor) and translated into Java using GitHub Copilot.


Most of the knowledge on how the GW1000U communicates was originally discussed here: <https://www.wxforum.net/index.php?topic=14299.0>

## Discovery

Discovery is not supported. All things must be added manually.

The LacrosseInterceptorServlet can be accessed at `http://$OPENHAB_IP:8080/request.breq` to verify that it is active.

Any gateways that are configured to connect to openHAB will be displayed here along with the serial number that is needed for the Thing configuration.

The easiest way to obtain the serial number for the C84612 weather station is to register it with La Crosse Alerts and then view the serial number on their Device Settings page.

The serial number for a TX60-U sensor can be found on the sticker on the back of the sensor.

Once the things are configured properly, they will change to ONLINE status upon receipt of data from the gateway. Note however the status will never change to OFFLINE.

## Supported Things

### C84612 Weather Station

**Thing type ID:** `weatherstation`  
Receives all measurements from the weather station and its outdoor sensors (Wind, Rain and Temp).
The weather station cannot be used along with TX60-U sensors on a single GW1000U gateway.
Using the weather station and TX60-U sensors requires at least 2 gateways.

#### Thing Configuration

| Parameter      | Type   | Required | Description                                                                     |
|----------------|--------|----------|---------------------------------------------------------------------------------|
| gatewaySn      | text   | yes      | The serial number (8 chars) of the GW1000U gateway hosting the weather station  |
| stationSn      | text   | yes      | The serial number (16 chars) of the weather station                             |

#### Channels

The C84612 Weather Station has the following channels (All channels are read-only):

| Channel ID        | Label                | Item Type             | Description                                       |
|-------------------|----------------------|-----------------------|---------------------------------------------------|
| temperatureIn     | Indoor Temperature   | Number:Temperature    | Measured indoor temperature (°C)                  |
| humidityIn        | Indoor Humidity      | Number:Dimensionless  | Measured indoor humidity (%)                      |
| temperatureOut    | Outdoor Temperature  | Number:Temperature    | Measured outdoor temperature (°C)                 |
| humidityOut       | Outdoor Humidity     | Number:Dimensionless  | Measured outdoor humidity (%)                     |
| windChill         | Wind Chill           | Number:Temperature    | Calculated wind chill temperature (°C)            |
| rainTotal         | Total Rainfall       | Number:Length         | Total rainfall measured (cm)                      |
| rain              | Rainfall             | Number:Length         | Rainfall over the latest period  (cm)             |
| windDirection     | Wind Direction       | Number:Angle          | Current wind direction in degrees (1-360)         |
| windSpeed         | Wind Speed           | Number:Speed          | Current wind speed (km/h)                         |
| gustDirection     | Gust Direction       | Number:Angle          | Current gust direction in degrees (1-360)         |
| gustSpeed         | Gust Speed           | Number:Speed          | Current wind gust speed (km/h)                    |
| barometer         | Barometric Pressure  | Number:Pressure       | Current barometric pressure (mbar)                |
| rfSignalStrength  | RF Signal Strength   | Number                | RF signal strength as received at gateway (1-100) |
| status            | Status               | String                | Status of the weather station??                   |
| forecast          | Forecast             | String                | Weather forecast??                                |
| lastSeenDateTime  | Last Seen            | DateTime              | Last time data was received                       |

### TX60-U Sensor

**Thing type ID:** `sensor`  
Supports up to 5 TX60-U sensors per gateway, each as a channel group.

The sensors display the temperature reading in °F only.

If the sensors were previously connected to the gateway, the sensor serial number of each sensor must be entered into the same number configuration parameter as currently registered in the gateway.

#### Thing Configuration

| Parameter      | Type   | Required | Description                                                            |
|----------------|--------|----------|------------------------------------------------------------------------|
| gatewaySn      | text   | yes      | The serial number (8 chars) of the GW1000U gateway hosting the sensors |
| sensor1sn      | text   | yes      | Serial number (16 chars) of the first TX60-U sensor                    |
| sensor2sn      | text   | no       | Serial number (16 chars) of the second TX60-U sensor (optional)        |
| sensor3sn      | text   | no       | Serial number (16 chars) of the third TX60-U sensor (optional)         |
| sensor4sn      | text   | no       | Serial number (16 chars) of the fourth TX60-U sensor (optional)        |
| sensor5sn      | text   | no       | Serial number (16 chars) of the fifth TX60-U sensor (optional)         |

#### Channel Groups

Each sensor (1-5) provides the following channels (All channels are read-only):

| Channel ID         | Label               | Item Type             | Description                                      |
|--------------------|---------------------|-----------------------|--------------------------------------------------|
| temperature        | Temperature         | Number:Temperature    | Temperature measured at the sensor (°F)          |
| temperatureProbe   | External Temperature| Number:Temperature    | Temperature measured by the external probe (°F)  |
| humidity           | Humidity            | Number:Dimensionless  | Humidity measured at the sensor (%)              |
| heatIndex          | Heat Index          | Number:Temperature    | Calculated heat index (°F)                       |
| dewPoint           | Dew Point           | Number:Temperature    | Calculated dew point (°F)                        |
| rfSignalStrength   | RF Signal Strength  | Number                | RF signal strength as received at gateway (1-100)|
| batteryStatus      | Battery Status      | String                | Battery status (OK or Low)                       |
| lastSeenDateTime   | Last Seen DateTime  | DateTime              | Last time data was received from the sensor      |

## Full Example

### `lacrosse.things` Example

```java
lacrosse:weatherstation:ws1 "Weather Station" [ gatewaySn="AABBCCDD", stationSn="1234567890ABCDEF" ]

lacrosse:sensor:mysensors "Temperature Sensors" [ gatewaySn="AABBCCDD", sensor1sn="1111222233334444", sensor2sn="5555666677778888" ]
```

### `lacrosse.items` Example

Note that the `expire` directives will cause the state of the items to change to UNDEF if no updates are received for more than 45 minutes.

```java
// Weather Station Items
Number:Temperature Weather_Indoor_Temp "Indoor Temperature [%.1f %unit%]" <temperature> { channel="lacrosse:weatherstation:ws1:temperatureIn", expire="45m" }
Number:Dimensionless Weather_Indoor_Humidity "Indoor Humidity [%d %%]" <humidity> { channel="lacrosse:weatherstation:ws1:humidityIn", expire="45m" }
Number:Temperature Weather_Outdoor_Temp "Outdoor Temperature [%.1f %unit%]" <temperature> { channel="lacrosse:weatherstation:ws1:temperatureOut", expire="45m" }
Number:Dimensionless Weather_Outdoor_Humidity "Outdoor Humidity [%d %%]" <humidity> { channel="lacrosse:weatherstation:ws1:humidityOut", expire="45m" }
Number:Temperature Weather_Wind_Chill "Wind Chill [%.1f %unit%]" { channel="lacrosse:weatherstation:ws1:windChill", expire="45m" }
Number:Length Weather_Rain_Total "Total Rainfall [%.1f %unit%]" { channel="lacrosse:weatherstation:ws1:rainTotal", expire="45m" }
Number:Length Weather_Rain "Rainfall [%.1f %unit%]" { channel="lacrosse:weatherstation:ws1:rain", expire="45m" }
Number:Angle Weather_Wind_Direction "Wind Direction [%d %unit%]" { channel="lacrosse:weatherstation:ws1:windDirection", expire="45m" }
Number:Speed Weather_Wind_Speed "Wind Speed [%.1f %unit%]" { channel="lacrosse:weatherstation:ws1:windSpeed", expire="45m" }
Number:Angle Weather_Gust_Direction "Gust Direction [%d %unit%]" { channel="lacrosse:weatherstation:ws1:gustDirection", expire="45m" }
Number:Speed Weather_Gust_Speed "Gust Speed [%.1f %unit%]" { channel="lacrosse:weatherstation:ws1:gustSpeed", expire="45m" }
Number:Pressure Weather_Barometer "Barometric Pressure [%.1f %unit%]" { channel="lacrosse:weatherstation:ws1:barometer", expire="45m" }
Number Weather_RF_Signal "RF Signal Strength [%d %%]" { channel="lacrosse:weatherstation:ws1:rfSignalStrength", expire="45m" }
String Weather_Status "Status [%s]" { channel="lacrosse:weatherstation:ws1:status", expire="45m" }
String Weather_Forecast "Forecast [%s]" { channel="lacrosse:weatherstation:ws1:forecast", expire="45m" }
DateTime Weather_Last_Seen "Last Seen [%1$tA, %1$tm-%1$td-%1$tY %1$tl:%1$tM %1$tp]" { channel="lacrosse:weatherstation:ws1:lastSeenDateTime" }

// TX60-U Sensor 1 Items
Number:Temperature TX60U1_Temperature "Sensor 1 Temperature [%.1f %unit%]" <temperature> { channel="lacrosse:sensor:mysensors:sensor1#temperature", expire="45m" }
Number:Temperature TX60U1_External_Temperature "Sensor 1 External Temp [%.1f %unit%]" <temperature> { channel="lacrosse:sensor:mysensors:sensor1#temperatureProbe", expire="45m" }
Number:Dimensionless TX60U1_Humidity "Sensor 1 Humidity [%d %%]" <humidity> { channel="lacrosse:sensor:mysensors:sensor1#humidity", expire="45m" }
Number:Temperature TX60U1_Heat_Index "Sensor 1 Heat Index [%.1f %unit%]" { channel="lacrosse:sensor:mysensors:sensor1#heatIndex", expire="45m" }
Number:Temperature TX60U1_Dew_Point "Sensor 1 Dew Point [%.1f %unit%]" { channel="lacrosse:sensor:mysensors:sensor1#dewPoint", expire="45m" }
Number TX60U1_RF_Signal "Sensor 1 RF Signal [%d %%]" { channel="lacrosse:sensor:mysensors:sensor1#rfSignalStrength", expire="45m" }
String TX60U1_Battery_Status "Sensor 1 Battery [%s]" { channel="lacrosse:sensor:mysensors:sensor1#batteryStatus" }
DateTime TX60U1_Last_Seen "Sensor 1 Last Seen [%1$tA, %1$tm-%1$td-%1$tY %1$tl:%1$tM %1$tp]" { channel="lacrosse:sensor:mysensors:sensor1#lastSeenDateTime" }

// TX60-U Sensor 2 Items
Number:Temperature TX60U2_Temperature "Sensor 2 Temperature [%.1f %unit%]" <temperature> { channel="lacrosse:sensor:mysensors:sensor2#temperature", expire="45m" }
Number:Temperature TX60U2_External_Temperature "Sensor 2 External Temp [%.1f %unit%]" <temperature> { channel="lacrosse:sensor:mysensors:sensor2#temperatureProbe", expire="45m" }
Number:Dimensionless TX60U2_Humidity "Sensor 2 Humidity [%d %%]" <humidity> { channel="lacrosse:sensor:mysensors:sensor2#humidity", expire="45m" }
Number:Temperature TX60U2_Heat_Index "Sensor 2 Heat Index [%.1f %unit%]" { channel="lacrosse:sensor:mysensors:sensor2#heatIndex", expire="45m" }
Number:Temperature TX60U2_Dew_Point "Sensor 2 Dew Point [%.1f %unit%]" { channel="lacrosse:sensor:mysensors:sensor2#dewPoint", expire="45m" }
Number TX60U2_RF_Signal "Sensor 2 RF Signal [%d %%]" { channel="lacrosse:sensor:mysensors:sensor2#rfSignalStrength", expire="45m" }
String TX60U2_Battery_Status "Sensor 2 Battery [%s]" { channel="lacrosse:sensor:mysensors:sensor2#batteryStatus" }
DateTime TX60U2_Last_Seen "Sensor 2 Last Seen [%1$tA, %1$tm-%1$td-%1$tY %1$tl:%1$tM %1$tp]" { channel="lacrosse:sensor:mysensors:sensor2#lastSeenDateTime" }

// Add more TX60-U sensor items as needed
```

### `lacrosse.sitemap` Example

```perl
sitemap lacrosse label="La Crosse"
{
  Frame label="Weather Station" {
    Text item=Weather_Indoor_Temp icon="temperature"
    Text item=Weather_Indoor_Humidity icon="humidity"
    Text item=Weather_Outdoor_Temp icon="temperature"
    Text item=Weather_Outdoor_Humidity icon="humidity"
    Text item=Weather_Wind_Chill icon="temperature_cold"
    Text item=Weather_Rain_Total icon="rain"
    Text item=Weather_Rain icon="rain"
    Text item=Weather_Wind_Direction icon="wind"
    Text item=Weather_Wind_Speed icon="wind"
    Text item=Weather_Gust_Direction icon="wind"
    Text item=Weather_Gust_Speed icon="wind"
    Text item=Weather_Barometer icon="pressure"
    Text item=Weather_RF_Signal icon="qualityofservice"
    Text item=Weather_Status
    Text item=Weather_Forecast
    Text item=Weather_Last_Seen icon="time"
  }
  Frame label="TX60-U Sensor 1" {
    Text item=TX60U1_Temperature icon="temperature"
    Text item=TX60U1_External_Temperature icon="temperature"
    Text item=TX60U1_Humidity icon="humidity"
    Text item=TX60U1_Heat_Index icon="temperature_hot"
    Text item=TX60U1_Dew_Point icon="temperature"
    Text item=TX60U1_RF_Signal icon="qualityofservice"
    Text item=TX60U1_Battery_Status icon="batterylevel"
    Text item=TX60U1_Last_Seen icon="time"
  }
  Frame label="TX60-U Sensor 2" {
    Text item=TX60U2_Temperature icon="temperature"
    Text item=TX60U2_External_Temperature icon="temperature"
    Text item=TX60U2_Humidity icon="humidity"
    Text item=TX60U2_Heat_Index icon="temperature_hot"
    Text item=TX60U2_Dew_Point icon="temperature"
    Text item=TX60U2_RF_Signal icon="qualityofservice"
    Text item=TX60U2_Battery_Status icon="batterylevel"
    Text item=TX60U2_Last_Seen icon="time"
  }
  // Add more TX60-U sensor frames as needed
}
```
