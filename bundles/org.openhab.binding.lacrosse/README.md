# La Crosse Binding

This binding receives data from La Crosse C84612 weather stations or TX60-U temperature sensors via the hardwired Ethernet GW1000U gateway.

The GW1000U must be configured to use the IP address and web server port of the openHAB server as its Proxyserver in order to use this binding.
The Gateway Advanced Setup (GAS) utility from La Crosse must be used to change the gateway's settings.

After this setting is changed in the gateway, it will no longer communicate the La Crosse but instead all data transmitted will be re-directed to openHAB.
Note that this also prevents any of these devices used with openHAB from using La Crosse's online services (lacrossealertsmobile.com).

If you have any intention of using La Crosse's alerts service, you should register your station with La Crosse before using this binding.


## Discovery

Discovery is not supported. All things must be added manually.
The LacrosseInterceptorServlet can be accessed at `http://$OPENHAB_IP:8080/request.breq` to veify that it is loaded and view the list of configured devices.

## Supported Things

### C84612 Weather Station

**Thing type ID:** `weatherstation`  
Recieves all measurements from the weather station and its outdoor sensors (wind, themo and rain).
All channels are read-only.

#### Configuration Parameters

| Parameter      | Type   | Required | Description                                                        |
|:---------------|:-------|:---------|:-------------------------------------------------------------------|
| gatewayMac     | text   | yes      | MAC address of the GW1000U gateway (without dashes)                |
| stationSn      | text   | yes      | The serial number (16 chars) of the weather station                |

#### Channels

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

---

### TX60-U Sensor

**Thing type ID:** `sensor`  
Supports up to 5 TX60-U sensors per gateway, each as a channel group.
The sensors display the temperature reading in °F only.
All channels are read-only.

#### Configuration Parameters

| Parameter      | Type   | Required | Description                                                        |
|:---------------|:-------|:---------|:-------------------------------------------------------------------|
| gatewayMac     | text   | yes      | MAC address of the GW1000U gateway (without dashes)                |
| sensor1sn      | text   | yes      | Serial number (16 chars) of the first TX60-U sensor                |
| sensor2sn      | text   | no       | Serial number (16 chars) of the second TX60-U sensor (optional)    |
| sensor3sn      | text   | no       | Serial number (16 chars) of the third TX60-U sensor (optional)     |
| sensor4sn      | text   | no       | Serial number (16 chars) of the fourth TX60-U sensor (optional)    |
| sensor5sn      | text   | no       | Serial number (16 chars) of the fifth TX60-U sensor (optional)     |

#### Channel Groups

Each sensor (1-5) provides the following channels:

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
Thing lacrosse:weatherstation:ws1 "Outdoor Weather Station" [ gatewayMac="AABBCCDDEEFF", stationSn="1234567890ABCDEF" ]

Thing lacrosse:sensor:mysensors "Multi-sensor Array" [ gatewayMac="AABBCCDDEEFF", sensor1sn="1111222233334444", sensor2sn="5555666677778888" ]
```

### `lacrosse.items` Example

```java
// Weather Station Items
Number:Temperature Weather_Indoor_Temp "Indoor Temperature [%.1f %unit%]" { channel="lacrosse:weatherstation:ws1:temperatureIn" }
Number:Dimensionless Weather_Indoor_Humidity "Indoor Humidity [%.0f %unit%]" { channel="lacrosse:weatherstation:ws1:humidityIn" }
Number:Temperature Weather_Outdoor_Temp "Outdoor Temperature [%.1f %unit%]" { channel="lacrosse:weatherstation:ws1:temperatureOut" }
Number:Dimensionless Weather_Outdoor_Humidity "Outdoor Humidity [%.0f %unit%]" { channel="lacrosse:weatherstation:ws1:humidityOut" }
Number:Temperature Weather_Wind_Chill "Wind Chill [%.1f %unit%]" { channel="lacrosse:weatherstation:ws1:windChill" }
Number:Length Weather_Rain_Total "Total Rainfall [%.1f %unit%]" { channel="lacrosse:weatherstation:ws1:rainTotal" }
Number:Length Weather_Rain "Rainfall [%.1f %unit%]" { channel="lacrosse:weatherstation:ws1:rain" }
Number:Angle Weather_Wind_Direction "Wind Direction [%.0f %unit%]" { channel="lacrosse:weatherstation:ws1:windDirection" }
Number:Speed Weather_Wind_Speed "Wind Speed [%.1f %unit%]" { channel="lacrosse:weatherstation:ws1:windSpeed" }
Number:Angle Weather_Gust_Direction "Gust Direction [%.0f %unit%]" { channel="lacrosse:weatherstation:ws1:gustDirection" }
Number:Speed Weather_Gust_Speed "Gust Speed [%.1f %unit%]" { channel="lacrosse:weatherstation:ws1:gustSpeed" }
Number:Pressure Weather_Barometer "Barometric Pressure [%.1f %unit%]" { channel="lacrosse:weatherstation:ws1:barometer" }
Number Weather_RF_Signal "RF Signal Strength [%d]" { channel="lacrosse:weatherstation:ws1:rfSignalStrength" }
String Weather_Status "Status [%s]" { channel="lacrosse:weatherstation:ws1:status" }
String Weather_Forecast "Forecast [%s]" { channel="lacrosse:weatherstation:ws1:forecast" }
DateTime Weather_Last_Seen "Last Seen [%1$tF %1$tT]" { channel="lacrosse:weatherstation:ws1:lastSeenDateTime" }

