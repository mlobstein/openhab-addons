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
package org.openhab.binding.omnilink.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link BridgeOfflineException} defines an exception for when the OmniLink
 * Bridge is offline or unavailable.
 *
 * @author Craig Hamilton - Initial contribution
 */
@NonNullByDefault
public class BridgeOfflineException extends Exception {
    private static final long serialVersionUID = -9081729691518514097L;

    public BridgeOfflineException(Exception e) {
        super(e);
    }
}
