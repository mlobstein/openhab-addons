# Panasonic Blu-ray Player Binding

This binding connects Panasonic Blu-ray players from 2011/2012 and UHD Blu-ray players from 2018 to openHAB.  
Supported Blu-ray models: DMP-BDT110, DMP-BDT210, DMP-BDT310, DMP-BDT120, DMP-BDT220, DMP-BDT320, DMP-BBT01 & DMP-BDT500.  
Supported UHD models: DP-UB420, DP-UB820 & DP-UB9000.  

The control protocol of these players is undocumented and all understanding of how the protocol works was learned from Internet forum posts and observing network traffic between the player and its iOS/android apps.
Currently the binding can send remote control commands to the player and retrieve some basic playback status information.
It is possible that more functionality could be added in the future if additional information about the protocol is discovered.

It is understood the reason that players released after 2012 no longer had the http control protocol was that Panasonic outsourced its firmware development to another entity.
It is believed that they resurrected some of their in-house technology for the UHD player firmware and with it the 2011/2012 http control protocol was again implemented on the 2018 UHD player line.

To enable network remote control of the player, configure the following player settings:  
**Player settings/Network/Network Settings/Remote Device Settings**  
Then make sure you have the following values set:  
**Remote Device Operation: On**  
**Registration Type: Automatic**  

## Supported Things

There are two supported thing types, which represent either a BD player or a UHD player.
A supported Blu-ray uses the `bd_player` id and a supported UHD Blu-ray player uses the `uhd_player` id.
Multiple Things can be added if more than one player is to be controlled.

## Discovery

Auto-discovery is supported if the player can be located on the local network using UPNP.
Otherwise the thing must be manually added.

## Binding Configuration

The binding has no configuration options, all configuration is done at Thing level.

## Thing Configuration

The thing has a few configuration parameters:

| Parameter | Description                                                                                           |
|-----------|-------------------------------------------------------------------------------------------------------|
| hostName  | The host name or IP address of the Blu-ray player. Mandatory.                                         |
| refresh   | Overrides the refresh interval of the player status. Optional, the default and minimum is 10 seconds. |
| playerKey | For UHD players, to enable authentication of control commands, a key for the player must be specified.|

Some notes:

* Not all commands work on all players (ie: Power does not work on DMP-BDT110)
* Not all status information is available from all players (ie: playback elapsed time not reported by some players)
* On some players the time and chapter information is only available when playing a Blu-ray disc (not DVD or CD)
* The openHAB server must be on the same IP subnet as the player (404 response errors are sent if on a different subnet)

**List of available button commands for BD players:**  

Button: (Command)  
Power Toggle:  RC_POWER  
Open/Close:  RC_OP_CL  
1 (@.):  RC_D1  
2 (ABC):  RC_D2  
3 (DEF):  RC_D3  
4 (GHI):  RC_D4  
5 (JKL):  RC_D5  
6 (MNO):  RC_D6  
7 (PQRS):  RC_D7  
8 (TUV):  RC_D8  
9 (WXYZ):  RC_D9  
0 (-,):  RC_D0  
12:     RC_D12  
&#42; (Cancel):  RC_CLEAR  
&#35; ([_]):  RC_SHARP  
Skype:  RC_SKYPE  
3D:  RC_3D  
Audio:  RC_AUDIOSEL  
Netflix:     RC_NETFLIX  
Network:     RC_NETWORK  
Fast Fwd:  RC_CUE
Reverse:  RC_REV  
Play:  RC_PLAYBACK  
Skip Fwd:  RC_SKIPFWD  
Skip Back:  RC_SKIPREV  
Pause:  RC_PAUSE  
Stop:  RC_STOP  
Status:  RC_DSPSEL  
Exit:  RC_EXIT  
Pop-Up Menu (Top Menu):  RC_TITLE  
Pop-Up Menu:  RC_PUPMENU  
VIERA Cast:  RC_V_CAST  
Home:  RC_MLTNAV  
Up:  RC_UP  
Down:  RC_DOWN  
Left:  RC_LEFT  
Right:  RC_RIGHT  
OK:  RC_SELECT  
Submenu:  RC_MENU  
Return:  RC_RETURN  
Red:  RC_RED  
Green:  RC_GREEN  
Blue:  RC_BLUE  
Yellow:  RC_YELLOW  

Playback View (buttons not in other views):  
PIP:  RC_P_IN_P  
OSD (DISPLAY):  RC_OSDONOFF  

Shuttle(BD) View (buttons not in other views):  
(swipe in CW circle):  RC_SHFWD2  
(swipe in CCW circle):  RC_SHREV2  

