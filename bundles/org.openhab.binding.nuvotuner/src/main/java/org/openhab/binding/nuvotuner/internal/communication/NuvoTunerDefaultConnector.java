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
import org.openhab.binding.nuvotuner.internal.NuvoTunerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to create a default NuvoDefaultConnector before initialization is complete.
 *
 * @author Laurent Garnier - Initial contribution
 * @author Michael Lobstein - Adapted for the Nuvo binding
 */
@NonNullByDefault
public class NuvoTunerDefaultConnector extends NuvoTunerConnector {

    private final Logger logger = LoggerFactory.getLogger(NuvoTunerDefaultConnector.class);

    @Override
    public void open() throws NuvoTunerException {
        logger.warn("Nuvo binding incorrectly configured. Please configure for Serial or IP over serial connection");
        setConnected(false);
    }

    @Override
    public void close() {
        setConnected(false);
    }
}
