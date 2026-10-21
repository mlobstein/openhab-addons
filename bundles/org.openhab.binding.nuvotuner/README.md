# Nuvo Tuner Binding

The Nuvo Tuner binding supports Nuvo tuners (T2FAM / T2SIR / T2DAB) and exposes controls for both tuner sections (A and B) through grouped channels.

For tuners configured as source(s) in the [nuvo binding](https://www.openhab.org/addons/bindings/nuvo/), this binding can be used as a companion to allow direct selection of AM and FM frequencies and Sirius channels, which is not possible via NuvoNet tuner control (NuvoNet only supports preset tuning, seek tuning, or step tuning).

To use Sirius capability on a T2SIR, an external SC-H1 or SCHDOC1 must be connected to the tuner's 8-pin Sirius connector for each tuner section (A and B).

## Supported Things

This binding provides one Thing type:

- `tuner` — Nuvo Tuner (T2FAM / T2SIR / T2DAB)

## Discovery

Discovery is not supported by this binding.  
All things must be added manually.

## Thing Configuration

> **Connection requirement:** You must configure **either**
> - `serialPort`, **or**
> - both `host` **and** `port`.

| Pararameter ID | Type               | Required                | Default | Description                                                                                                                                |
|----------------|--------------------|-------------------------|---------|--------------------------------------------------------------------------------------------------------------------------------------------|
| serialPort     | text               | Conditionally required* | (none)  | Serial port used to connect directly to the Nuvo tuner.                                                                                    |
| host           | text               | Conditionally required* | (none)  | Hostname or IP address for a serial-over-IP connection.                                                                                    |
| port           | integer (1..65535) | Conditionally required* | 10000   | Port for serial-over-IP communication.                                                                                                     |
| amFreqMap      | text               | No                      | (none)  | AM frequency-to-description mappings, defined directly in the Thing configuration. Values are exposed as state options for `am-frequency`. |
| fmFreqMap      | text               | No                      | (none)  | FM frequency-to-description mappings, defined directly in the Thing configuration. Values are exposed as state options for `fm-frequency`. |

> \* Conditional requirement:
> - If `serialPort` is set, `host`/`port` are not required.
> - If `serialPort` is not set, both `host` and `port` must be set.

### `amFreqMap` / `fmFreqMap` Format (Thing configuration value)

Mappings are entered directly in the Thing configuration value, with user-defined frequencies in this format:

`frequency=description`

Examples:

- AM: `660=WNBC - News/Talk`
- FM: `102.7=KIIS-FM - Top 40`

When entering multiple mappings, provide one mapping per line.

### Frequency Map Selection Behavior

- The entries defined in `amFreqMap` are made available to the user as **state options** on the AM frequency item (channel `am-frequency`).
- The entries defined in `fmFreqMap` are made available to the user as **state options** on the FM frequency item (channel `fm-frequency`).
- State options include both the frequency and description, displayed in the format `frequency - description` (for example: `660 - WNBC - News/Talk`).
- When the user selects an option, the tuner changes to the selected frequency.

### Sirius Channel Option Behavior

- The `sirius-channel` item contains **state options** for all available Sirius channels.
- Available Sirius channels are loaded automatically by the binding at startup, and can be refreshed for a specific tuner when that tuner's `refresh-channels` switch is triggered.
- Sirius state options are displayed in the format `channel number - description` (for example: `2 - Hits 1`).
- When the user selects a Sirius state option, the tuner changes to the selected Sirius channel.

## Channel Groups

The `tuner` Thing defines two channel groups:

- `tuner_a` (type: `tuner`) — controls for Tuner A
- `tuner_b` (type: `tuner`) — controls for Tuner B

Each group includes the same channel set described below.

## Channels

| Channel Type ID  | Item Type | Read/Write | Description                                                                                                                                                                                                 |
|------------------|-----------|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| active-band      | String    | R          | The currently active band for this tuner.                                                                                                                                                                   |
| sirius-channel   | Number    | RW         | The currently tuned Sirius channel. State options are automatically loaded for available Sirius channels and displayed as `channel number - description`; selecting an option tunes to that Sirius channel. |
| am-frequency     | Number    | RW         | The currently tuned AM frequency. State options are populated from `amFreqMap` and displayed as `frequency - description`; selecting an option tunes the tuner to that frequency.                           |
| fm-frequency     | Number    | RW         | The currently tuned FM frequency. State options are populated from `fmFreqMap` and displayed as `frequency - description`; selecting an option tunes the tuner to that frequency.                           |
| dab-channel      | String    | RW         | The currently tuned DAB channel.                                                                                                                                                                            |
| refresh-channels | Switch    | RW         | Triggers refresh of the Sirius or DAB channel list.                                                                                                                                                         |

## DAB Status (Experimental)

- **DAB functionality is experimental and has not been tested.**
- The `dab-channel` channel is exposed, but behavior may be incomplete or unreliable.
- DAB functionality was implemented based on available documentation and has not been tested with an actual T2DAB tuner.

## Full Example

### `nuvotuner.things` Example

```java
Thing nuvotuner:tuner:house "Nuvo Tuner" [
  host="192.168.1.25",
  port=10000,
  amFreqMap="660=WNBC - News/Talk
720=WGN - News/Talk",
  fmFreqMap="101.1="KRTH - K-Earth 101
102.7=KIIS-FM - Top 40"
]
```

### `nuvotuner.items` Example

```java
// Tuner A
String Nuvo_TunerA_ActiveBand      "Tuner A Active Band [%s]"        { channel="nuvotuner:tuner:house:tuner_a#active-band" }
Number Nuvo_TunerA_SiriusChannel   "Tuner A Sirius Channel [%.0f]"   { channel="nuvotuner:tuner:house:tuner_a#sirius-channel" }
Number Nuvo_TunerA_AMFrequency     "Tuner A AM Frequency [%.0f]"     { channel="nuvotuner:tuner:house:tuner_a#am-frequency" }
Number Nuvo_TunerA_FMFrequency     "Tuner A FM Frequency [%.1f]"     { channel="nuvotuner:tuner:house:tuner_a#fm-frequency" }
String Nuvo_TunerA_DABChannel      "Tuner A DAB Channel [%s]"        { channel="nuvotuner:tuner:house:tuner_a#dab-channel" }
Switch Nuvo_TunerA_RefreshChannels "Tuner A Refresh Channels"        { channel="nuvotuner:tuner:house:tuner_a#refresh-channels" }

// Tuner B
String Nuvo_TunerB_ActiveBand      "Tuner B Active Band [%s]"        { channel="nuvotuner:tuner:house:tuner_b#active-band" }
Number Nuvo_TunerB_SiriusChannel   "Tuner B Sirius Channel [%.0f]"   { channel="nuvotuner:tuner:house:tuner_b#sirius-channel" }
Number Nuvo_TunerB_AMFrequency     "Tuner B AM Frequency [%.0f]"     { channel="nuvotuner:tuner:house:tuner_b#am-frequency" }
Number Nuvo_TunerB_FMFrequency     "Tuner B FM Frequency [%.1f]"     { channel="nuvotuner:tuner:house:tuner_b#fm-frequency" }
String Nuvo_TunerB_DABChannel      "Tuner B DAB Channel [%s]"        { channel="nuvotuner:tuner:house:tuner_b#dab-channel" }
Switch Nuvo_TunerB_RefreshChannels "Tuner B Refresh Channels"        { channel="nuvotuner:tuner:house:tuner_b#refresh-channels" }
```

### `nuvotuner.sitemap` Example

```perl
sitemap nuvotuner label="Nuvo Tuner" {
    Frame label="Tuner A" {
        Text item=Nuvo_TunerA_ActiveBand
        Selection item=Nuvo_TunerA_SiriusChannel
        Selection item=Nuvo_TunerA_AMFrequency
        Selection item=Nuvo_TunerA_FMFrequency
        Text item=Nuvo_TunerA_DABChannel
        Switch item=Nuvo_TunerA_RefreshChannels mappings=[ON="Refresh"]
    }

    Frame label="Tuner B" {
        Text item=Nuvo_TunerB_ActiveBand
        Selection item=Nuvo_TunerB_SiriusChannel
        Selection item=Nuvo_TunerB_AMFrequency
        Selection item=Nuvo_TunerB_FMFrequency
        Text item=Nuvo_TunerB_DABChannel
        Switch item=Nuvo_TunerB_RefreshChannels mappings=[ON="Refresh"]
    }
}
```

## Troubleshooting

Enable debug logging for the binding:

```shell
log:set DEBUG org.openhab.binding.nuvotuner
```

Then review `openhab.log` for connection and command handling details.
