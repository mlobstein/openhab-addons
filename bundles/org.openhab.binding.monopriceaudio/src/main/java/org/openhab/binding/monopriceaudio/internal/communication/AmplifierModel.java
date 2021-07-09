/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.monopriceaudio.internal.communication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.monopriceaudio.internal.configuration.MonopriceAudioThingConfiguration;
import org.openhab.binding.monopriceaudio.internal.dto.MonopriceAudioZoneDTO;
import org.openhab.core.types.StateOption;

/**
 * The {@link AmplifierModel} is responsible for mapping low level communications from each supported amplifier model.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public enum AmplifierModel {

    // to avoid breaking existing installations, the 10761/DAX66 will still be known as 'amplifier'
    AMPLIFIER("<", "\r", "?", "", "#>", "PR", "CH", "VO", "MU", "TR", "BS", "BL", "DT", 6, 38, -7, 7, 7, -10, 10, 10,
            18, 6, true, Arrays.asList("11", "12", "13", "14", "15", "16", "21", "22", "23", "24", "25", "26", "31",
                    "32", "33", "34", "35", "36")) {
        @Override
        public MonopriceAudioZoneDTO getZoneData(String newZoneData) {
            MonopriceAudioZoneDTO zoneData = new MonopriceAudioZoneDTO();
            Matcher matcher = MONOPRICE_PATTERN.matcher(newZoneData);

            if (matcher.find()) {
                zoneData.setZone(matcher.group(1));
                zoneData.setPage(matcher.group(2));
                zoneData.setPower(matcher.group(3));
                zoneData.setMute(matcher.group(4));
                zoneData.setDnd(matcher.group(5));
                zoneData.setVolume(Integer.parseInt(matcher.group(6)));
                zoneData.setTreble(Integer.parseInt(matcher.group(7)));
                zoneData.setBass(Integer.parseInt(matcher.group(8)));
                zoneData.setBalance(Integer.parseInt(matcher.group(9)));
                zoneData.setSource(matcher.group(10));
                zoneData.setKeypad(matcher.group(11));
            }

            return zoneData;
        }

        @Override
        public List<StateOption> getSourceLabels(MonopriceAudioThingConfiguration config) {
            List<StateOption> sourceLabels = new ArrayList<>();
            sourceLabels.add(new StateOption("1", config.inputLabel1));
            sourceLabels.add(new StateOption("2", config.inputLabel2));
            sourceLabels.add(new StateOption("3", config.inputLabel3));
            sourceLabels.add(new StateOption("4", config.inputLabel4));
            sourceLabels.add(new StateOption("5", config.inputLabel5));
            sourceLabels.add(new StateOption("6", config.inputLabel6));
            return sourceLabels;
        }
    },
    MONOPRICE70V("!", "+\r", "?", "ZS", "?", "PR", "IS", "VO", "MU", "TR", "BS", "BA", "", 2, 38, -7, 7, 7, -32, 31, 32,
            6, 2, false, Arrays.asList("1", "2", "3", "4", "5", "6")) {
        @Override
        public MonopriceAudioZoneDTO getZoneData(String newZoneData) {
            MonopriceAudioZoneDTO zoneData = new MonopriceAudioZoneDTO();

            Matcher matcher = MONOPRICE70V_PATTERN.matcher(newZoneData);
            if (matcher.find()) {
                zoneData.setZone(matcher.group(1));
                zoneData.setVolume(Integer.parseInt(matcher.group(2)));
                zoneData.setPower(matcher.group(3));
                zoneData.setMute(matcher.group(4));
                zoneData.setSource(matcher.group(5));
                return zoneData;
            }

            matcher = MONOPRICE70V_TREB_PATTERN.matcher(newZoneData);
            if (matcher.find()) {
                zoneData.setZone(matcher.group(1));
                zoneData.setTreble(Integer.parseInt(matcher.group(2)));
                return zoneData;
            }

            matcher = MONOPRICE70V_BASS_PATTERN.matcher(newZoneData);
            if (matcher.find()) {
                zoneData.setZone(matcher.group(1));
                zoneData.setBass(Integer.parseInt(matcher.group(2)));
                return zoneData;
            }

            matcher = MONOPRICE70V_BALN_PATTERN.matcher(newZoneData);
            if (matcher.find()) {
                zoneData.setZone(matcher.group(1));
                zoneData.setBalance(Integer.parseInt(matcher.group(2)));
                return zoneData;
            }

            return zoneData;
        }

        @Override
        public List<StateOption> getSourceLabels(MonopriceAudioThingConfiguration config) {
            List<StateOption> sourceLabels = new ArrayList<>();
            sourceLabels.add(new StateOption("0", config.inputLabel1));
            sourceLabels.add(new StateOption("1", config.inputLabel2));
            return sourceLabels;
        }
    },
    XANTECH44("!", "+\r", "?", "ZD", "#", "PR", "CH", "VO", "MU", "TR", "BS", "BL", "DT", 8, 38, -4, 4, 4, -10, 10, 10,
            4, 4, false, Arrays.asList("1", "2", "3", "4")) {
        @Override
        public MonopriceAudioZoneDTO getZoneData(String newZoneData) {
            return getXantechZoneData(newZoneData);
        }

        @Override
        public List<StateOption> getSourceLabels(MonopriceAudioThingConfiguration config) {
            List<StateOption> sourceLabels = new ArrayList<>();
            sourceLabels.add(new StateOption("1", config.inputLabel1));
            sourceLabels.add(new StateOption("2", config.inputLabel2));
            sourceLabels.add(new StateOption("3", config.inputLabel3));
            sourceLabels.add(new StateOption("4", config.inputLabel4));
            return sourceLabels;
        }
    },
    XANTECH88("!", "+\r", "?", "ZD", "#", "PR", "CH", "VO", "MU", "TR", "BS", "BL", "DT", 8, 38, -4, 4, 4, -10, 10, 10,
            24, 8, false, Arrays.asList("11", "12", "13", "14", "15", "16", "17", "18", "21", "22", "23", "24", "25",
                    "26", "27", "28", "31", "32", "33", "34", "35", "36", "37", "38")) {
        @Override
        public MonopriceAudioZoneDTO getZoneData(String newZoneData) {
            return getXantechZoneData(newZoneData);
        }

        @Override
        public List<StateOption> getSourceLabels(MonopriceAudioThingConfiguration config) {
            List<StateOption> sourceLabels = new ArrayList<>();
            sourceLabels.add(new StateOption("1", config.inputLabel1));
            sourceLabels.add(new StateOption("2", config.inputLabel2));
            sourceLabels.add(new StateOption("3", config.inputLabel3));
            sourceLabels.add(new StateOption("4", config.inputLabel4));
            sourceLabels.add(new StateOption("5", config.inputLabel5));
            sourceLabels.add(new StateOption("6", config.inputLabel6));
            sourceLabels.add(new StateOption("7", config.inputLabel7));
            sourceLabels.add(new StateOption("8", config.inputLabel8));
            return sourceLabels;
        }
    };

    private static MonopriceAudioZoneDTO getXantechZoneData(String newZoneData) {
        MonopriceAudioZoneDTO zoneData = new MonopriceAudioZoneDTO();
        Matcher matcher = XANTECH_PATTERN.matcher(newZoneData);

        if (matcher.find()) {
            zoneData.setZone(matcher.group(1));
            zoneData.setPower(matcher.group(2));
            zoneData.setSource(matcher.group(3));
            zoneData.setVolume(Integer.parseInt(matcher.group(4)));
            zoneData.setMute(matcher.group(5));
            zoneData.setTreble(Integer.parseInt(matcher.group(6)));
            zoneData.setBass(Integer.parseInt(matcher.group(7)));
            zoneData.setBalance(Integer.parseInt(matcher.group(8)));
            zoneData.setKeypad(matcher.group(9));
            zoneData.setPage(matcher.group(10));
        }
        return zoneData;
    }

    // Monoprice 10761 / DAX66 status string: #>1200010000130809100601
    private static final Pattern MONOPRICE_PATTERN = Pattern
            .compile("^#>(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})");

    // Monoprice 70v 31028 status string: ?6ZS VO8 PO1 MU0 IS0+ (does not include treble, bass & balance)
    private static final Pattern MONOPRICE70V_PATTERN = Pattern
            .compile("^\\?(\\d{1})ZS VO(\\d{1,2}) PO(\\d{1}) MU(\\d{1}) IS(\\d{1})+");
    private static final Pattern MONOPRICE70V_TREB_PATTERN = Pattern.compile("^\\?(\\d{1})TR(\\d{1,2})+");
    private static final Pattern MONOPRICE70V_BASS_PATTERN = Pattern.compile("^\\?(\\d{1})BS(\\d{1,2})+");
    private static final Pattern MONOPRICE70V_BALN_PATTERN = Pattern.compile("^\\?(\\d{1})BA(\\d{1,2})+");

    // Xantech status string: #1ZS PR0 SS1 VO0 MU1 TR7 BS7 BA32 LS0 PS0+
    private static final Pattern XANTECH_PATTERN = Pattern.compile(
            "^#(\\d{1,2})ZS PR(\\d{1}) SS(\\d{1}) VO(\\d{1,2}) MU(\\d{1}) TR(\\d{1,2}) BS(\\d{1,2}) BA(\\d{1,2}) LS(\\d{1}) PS(\\d{1})+");

    private String cmdPrefix;
    private String cmdSuffix;
    private String queryPrefix;
    private String querySuffix;
    private String respPrefix;
    private String powerCmd;
    private String sourceCmd;
    private String volumeCmd;
    private String muteCmd;
    private String trebleCmd;
    private String bassCmd;
    private String balanceCmd;
    private String dndCmd;
    private int maxSrc;
    private int maxVol;
    private int minTone;
    private int maxTone;
    private int toneOffset;
    private int minBal;
    private int maxBal;
    private int balOffset;
    private int maxZones;
    private int numSources;
    private boolean padNumbers;
    private List<String> zoneIds;
    private Map<String, String> zoneIdMap = new HashMap<>();

    private static final String ON_STR = "1";
    private static final String OFF_STR = "0";

    private static final String ON_STR_PAD = "01";
    private static final String OFF_STR_PAD = "00";

    /**
     * Constructor for all the enum parameters
     *
     **/
    AmplifierModel(String cmdPrefix, String cmdSuffix, String queryPrefix, String querySuffix, String respPrefix,
            String powerCmd, String sourceCmd, String volumeCmd, String muteCmd, String trebleCmd, String bassCmd,
            String balanceCmd, String dndCmd, int maxSrc, int maxVol, int minTone, int maxTone, int toneOffset,
            int minBal, int maxBal, int balOffset, int maxZones, int numSources, boolean padNumbers,
            List<String> zoneIds) {
        this.cmdPrefix = cmdPrefix;
        this.cmdSuffix = cmdSuffix;
        this.queryPrefix = queryPrefix;
        this.querySuffix = querySuffix;
        this.respPrefix = respPrefix;
        this.powerCmd = powerCmd;
        this.sourceCmd = sourceCmd;
        this.volumeCmd = volumeCmd;
        this.muteCmd = muteCmd;
        this.trebleCmd = trebleCmd;
        this.bassCmd = bassCmd;
        this.balanceCmd = balanceCmd;
        this.dndCmd = dndCmd;
        this.maxSrc = maxSrc;
        this.maxVol = maxVol;
        this.minTone = minTone;
        this.maxTone = maxTone;
        this.toneOffset = toneOffset;
        this.minBal = minBal;
        this.maxBal = maxBal;
        this.balOffset = balOffset;
        this.maxZones = maxZones;
        this.numSources = numSources;
        this.padNumbers = padNumbers;
        this.zoneIds = zoneIds;

        int i = 1;
        for (String zoneId : zoneIds) {
            zoneIdMap.put(zoneId, "zone" + i);
            i++;
        }
    }

    public abstract MonopriceAudioZoneDTO getZoneData(String newZoneData);

    public abstract List<StateOption> getSourceLabels(MonopriceAudioThingConfiguration config);

    public String getZoneIdFromZoneName(String zoneName) {
        for (String zoneId : zoneIdMap.keySet()) {
            if (zoneIdMap.get(zoneId).equals(zoneName)) {
                return zoneId;
            }
        }
        return "";
    }

    public String getZoneName(String zoneId) {
        String zoneName = zoneIdMap.get(zoneId);
        if (zoneName != null) {
            return zoneName;
        } else {
            return "";
        }
    }

    public String getCmdPrefix() {
        return cmdPrefix;
    }

    public String getQueryPrefix() {
        return queryPrefix;
    }

    public String getQuerySuffix() {
        return querySuffix;
    }

    public String getRespPrefix() {
        return respPrefix;
    }

    public String getPowerCmd() {
        return powerCmd;
    }

    public String getSourceCmd() {
        return sourceCmd;
    }

    public String getVolumeCmd() {
        return volumeCmd;
    }

    public String getMuteCmd() {
        return muteCmd;
    }

    public String getTrebleCmd() {
        return trebleCmd;
    }

    public String getBassCmd() {
        return bassCmd;
    }

    public String getBalanceCmd() {
        return balanceCmd;
    }

    public String getDndCmd() {
        return dndCmd;
    }

    public int getMaxSrc() {
        return maxSrc;
    }

    public int getMaxVol() {
        return maxVol;
    }

    public int getMinTone() {
        return minTone;
    }

    public int getMaxTone() {
        return maxTone;
    }

    public int getMinBal() {
        return minBal;
    }

    public int getMaxBal() {
        return maxBal;
    }

    public int getBalOffset() {
        return balOffset;
    }

    public int getToneOffset() {
        return toneOffset;
    }

    public int getMaxZones() {
        return maxZones;
    }

    public int getNumSources() {
        return numSources;
    }

    public String getCmdSuffix() {
        return cmdSuffix;
    }

    public boolean isPadNumbers() {
        return padNumbers;
    }

    public List<String> getZoneIds() {
        return zoneIds;
    }

    public String getOnStr() {
        if (padNumbers) {
            return ON_STR_PAD;
        } else {
            return ON_STR;
        }
    }

    public String getOffStr() {
        if (padNumbers) {
            return OFF_STR_PAD;
        } else {
            return OFF_STR;
        }
    }
}
