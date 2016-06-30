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
import org.onlab.util.Frequency;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.GridType;
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
import static org.onosproject.net.optical.device.OmsPortHelper.omsPortDescription;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Retrieves the ports from a Oplink roadm device via netconf.
 */
public class OplinkRoadmDeviceDescription extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    private final Logger log = getLogger(getClass());
    public static final GridType GRID_TYPE = GridType.DWDM;
    public static final ChannelSpacing CHANNEL_SPACING = ChannelSpacing.CHL_50GHZ;
    public static final Frequency START_CENTER_FREQ = Frequency.ofGHz(191_350);
    public static final Frequency END_CENTER_FREQ = Frequency.ofGHz(196_100);

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
        String filter = "<open-oplink-device xmlns=\"http://com/att/device\"><ports/></open-oplink-device>";
        String reply;
        try {
            reply = session.getConfig("running", filter);
        } catch (IOException e) {
            throw new RuntimeException(new NetconfException("Failed to retrieve configuration.", e));
        }
        List<PortDescription> descriptions =
                parseOplinkRoadmPorts(XmlConfigParser.
                        loadXml(new ByteArrayInputStream(reply.getBytes())));
        return ImmutableList.copyOf(descriptions);
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
        PortNumber portNumber = PortNumber.portNumber(cfg.getLong("port-id"));
        HierarchicalConfiguration portInfo = cfg.configurationAt("port");
        DefaultAnnotations annotations = DefaultAnnotations.builder().
                set(AnnotationKeys.PORT_NAME, portInfo.getString("port-name")).
                set("portDirection", portInfo.getString("port-direction")).
                build();
        return omsPortDescription(portNumber, true, START_CENTER_FREQ, END_CENTER_FREQ,
                CHANNEL_SPACING.frequency(), annotations);
    }
}
