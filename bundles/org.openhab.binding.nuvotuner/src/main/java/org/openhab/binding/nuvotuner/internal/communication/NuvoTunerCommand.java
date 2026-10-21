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
package org.openhab.binding.nuvotuner.internal.communication;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Represents the different kinds of commands
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public enum NuvoTunerCommand {
    GET_CONTROLLER_VERSION("VER"),
    GET_STATUS("STATUS"),
    GET_SIRIUS_CHANNEL_LIST("_LISTSR"),
    GET_DAB_CHANNEL_LIST("DABLIST"),
    SET_SIRIUS_CHANNEL("SR"),
    SET_AM_FREQ("AM"),
    SET_FM_FREQ("FM"),
    SET_DAB_CHANNEL("DAB");

    private final String value;

    NuvoTunerCommand(String value) {
        this.value = value;
    }

    /**
     * Get the command name
     *
     * @return the command name
     */
    public String getValue() {
        return value;
    }
}
