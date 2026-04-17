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
package org.openhab.binding.lacrosse.internal.interceptor;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
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
 * received data packets to the handlers.
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

    Map<String, Integer> lastHistoryAddressMap = new HashMap<String, Integer>();
    Map<String, String> gatewayIpAddressMap = new HashMap<String, String>();
    Map<String, String> unconfiguredGatewayIpAddressMap = new HashMap<String, String>();

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
            logger.warn("Error during LacrosseInterceptorServlet startup", e);
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
     * Dispatches a ping to the handlers when non-data packets are received by the GW1000U gateway
     *
     * @param gatewaySn The serial number of the gateway that received the packet
     *
     */
    public void dispatchReceivedPing(String gatewaySn) {
        getLacrosseHandlers().forEach(handler -> {
            handler.handlePing(gatewaySn);
        });
    }

    /**
     * Dispatches the Lacrosse data packet received by the GW1000U gateway to the handlers
     *
     * @param gatewaySn The serial number of the gateway that received the packet
     * @param pktTyp The type of packet received (current, history, etc.)
     * @param data A string containing the data packet received
     *
     */
    public void dispatchReceivedGatewayData(String gatewaySn, String pktType, String data) {
        getLacrosseHandlers().forEach(handler -> {
            handler.handleDataPacket(gatewaySn, pktType, data);
        });
    }

    public List<SimpleEntry<String, ?>> getConfigMaps() {
        List<SimpleEntry<String, ?>> configMaps = new ArrayList<SimpleEntry<String, ?>>();
        getLacrosseHandlers().forEach(handler -> {
            configMaps.add(handler.getConfigMap());
        });
        return configMaps;
    }

    public void saveLastHistoryAddress(String gatewaySn, Integer lastHistoryAddres) {
        lastHistoryAddressMap.put(gatewaySn, lastHistoryAddres);
    }

    public Integer getLastHistoryAddress(String gatewaySn) {
        final Integer lastHistoryAddressLocal = lastHistoryAddressMap.get(gatewaySn);
        return lastHistoryAddressLocal != null ? lastHistoryAddressLocal : 0;
    }

    public void putGatewayIpAddress(String gatewaySn, String ipAddress) {
        gatewayIpAddressMap.put(gatewaySn, ipAddress);
    }

    public @Nullable String getGatewayIpAddress(String gatewaySn) {
        return gatewayIpAddressMap.get(gatewaySn);
    }

    public void putUnconfiguredGatewayInfo(String gatewaySn, String ipAddress) {
        unconfiguredGatewayIpAddressMap.put(gatewaySn, ipAddress);
    }

    public Map<String, String> getUnconfiguredGatewayMap() {
        return unconfiguredGatewayIpAddressMap;
    }

    public void removeUnconfiguredGatewayInfo(String gatewaySn) {
        unconfiguredGatewayIpAddressMap.remove(gatewaySn);
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
