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
package org.openhab.binding.denonmarantz.internal.xml.entities.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openhab.binding.denonmarantz.internal.xml.adapters.OnOffAdapter;

/**
 * Contains an On/Off value in the form of a boolean
 *
 * @author Jeroen Idserda - Initial contribution
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class OnOffType {

    @XmlJavaTypeAdapter(OnOffAdapter.class)
    private Boolean value;

    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }
}
