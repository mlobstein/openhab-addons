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
 * Represents the different IDs of the Nuvo Tuner
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public enum NuvoTunerEnum {
    TUNER_A("T'A'"),
    TUNER_B("T'B'");

    private final String id;

    NuvoTunerEnum(String id) {
        this.id = id;
    }

    /**
     * Get the id
     *
     * @return the id
     */
    public String getId() {
        return id;
    }
}
