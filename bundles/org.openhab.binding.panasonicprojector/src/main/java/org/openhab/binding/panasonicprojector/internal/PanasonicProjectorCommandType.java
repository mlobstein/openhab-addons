/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.panasonicprojector.internal;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Represents all valid command types which could be processed by this
 * binding.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public enum PanasonicProjectorCommandType {
    POWER,
    SOURCE,
    PICTUREMODE,
    FREEZE,
    BLANK,
    BUTTON;

    /**
     * Procedure to convert command type string to command type class.
     *
     * @param commandTypeText
     *            command string e.g. power, source, picturemode, etc.
     * @return corresponding command type.
     * @throws IllegalArgumentException
     *             No valid class for command type.
     */
    public static PanasonicProjectorCommandType getCommandType(String commandTypeText) throws IllegalArgumentException {
        for (PanasonicProjectorCommandType c : PanasonicProjectorCommandType.values()) {
            if (c.name().toLowerCase(Locale.ENGLISH).equals(commandTypeText)) {
                return c;
            }
        }

        throw new IllegalArgumentException("Not valid command type: " + commandTypeText);
    }
}