**List of available button commands for UHD players:**  

Button: (Command)  
Power ON:  RC_POWERON  
Power Off:  RC_POWEROFF  
Power Toggle:  RC_POWER  
Play:  RC_PLAYBACK  
Pause:  RC_PAUSE  
Stop:  RC_STOP  
Fast Fwd:  RC_CUE  
Reverse:  RC_REV  
Skip Fwd:  RC_SKIPFWD  
Skip Back:  RC_SKIPREV  
Manual Skip +60s:  RC_MNSKIP  
Manual Skip -10s:  RC_MNBACK  
Open/Close:  RC_OP_CL  
Status:  RC_DSPSEL  
Top Menu:  RC_TITLE  
Pop-up Menu:  RC_PUPMENU  
Up:  RC_UP  
Down:  RC_DOWN  
Left:  RC_LEFT  
Right:  RC_RIGHT  
OK:  RC_SELECT  
Submenu:  RC_MENU  
Return:  RC_RETURN  
1:  RC_D1  
2:  RC_D2  
3:  RC_D3  
4:  RC_D4  
5:  RC_D5  
6:  RC_D6  
7:  RC_D7  
8:  RC_D8  
9:  RC_D9  
0:  RC_D0  
12:  RC_D12  
&#42; (Cancel):  RC_CLEAR  
&#35; ([_]):  RC_SHARP  
Red:  RC_RED  
Green:  RC_GREEN  
Blue:  RC_BLUE  
Yellow:  RC_YELLOW  
Home:  RC_MLTNAV  
Netflix:  RC_NETFLIX  
Internet:  RC_V_CAST  
Setup:  RC_SETUP  
Exit:  RC_EXIT  
Audio:  RC_AUDIOSEL  
Subtitle:  RC_TITLEONOFF  
Closed Caption:  RC_CLOSED_CAPTION  
Playback Info:  RC_PLAYBACKINFO  
HDR Picture Mode:  RC_HDR_PICTUREMODE  
Mirroring:  RC_MIRACAST  
Picture Setting:  RC_PICTURESETTINGS  
Sound Effect:  RC_SOUNDEFFECT  
High Clarity:  RC_HIGHCLARITY  

## Channels

The following channels are available:

| Channel ID      | Item Type   | Description                                                                            |
|-----------------|-------------|----------------------------------------------------------------------------------------|
| button          | String      | Sends a remote command to control the player. See list of available commands above.    |
| play_mode       | String      | The current playback mode ie: STOP, PLAY, PAUSE (ReadOnly)                             |
| time_elapsed    | Number:Time | The total number of seconds of playback time elapsed (ReadOnly)                        |
| time_total      | Number:Time | The total length of the current playing title in seconds (ReadOnly) Not on UHD players |
| chapter_current | Number      | The current chapter number (ReadOnly) Not on UHD players                               |
| chapter_total   | Number      | The total number of chapters in the current title (ReadOnly) Not on UHD players        |

## Full Example

panasonicbr.things:

```java
panasonicbr:bd_player:myplayer1 "My Blu-ray player" [ hostName="192.168.10.1", refresh=10 ]
panasonicbr:uhd_player:myplayer1 "My UHD Blu-ray player" [ hostName="192.168.10.1", refresh=10, playerKey="ABCDEF1234567890abcdef0123456789" ]
```

panasonicbr.items:

```java
String Player_Button            "Send Command to Player"    { channel="panasonicbr:player:myplayer1:button" }
String Player_Play_Mode         "Status: [%s]"              { channel="panasonicbr:player:myplayer1:play_mode" }
Number:Time Player_Time_Elapsed "Elapsed Time: [%d %unit%]" { channel="panasonicbr:player:myplayer1:time_elapsed" }
Number:Time Player_Time_Total "Total Time: [%d %unit%]" { channel="panasonicbr:player:myplayer1:time_total" }
Number Player_Chapter_Current "Current Chapter: [%d]" { channel="panasonicbr:player:myplayer1:chapter_current" }
Number Player_Chapter_Total "Total Chapters: [%d]" { channel="panasonicbr:player:myplayer1:chapter_total" }

```

panasonicbr.sitemap:

```perl
sitemap panasonicbr label="Panasonic" {
    Frame label="My Blu-ray Player" {
        Selection item=Player_Button
        Text item=Player_Play_Mode
        Text item=Player_Time_Elapsed
        Text item=Player_Time_Total
        Text item=Player_Chapter_Current
        Text item=Player_Chapter_Total
    }
}
```
