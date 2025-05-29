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

import static org.openhab.binding.lacrosse.internal.LacrosseBindingConstants.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lacrosse.internal.interceptor.LacrosseGatewayInterceptorService;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link LacrosseHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.lacrosse", service = ThingHandlerFactory.class)
public class LacrosseHandlerFactory extends BaseThingHandlerFactory {

    private final LacrosseGatewayInterceptorService interceptorService;

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_SENSOR,
            THING_TYPE_WEATHER_STATION);

    @Activate
    public LacrosseHandlerFactory(@Reference LacrosseGatewayInterceptorService interceptorService) {
        this.interceptorService = interceptorService;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_SENSOR.equals(thingTypeUID) || THING_TYPE_WEATHER_STATION.equals(thingTypeUID)) {
            final LacrosseHandler handler = new LacrosseHandler(thing);
            this.interceptorService.addLacrosseHandler(handler);
            return handler;
        }

        return null;
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        interceptorService.removeLacrosseHandler((LacrosseHandler) thingHandler);
    }
}
