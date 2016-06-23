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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.optical.device.OchPortHelper.ochPortDescription;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Retrieves the ports from a Oplink roadm device via netconf.
 */
public class OplinkRoadmDeviceDescription extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    private final Logger log = getLogger(getClass());

    @Override
    public DeviceDescription discoverDeviceDetails() {
        log.info("No description to be added for device");
        //TODO to be implemented if needed.
        return null;
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        String reply;
        try {
            //reply = session.get(requestBuilder());
            reply = session.getConfig("running");
        } catch (IOException e) {
            throw new RuntimeException(new NetconfException("Failed to retrieve configuration.", e));
        }
        List<PortDescription> descriptions =
                parseOplinkRoadmPorts(XmlConfigParser.
                        loadXml(new ByteArrayInputStream(reply.getBytes())));
        return ImmutableList.copyOf(descriptions);
    }

    /**
     * Builds a request crafted to get the configuration required to create port
     * descriptions for the device.
     *
     * @return The request string.
     */
    private String requestBuilder() {
        StringBuilder rpc = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        //Message ID is injected later.
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        rpc.append("<get>");
        rpc.append("<filter type=\"subtree\">");
        rpc.append("<open-oplink-device xmlns=\"http://com/att/device\">");
        rpc.append("<ports/>");
        rpc.append("</open-oplink-device>");
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append("</rpc>");
        return rpc.toString();
    }

    /**
     * Parses a configuration and returns a set of ports for Oplink Roadm.
     *
     * @param cfg a hierarchical configuration
     * @return a list of port descriptions
     */
    private static List<PortDescription> parseOplinkRoadmPorts(HierarchicalConfiguration cfg) {
        List<PortDescription> portDescriptions = Lists.newArrayList();
        List<HierarchicalConfiguration> subtrees =
                cfg.configurationsAt("data.open-oplink-device.ports");
        for (HierarchicalConfiguration portConfig : subtrees) {
            portDescriptions.add(parsePort(portConfig));
        }
        return portDescriptions;
    }

    private static PortDescription parsePort(HierarchicalConfiguration cfg) {
        PortNumber portNumber = PortNumber.portNumber(cfg.getString("port-id"));
        HierarchicalConfiguration portInfo = cfg.configurationAt("port");
        boolean enabled = true;
        OduSignalType signalType = null;
        boolean isTunable = true;
        int wavelengthNumber = portInfo.configurationAt("available-wavelengths").
                getInt("wavelength-number");
        OchSignal lambda = new OchSignal(GridType.DWDM, ChannelSpacing.CHL_50GHZ, wavelengthNumber, 4);
        DefaultAnnotations annotations = DefaultAnnotations.builder().
                set(AnnotationKeys.PORT_NAME, cfg.getString("port-name")).
                build();
        return ochPortDescription(portNumber, enabled, signalType, isTunable, lambda, annotations);
    }
}
