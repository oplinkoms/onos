/*
 * Copyright 2016 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.drivers.oplink;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Get/Set the power from a Oplink roadm device via netconf.
 */
public class OplinkRoadmPowerConfig<T> extends AbstractHandlerBehaviour
        implements PowerConfig<T> {
    private final Logger log = getLogger(getClass());

    @Override
    public Optional<Long> getTargetPower(PortNumber port, T component) {
        return Optional.of(acquireTargetPower(port, component));
    }

    @Override
    public void setTargetPower(PortNumber port, T component, long power) {
        if (component instanceof OchSignal) {
            setChannelTargetPower(port, (OchSignal) component, power);
        } else {
            setPortTargetPower(port, power);
        }
    }

    @Override
    public Optional<Long> currentPower(PortNumber port, T component) {
        return Optional.of(acquireCurrentPower(port, component));
    }

    /**
     * Retrieves session reply information for get config operation.
     *
     * @param request the request string of xml content
     * @return The reply string
     */
    private String netconfGetConfig(String filter) {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        String reply;
        try {
            reply = session.getConfig("running", filter);
        } catch (IOException e) {
            throw new RuntimeException(new NetconfException("Failed to retrieve configuration.", e));
        }
        return reply;
    }

    /**
     * Retrieves session reply information for edit config operation.
     *
     * @param cfg the new configuration to be set
     * @return The reply string
     */
    private boolean netconfEditConfig(String cfg) {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        boolean reply = false;
        try {
            reply = session.editConfig("running", "replace", cfg);
        } catch (IOException e) {
            throw new RuntimeException(new NetconfException("Failed to edit configuration.", e));
        }
        return reply;
    }

    /**
     * Builds a request filter crafted to get the configuration required to get port
     * target power for the device.
     *
     * @param port the device port number used to get power
     * @return The request string
     */
    private String getPortPowerFilter(PortNumber port) {
        StringBuilder filt = new StringBuilder("<open-oplink-device xmlns=\"http://com/att/device\">");
        filt.append("<ports><port-id>");
        filt.append(Long.toString(port.toLong()));
        filt.append("</port-id></ports>");
        filt.append("</open-oplink-device>");
        return filt.toString();
    }

    /**
     * Builds a request filter crafted to get the configuration required to get channel
     * target power for the device.
     *
     * @param port the device port number used to get power
     * @Param channel the wavelength number used to get channel power
     * @return The request string
     */
    private String getChannelPowerFilter(PortNumber port, OchSignal channel) {
        StringBuilder filt = new StringBuilder("<open-oplink-device xmlns=\"http://com/att/device\">");
        filt.append("<ports><port-id>");
        filt.append(Long.toString(port.toLong()));
        filt.append("</port-id>");
        filt.append("<port><used-wavelengths><wavelength-number>");
        filt.append(Integer.toString(channel.spacingMultiplier()));
        filt.append("</wavelength-number></used-wavelengths></port>");
        filt.append("</ports>");
        filt.append("</open-oplink-device>");
        return filt.toString();
    }

    private Long acquireTargetPower(PortNumber port, T component) {
        if (component instanceof OchSignal) {
            return acquireChannelPower(port, (OchSignal) component, "wavelength-target-power");
        }
        return acquirePortPower(port, "port-target-power");
    }

    private Long acquireCurrentPower(PortNumber port, T component) {
        Long power = -10000L;
        if (component instanceof OchSignal) {
            return acquireChannelPower(port, (OchSignal) component, "wavelength-current-power");
        }
        return acquirePortPower(port, "port-current-power");
    }

    private Long acquirePortPower(PortNumber port, String selection) {
        String reply = netconfGetConfig(getPortPowerFilter(port));
        HierarchicalConfiguration cfg =
                XmlConfigParser.loadXml(new ByteArrayInputStream(reply.getBytes()));
        HierarchicalConfiguration info =
            cfg.configurationAt("data.open-oplink-device.ports.port");
        return info.getLong(selection);
    }

    private Long acquireChannelPower(PortNumber port, OchSignal channel, String selection) {
        String reply = netconfGetConfig(getChannelPowerFilter(port, channel));
        HierarchicalConfiguration cfg =
                XmlConfigParser.loadXml(new ByteArrayInputStream(reply.getBytes()));
        HierarchicalConfiguration info =
            cfg.configurationAt("data.open-oplink-device.ports.port.used-wavelengths");
        return info.getLong(selection);
    }

    private boolean setPortTargetPower(PortNumber port, long power) {
        StringBuilder cfg = new StringBuilder("<open-oplink-device xmlns=\"http://com/att/device\">");
        cfg.append("<ports><port-id>");
        cfg.append(Long.toString(port.toLong()));
        cfg.append("</port-id>");
        cfg.append("<port><port-target-power>");
        cfg.append(Long.toString(power));
        cfg.append("</port-target-power></port>");
        cfg.append("</ports>");
        cfg.append("</open-oplink-device>");
        return netconfEditConfig(cfg.toString());
    }

    private boolean setChannelTargetPower(PortNumber port, OchSignal channel, long power) {
        StringBuilder cfg = new StringBuilder("<open-oplink-device xmlns=\"http://com/att/device\">");
        cfg.append("<ports><port-id>");
        cfg.append(Long.toString(port.toLong()));
        cfg.append("</port-id>");
        cfg.append("<port><used-wavelengths><wavelength-number>");
        cfg.append(Integer.toString(channel.spacingMultiplier()));
        cfg.append("</wavelength-number>");
        cfg.append("<wavelength-target-power>");
        cfg.append(Long.toString(power));
        cfg.append("</wavelength-target-power>");
        cfg.append("</used-wavelengths></port>");
        cfg.append("</ports>");
        cfg.append("</open-oplink-device>");
        return netconfEditConfig(cfg.toString());
    }
}
