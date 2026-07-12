# Panasonic Projector Binding

This binding is compatible with Panasonic projectors via a serial port or USB to serial adapter.
Alternatively, you can connect to your projector via a TCP connection using a serial over IP device or by using`ser2net`.

## Supported Things

This binding supports two thing types based on the connection used: `projector-serial` and `projector-tcp`.

## Discovery

The projector thing cannot be auto-discovered, it has to be configured manually.

## Binding Configuration

There are no overall binding configuration settings that need to be set.
All settings are through thing configuration parameters.

## Thing Configuration

The `projector-serial` thing has the following configuration parameters:

| Parameter       | Name             | Description                                                                                                                                                    | Required |
|-----------------|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| serialPort      | Serial Port      | Serial port device name that is connected to the Panasonic projector to control, e.g. COM1 on Windows, /dev/ttyS0 on Linux or /dev/tty.PL2303-0000103D on Mac. | yes      |
| pollingInterval | Polling Interval | Polling interval in seconds to update channel states, range 5-60 seconds; default 10 seconds.                                                                  | no       |

The `projector-tcp` thing has the following configuration parameters:

| Parameter       | Name             | Description                                                                                    | Required |
|-----------------|------------------|------------------------------------------------------------------------------------------------|----------|
| host            | Host Name        | Host Name or IP address for the serial over IP device.                                         | yes      |
| port            | Port             | Port for the serial over IP device.                                                            | yes      |
| pollingInterval | Polling Interval | Polling interval in seconds to update channel states, range 5-60 seconds; default 10 seconds.  | no       |

Some notes:

* The binding was developed to work with a PT-AE8000U but should support other Panasonic projectors as well.
* The _source_ channel includes a dropdown with the source inputs for a PT-AE8000U.
* If your projector has a source input that is not in the dropdown, the three character code to access that input will be displayed by the _source_ channel when that input is selected by the remote control.
* By using the sitemap mapping or a rule to send the input's code back to the _source_ channel, any source on the projector can be accessed by the binding.
* The channel _picturemode_ is pre-populated with options for the PT-AE8000U

* On Linux, you may get an error stating the serial port cannot be opened when the panasonicprojector binding tries to load.
* You can get around this by adding the `openhab` user to the `dialout` group like this: `usermod -a -G dialout openhab`.
* Also on Linux you may have issues with the USB if using two serial USB devices e.g. panasonicprojector and RFXcom. See the [general documentation about serial port configuration](/docs/administration/serial.html) for more on symlinking the USB ports.
* Here is an example of ser2net.conf you can use to share your serial port /dev/ttyUSB0 on IP port 4444 using [ser2net Linux tool](https://sourceforge.net/projects/ser2net/) (take care, the baud rate is specific to the Panasonic projector):

```
4444:raw:0:/dev/ttyUSB0:9600 8DATABITS NONE 1STOPBIT LOCAL
```

## Channels

| Channel            | Item Type | Purpose                                                                                                   | Values     | 
| ------------------ | --------- | --------------------------------------------------------------------------------------------------------- | ---------- | 
| power              | Switch    | Powers the projector on or off.                                                                           |            | 
| source             | String    | Retrieve or set the input source.                                                                         | See above  | 
| picturemode        | String    | Retrieve or set the picture mode.                                                                         | See above  | 
| freeze             | Switch    | Turn the freeze screen mode on or off.                                                                    |            | 
| blank              | Switch    | Turn the screen blank mode on or off. Do not toggle this switch continuously over a short period of time. |            | 
| button             | String    | Send a key operation command to the projector.                                                            | Write-only | 

## Full Example

things/panasonic.things:

```
// serial port connection
panasonicprojector:projector-serial:hometheater "Projector" [ serialPort="COM5", pollingInterval=10 ]

// serial over IP connection
panasonicprojector:projector-tcp:hometheater "Projector" [ host="192.168.0.10", port=4444, pollingInterval=10 ]

```

items/panasonic.items

```
// Note: replace `-serial` with `-tcp` for `projector-tcp` thing type

Switch panasonicPower                                      { channel="panasonicprojector:projector-serial:hometheater:power" }
String panasonicSource       "Source [%s]"                 { channel="panasonicprojector:projector-serial:hometheater:source" }
String panasonicPictureMode  "Picture Mode [%s]"           { channel="panasonicprojector:projector-serial:hometheater:picturemode" }
Switch panasonicFreeze                                     { channel="panasonicprojector:projector-serial:hometheater:freeze" }
Switch panasonicBlank                                      { channel="panasonicprojector:projector-serial:hometheater:blank" }
String panasonicButton                                     { channel="panasonicprojector:projector-serial:hometheater:button", autoupdate="false" }

```

sitemaps/panasonic.sitemap

```
sitemap panasonic label="Panasonic Projector" {
    Frame label="Controls" {
        Switch     item=panasonicPower  label="Power"
        Selection  item=panasonicSource label="Source" mappings=["HD1"="HDMI1", "HD2"="HDMI2", "HD3"="HDMI3", "CP1"="Component", "RG1"="PC DSUB", "VID"="Video", "SVD"="S-Video"]
        Selection  item=panasonicPictureMode label="Picture Mode"
        Switch     item=panasonicFreeze label="Freeze"
        Switch     item=panasonicBlank  label="Blank Screen"
        Selection  item=panasonicButton label="Button"
    }
    Frame label="Advanced Controls" {
        Switch     item=panasonicButton label="Lens Memory"     mappings=["VXX:LMLI0=+00000"="1","VXX:LMLI0=+00001"="2","VXX:LMLI0=+00002"="3","VXX:LMLI0=+00003"="4","VXX:LMLI0=+00004"="5","VXX:LMLI0=+00005"="6"]
        Switch     item=panasonicButton label="3D Input Format" mappings=["VXX:DIFI1=+00000"="1","VXX:DIFI1=+00001"="2","VXX:DIFI1=+00003"="3","VXX:DIFI1=+00004"="4"]
        Switch     item=panasonicButton label="Trigger 1 Out"   mappings=["VXX:TROI0=+00000"="LOW","VXX:TROI0=+00001"="HIGH"]
        Switch     item=panasonicButton label="Trigger 2 Out"   mappings=["VXX:TROI1=+00000"="LOW","VXX:TROI1=+00001"="HIGH"]
    }
}
```
