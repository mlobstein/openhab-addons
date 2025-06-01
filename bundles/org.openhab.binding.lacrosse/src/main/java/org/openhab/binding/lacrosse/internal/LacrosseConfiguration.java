/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.binding.lacrosse.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link LacrosseConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class LacrosseConfiguration {
    public String gatewaySn = "";
    public String stationSn = "";
    public String sensor1sn = "";
    public String sensor2sn = "";
    public String sensor3sn = "";
    public String sensor4sn = "";
    public String sensor5sn = "";
}
