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
package org.openhab.binding.lacrosse.internal.interceptor;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lacrosse.internal.LacrosseHandler;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code GatewayInterceptorService} class manages the {@link LacrosseGatewayInterceptorServlet} and forwards
 * received data
 * packets to the handlers.
 *
 * @author Michael Lobstein - Initial contribution
 */
@Component(service = LacrosseGatewayInterceptorService.class, configurationPid = "binding.lacrosse.interceptorService")
@NonNullByDefault
public class LacrosseGatewayInterceptorService {
    private @Nullable HttpService httpService;

    private static final String LACROSSE_ENDPOINT = "/request.breq";
    private final Set<LacrosseHandler> handlers = ConcurrentHashMap.newKeySet();

    private final Logger logger = LoggerFactory.getLogger(LacrosseGatewayInterceptorService.class);

    @Activate
    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        try {
            final HttpService localHttpService = this.httpService;
            if (localHttpService != null) {
                // Register the interceptor servlet
                localHttpService.registerServlet(LACROSSE_ENDPOINT, createInterceptorServlet(), new Hashtable<>(),
                        localHttpService.createDefaultHttpContext());
            }
        } catch (NamespaceException | ServletException | IOException e) {
            logger.warn("Error during Interceptor servlet startup", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        final HttpService localHttpService = this.httpService;
        if (localHttpService != null) {
            // Unregister the interceptor servlet
            localHttpService.unregister(LACROSSE_ENDPOINT);
        }
    }

    /**
     * Constructs a {@code GatewayInterceptorServlet}.
     *
     * @return the newly created servlet
     *
     * @throws {@link IOException}
     *             in case of issues reading one of the internal html templates
     */
    private HttpServlet createInterceptorServlet() throws IOException {
        return new LacrosseGatewayInterceptorServlet(this);
    }

    /**
     * Dispatches the received Lacrosse data packet intercepted from the GW1000U gateway to the handlers
     *
     * @param mac The mac address of the gateway that sent the packet
     * @param pktTyp The type of packet received (current, history, etc.)
     * @param data A string containing the data packet received
     *
     */
    public void dispatchReceivedGatewayData(String mac, String pktType, String data) {
        getLacrosseHandlers().forEach(handler -> {
            handler.handleDataPacket(mac, pktType, data);
        });
    }

    public List<SimpleEntry<String, Object>> getConfigMaps() {
        List<SimpleEntry<String, Object>> configMaps = new ArrayList<SimpleEntry<String, Object>>();
        getLacrosseHandlers().forEach(handler -> {
            configMaps.add(handler.getConfigMap());
        });
        return configMaps;
    }

    /**
     * Adds a {@link LacrosseHandler} handler to the set of Lacrosse handlers.
     *
     * @param handler
     *            the handler to add to the handlers set
     */
    public void addLacrosseHandler(LacrosseHandler handler) {
        handlers.add(handler);
    }

    /**
     * Removes a {@link LacrosseHandler} handler from the set of Lacrosse handlers.
     *
     * @param handler
     *            the handler to remove from the handlers set
     */
    public void removeLacrosseHandler(LacrosseHandler handler) {
        handlers.remove(handler);
    }

    /**
     * Returns all the {@link LacrosseHandler} handlers.
     *
     * @return a set containing all the Lacrosse handlers
     */
    public Set<LacrosseHandler> getLacrosseHandlers() {
        return handlers;
    }

    @Reference
    protected void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    protected void unsetHttpService(HttpService httpService) {
        this.httpService = null;
    }
}
