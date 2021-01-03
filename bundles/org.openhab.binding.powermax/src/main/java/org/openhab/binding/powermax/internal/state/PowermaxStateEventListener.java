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
package org.openhab.binding.powermax.internal.state;

import java.util.EventListener;
import java.util.EventObject;

/**
 * Powermax Alarm state Listener interface. Handles Powermax Alarm state events
 *
 * @author Laurent Garnier - Initial contribution
 */
public interface PowermaxStateEventListener extends EventListener {

    /**
     * Event handler method for Powermax Alarm state events
     *
     * @param event the event object
     */
    public void onNewStateEvent(EventObject event);
}
