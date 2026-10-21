/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.binding.nuvotuner.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link NuvoTunerBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class NuvoTunerBindingConstants {
    public static final String BINDING_ID = "nuvotuner";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_TUNER = new ThingTypeUID(BINDING_ID, "tuner");

    // List of all Tuner Channel types
    public static final String CHANNEL_TYPE_ACTIVE_BAND = "active-band";
    public static final String CHANNEL_TYPE_SIRIUS_CHANNEL = "sirius-channel";
    public static final String CHANNEL_TYPE_AM_FREQUENCY = "am-frequency";
    public static final String CHANNEL_TYPE_FM_FREQUENCY = "fm-frequency";
    public static final String CHANNEL_TYPE_DAB_CHANNEL = "dab-channel";
    public static final String CHANNEL_TYPE_REFRESH_CHANNELS_SW = "refresh-channels";

    // Message types
    public static final String TYPE_TUNER_UPDATE = "tuner_update";
    public static final String TYPE_SIRIUS_CHANNEL_LIST = "sirius_ch_list";
    public static final String TYPE_DAB_CHANNEL_LIST = "dab_ch_list";

    // Tuner band types
    public static final String BAND_SR = "SR";
    public static final String BAND_AM = "AM";
    public static final String BAND_FM = "FM";
    public static final String BAND_DAB = "DAB";
}
