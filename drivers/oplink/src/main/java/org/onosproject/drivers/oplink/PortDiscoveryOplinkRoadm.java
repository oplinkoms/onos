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

import com.google.common.collect.Lists;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.behaviour.PortDiscovery;
import org.onosproject.net.device.OmsPortDescription;
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
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Retrieves the ports from a Oplink roadm device via netconf.
 */
public class PortDiscoveryOplinkRoadm extends AbstractHandlerBehaviour
        implements PortDiscovery {

    private final Logger log = getLogger(getClass());

    @Override
    public List<PortDescription> getPorts() {
        log.warn("PortDiscoveryOplinkRoadm: getPorts()");
        System.out.println("PortDiscoveryOplinkRoadm: getPorts()");
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        String reply;
        try {
            reply = session.get(requestBuilder());
        } catch (IOException e) {
            log.error("Failed to retrieve configuration.");
            System.out.println("Failed to retrieve configuration.");
            throw new RuntimeException(new NetconfException("Failed to retrieve configuration.", e));
        }
        List<PortDescription> descriptions = parseOmsPorts(XmlConfigParser.
                loadXml(new ByteArrayInputStream(reply.getBytes())));
        log.warn("PortDiscoveryOplinkRoadm: finished parseOmsPorts");
        System.out.println("PortDiscoveryOplinkRoadm: finished parseOmsPorts");
        return descriptions;
    }

    /**
     * Builds a request crafted to get the configuration required to create port
     * descriptions for the device.
     * @return The request string.
     */
    private String requestBuilder() {
        StringBuilder rpc = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        //Message ID is injected later.
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        rpc.append("<get>");
        rpc.append("<filter type=\"subtree\">");
        rpc.append("<interfaces xmlns=\"urn:ietf:params:xml:ns:yang:ietf-interfaces\">");
        rpc.append("</interfaces>");
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append("</rpc>");
        return rpc.toString();
    }

    /**
     * Parses a configuration and returns a set of ports for the oplink roadm.
     * @param cfg a hierarchical configuration
     * @return a list of port descriptions
     */
    private List<PortDescription> parseOmsPorts(HierarchicalConfiguration cfg) {
        List<PortDescription> portDescriptions = Lists.newArrayList();
        List<HierarchicalConfiguration> subtrees =
                cfg.configurationsAt("data.interfaces.interface");
        for (HierarchicalConfiguration omsConfig : subtrees) {
            if (omsConfig.getString("type").equals("ianaift:oms")) {
                HierarchicalConfiguration portConfig = omsConfig.configurationAt("port");
                int portNumber = portConfig.getInt("number");
                int portDirection = portConfig.getInt("direction");
                SparseAnnotations ann = DefaultAnnotations.builder()
                        .set(AnnotationKeys.PORT_NAME, portDirection + "-" + portNumber)
                        .build();
                PortDescription p = new OmsPortDescription(
                        PortNumber.portNumber(portDescriptions.size() + 1),
                        true,
                        OplinkRoadmDevice.START_CENTER_FREQ,
                        OplinkRoadmDevice.END_CENTER_FREQ,
                        OplinkRoadmDevice.CHANNEL_SPACING.frequency(),
                        ann);
                portDescriptions.add(p);
            }
        }
        return portDescriptions;
    }
}