// TX60-U Sensor 1 Items
Number:Temperature TX60U1_Temperature "Sensor 1 Temperature [%.1f %unit%]" { channel="lacrosse:sensor:mysensors:sensor1#temperature" }
Number:Temperature TX60U1_External_Temperature "Sensor 1 External Temp [%.1f %unit%]" { channel="lacrosse:sensor:mysensors:sensor1#temperatureProbe" }
Number:Dimensionless TX60U1_Humidity "Sensor 1 Humidity [%.0f %unit%]" { channel="lacrosse:sensor:mysensors:sensor1#humidity" }
Number:Temperature TX60U1_Heat_Index "Sensor 1 Heat Index [%.1f %unit%]" { channel="lacrosse:sensor:mysensors:sensor1#heatIndex" }
Number:Temperature TX60U1_Dew_Point "Sensor 1 Dew Point [%.1f %unit%]" { channel="lacrosse:sensor:mysensors:sensor1#dewPoint" }
Number TX60U1_RF_Signal "Sensor 1 RF Signal [%d]" { channel="lacrosse:sensor:mysensors:sensor1#rfSignalStrength" }
String TX60U1_Battery_Status "Sensor 1 Battery [%s]" { channel="lacrosse:sensor:mysensors:sensor1#batteryStatus" }
DateTime TX60U1_Last_Seen "Sensor 1 Last Seen [%1$tF %1$tT]" { channel="lacrosse:sensor:mysensors:sensor1#lastSeenDateTime" }

// TX60-U Sensor 2 Items
Number:Temperature TX60U2_Temperature "Sensor 2 Temperature [%.1f %unit%]" { channel="lacrosse:sensor:mysensors:sensor2#temperature" }
Number:Temperature TX60U2_External_Temperature "Sensor 2 External Temp [%.1f %unit%]" { channel="lacrosse:sensor:mysensors:sensor2#temperatureProbe" }
Number:Dimensionless TX60U2_Humidity "Sensor 2 Humidity [%.0f %unit%]" { channel="lacrosse:sensor:mysensors:sensor2#humidity" }
Number:Temperature TX60U2_Heat_Index "Sensor 2 Heat Index [%.1f %unit%]" { channel="lacrosse:sensor:mysensors:sensor2#heatIndex" }
Number:Temperature TX60U2_Dew_Point "Sensor 2 Dew Point [%.1f %unit%]" { channel="lacrosse:sensor:mysensors:sensor2#dewPoint" }
Number TX60U2_RF_Signal "Sensor 2 RF Signal [%d]" { channel="lacrosse:sensor:mysensors:sensor2#rfSignalStrength" }
String TX60U2_Battery_Status "Sensor 2 Battery [%s]" { channel="lacrosse:sensor:mysensors:sensor2#batteryStatus" }
DateTime TX60U2_Last_Seen "Sensor 2 Last Seen [%1$tF %1$tT]" { channel="lacrosse:sensor:mysensors:sensor2#lastSeenDateTime" }

// Add more TX60-U sensor items as needed
```

### `lacrosse.sitemap` Example

```perl
sitemap lacrosse label="Lacrosse"
{
  Frame label="Weather Station" {
    Text item=Weather_Indoor_Temp
    Text item=Weather_Indoor_Humidity
    Text item=Weather_Outdoor_Temp
    Text item=Weather_Outdoor_Humidity
    Text item=Weather_Wind_Chill
    Text item=Weather_Rain_Total
    Text item=Weather_Rain
    Text item=Weather_Wind_Direction
    Text item=Weather_Wind_Speed
    Text item=Weather_Gust_Direction
    Text item=Weather_Gust_Speed
    Text item=Weather_Barometer
    Text item=Weather_RF_Signal
    Text item=Weather_Status
    Text item=Weather_Forecast
    Text item=Weather_Last_Seen
  }
  Frame label="TX60-U Sensor 1" {
    Text item=TX60U1_Temperature
    Text item=TX60U1_External_Temperature
    Text item=TX60U1_Humidity
    Text item=TX60U1_Heat_Index
    Text item=TX60U1_Dew_Point
    Text item=TX60U1_RF_Signal
    Text item=TX60U1_Battery_Status
    Text item=TX60U1_Last_Seen
  }
  Frame label="TX60-U Sensor 2" {
    Text item=TX60U2_Temperature
    Text item=TX60U2_External_Temperature
    Text item=TX60U2_Humidity
    Text item=TX60U2_Heat_Index
    Text item=TX60U2_Dew_Point
    Text item=TX60U2_RF_Signal
    Text item=TX60U2_Battery_Status
    Text item=TX60U2_Last_Seen
  }
  // Add more TX60-U sensor frames as needed
}
```
