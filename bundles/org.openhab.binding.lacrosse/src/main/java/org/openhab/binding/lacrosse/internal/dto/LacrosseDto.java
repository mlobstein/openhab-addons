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
package org.openhab.binding.lacrosse.internal.dto;

import java.util.Date;

/**
 * The {@link LacrosseDto} pojo is the base object for the dtos that will contain data received from the La Crosse
 * devices
 *
 * @author Michael Lobstein - Initial contribution
 */
public class LacrosseDto {

    private Integer rfSignalStrength;
    private Date lastSeen;

    public Integer getRfSignalStrength() {
        return rfSignalStrength;
    }

    public void setRfSignalStrength(Integer rfSignalStrength) {
        this.rfSignalStrength = rfSignalStrength;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }
}
