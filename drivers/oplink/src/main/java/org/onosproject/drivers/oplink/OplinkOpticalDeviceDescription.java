/*
 * Copyright 2016 Open Networking Foundation
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
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.PortNumber;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onlab.packet.ChassisId;
import org.slf4j.Logger;

import java.util.List;

import static org.onosproject.drivers.oplink.OplinkOpticalUtility.CHANNEL_SPACING;
import static org.onosproject.drivers.oplink.OplinkOpticalUtility.START_CENTER_FREQ;
import static org.onosproject.drivers.oplink.OplinkOpticalUtility.STOP_CENTER_FREQ;
import static org.onosproject.net.optical.device.OmsPortHelper.omsPortDescription;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.drivers.oplink.OplinkNetconfUtility.*;

/**
 * Retrieves the ports from an Oplink optical netconf device.
 */
public class OplinkOpticalDeviceDescription extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    private static final String KEY_DIRECTION = "direction";
    private static final String KEY_INFO = "info";
    private static final String KEY_VENDOR = "vendor";
    private static final String KEY_NODETYPE = "node-type";
    private static final String KEY_NODENUM = "node-number";
    private static final String KEY_SWVER = "softwareVersion";
    private static final String KEY_DATA_ORGOPENRDMDEV_INFO = String.format("%s.%s.%s", KEY_DATA, KEY_ORGOPENRDMDEV, KEY_INFO);

    // log
    private static final Logger log = getLogger(OplinkOpticalDeviceDescription.class);

    @Override
    public DeviceDescription discoverDeviceDetails() {
        log.debug("Device description to be added for device {}", data().deviceId());
        String reply = netconfGet(handler(), getInfoFilter());
        return parseInfo(reply);
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        log.debug("Port description to be added for device {}", data().deviceId());
        String reply = netconfGet(handler(), getPortsFilter());
        List<PortDescription> descriptions = parsePorts(reply);
        return ImmutableList.copyOf(descriptions);
    }



    private String getInfoFilter() {
        return new StringBuilder(xmlOpen(KEY_ORGOPENRDMDEV_XMLNS))
            .append(xmlEmpty(KEY_INFO))
            .append(xmlClose(KEY_ORGOPENRDMDEV))
            .toString();
    }

    private DeviceDescription parseInfo(String content) {
        HierarchicalConfiguration subtree = configAt(content, KEY_DATA_ORGOPENRDMDEV_INFO);
        Device.Type type = mapNodeType(subtree.getString(KEY_NODETYPE));
        if (type == null) {
            return null;
        }
        String vendor = subtree.getString(KEY_VENDOR);
        String swVersion = subtree.getString(KEY_SWVER);
        // did not find appropriate loc for hwVersion and serialNumber
        String hwVersion = "unknown";
        String serialNumber = "unknown";
        Integer chassisId = subtree.getInteger(KEY_NODENUM, 0);
        ChassisId cid = new ChassisId(chassisId);
        return new DefaultDeviceDescription(data().deviceId().uri(), type, vendor, hwVersion, swVersion, serialNumber, cid, true);
    }
    
    private Device.Type mapNodeType(String nodeType) {
        if (nodeType.equals("rdm")) {
            return Device.Type.ROADM;
        } else if (nodeType.equals("toa")) {
            return Device.Type.OPTICAL_AMPLIFIER;
        } else if (nodeType.equals("ops")) {
            return Device.Type.FIBER_SWITCH;
        } else {
            log.debug("Device {} reports unsupported type", data().deviceId());
            return null;
        }
    }



    private String getPortsFilter() {
        return new StringBuilder(xmlOpen(KEY_ORGOPENRDMDEV_XMLNS))
                .append(xmlOpen(KEY_CIRPACKS))
                .append(xmlEmpty(KEY_PORTS))
                .append(xmlClose(KEY_CIRPACKS))
                .append(xmlClose(KEY_ORGOPENRDMDEV))
                .toString();
    }

    private List<PortDescription> parsePorts(String content) {
        List<HierarchicalConfiguration> subtrees = configsAt(content, KEY_DATA_ORGOPENRDMDEV_CIRPACKS_PORTS);
        List<PortDescription> portDescriptions = Lists.newArrayList();
        for (HierarchicalConfiguration portConfig : subtrees) {
            portDescriptions.add(parsePort(portConfig));
        }
        return portDescriptions;
    }

    private PortDescription parsePort(HierarchicalConfiguration cfg) {
        HierarchicalConfiguration portInfo = cfg.configurationAt(KEY_PORT);
        DefaultAnnotations annotations = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, portInfo.getString(KEY_PORTNAME))
                .set(KEY_DIRECTION, portInfo.getString(KEY_PORTDIRECT))
                .build();
        PortNumber portNumber = PortNumber.portNumber(cfg.getLong(KEY_PORTID), portInfo.getString(KEY_PORTNAME));
        return omsPortDescription(portNumber,
                                  true,
                                  START_CENTER_FREQ,
                                  STOP_CENTER_FREQ,
                                  CHANNEL_SPACING.frequency(),
                                  annotations);
    }
}
