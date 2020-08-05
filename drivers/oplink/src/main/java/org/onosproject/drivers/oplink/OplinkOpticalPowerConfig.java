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

import com.google.common.collect.Range;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.driver.extensions.OplinkAttenuation;
import org.onosproject.net.*;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficTreatment;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

import static org.onosproject.drivers.oplink.OplinkOpticalUtility.POWER_MULTIPLIER;
import static org.onosproject.drivers.oplink.OplinkOpticalUtility.RANGE_ATT;
import static org.onosproject.drivers.oplink.OplinkOpticalUtility.RANGE_GENERAL;
import static org.onosproject.drivers.oplink.OplinkNetconfUtility.*;
import static org.onosproject.drivers.oplink.OplinkOpticalProtectionSwitchConfig.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Get current or target port/channel power from an Oplink optical netconf device.
 * Set target port power or channel attenuation to an optical netconf device.
 */
public class OplinkOpticalPowerConfig<T> extends AbstractHandlerBehaviour
        implements PowerConfig<T> {

    // key
    public static final String KEY_CHNUM = "wavelength-number";
    public static final String KEY_CHPWR = "wavelength-power";
    public static final String KEY_CHSTATS = "wavelength-stats";
    public static final String KEY_OCMSTATS = "ocm-stats";
    public static final String KEY_PORTDIRECT_RX = "rx";
    public static final String KEY_PORTDIRECT_TX = "tx";
    public static final String KEY_PORTTARPWR = "port-target-power";
    public static final String KEY_PORTCURPWR = "port-current-power";
    public static final String KEY_PORTPROPERTY = "port-property";
    public static final String KEY_PORTPWRCAPMINRX = "port-power-capability-min-rx";
    public static final String KEY_PORTPWRCAPMAXRX = "port-power-capability-max-rx";
    public static final String KEY_PORTPWRCAPMINTX = "port-power-capability-min-tx";
    public static final String KEY_PORTPWRCAPMAXTX = "port-power-capability-max-tx";
    public static final String KEY_PORTS_PORT = String.format("%s.%s", KEY_DATA_PORTS, KEY_PORT);
    public static final String KEY_PORTS_PORT_PROPERTY = String.format("%s.%s", KEY_PORTS_PORT, KEY_PORTPROPERTY);
    public static final String KEY_OCMS = "ocms";

    // new key for new yang models
    public static final String KEY_CIRPACKNAME = "circuit-pack-name";
    public static final String KEY_INSTANT = "instant";
    public static final String KEY_WORKMODE = "work-mode";
    public static final String KEY_STATE = "state";
    public static final String KEY_CONFIG = "config";
    public static final String KEY_NAME = "name";
    public static final String KEY_PA = "PA";
    public static final String KEY_BA = "BA";
    public static final String KEY_AMPLIFIER = "amplifier";
    public static final String KEY_AMPLIFIERS = "amplifiers";
    public static final String KEY_OPTICALAMPLIFIER = "optical-amplifier";
    public static final String KEY_XMLNSTOA = "xmlns=\"http://openconfig.net/yang/optical-amplfier\"";
    public static final String KEY_OPTICALAMPLIFIER_XMLNS = String.format("%s %s", KEY_OPTICALAMPLIFIER, KEY_XMLNSTOA);
    public static final String KEY_OPTICALMODULES = "optical-modules";
    public static final String KEY_OPTICALCONTROL = "optical-control";
    public static final String KEY_XMLNSOPTICAL = "xmlns=\"http://com/oplink/optical\"";
    public static final String KEY_OPTICALCONTROL_XMLNS = String.format("%s %s", KEY_OPTICALCONTROL, KEY_XMLNSOPTICAL);
    public static final String KEY_DATA_PORT = String.format("%s.%s.%s.%s.%s.%s", KEY_DATA, KEY_OPTICALCONTROL, KEY_OPTICALMODULES, KEY_OPTICALAMPLIFIER, KEY_AMPLIFIERS, KEY_AMPLIFIER);
    public static final String KEY_TARGETGAIN = "target-gain";
    public static final String KEY_AMP = "amp";
    public static final String KEY_OPS = "ops";
    public static final String KEY_DATA_ORGOPENRDMDEV_CIRPACKS_PORTS_PORT = String.format("%s.%s", KEY_DATA_ORGOPENRDMDEV_CIRPACKS_PORTS, KEY_PORT);

    // log
    private static final Logger log = getLogger(OplinkOpticalPowerConfig.class);

    @Override
    public Optional<Double> getTargetPower(PortNumber port, T component) {
        Long power = acquireTargetPower(port, component);
        if (power == null) {
            return Optional.empty();
        }
        return Optional.of(power.doubleValue());
    }

    @Override
    public void setTargetPower(PortNumber port, T component, double power) {
        if (component instanceof OchSignal) {
            setChannelTargetPower(port, (OchSignal) component, (long) power);
        } else {
            setPortTargetGain(port, (long) power);
        }
    }

    @Override
    public Optional<Double> currentPower(PortNumber port, T component) {
        Long power = acquireCurrentPower(port, component, Direction.EGRESS);
        if (power == null) {
            return Optional.empty();
        }
        return Optional.of(power.doubleValue());
    }

    @Override
    public Optional<Double> currentInputPower(PortNumber port, T component) {
        Long power = acquireCurrentPower(port, component, Direction.INGRESS);
        if (power == null) {
            return Optional.empty();
        }
        return Optional.of(power.doubleValue());
    }

    @Override
    public Optional<Range<Double>> getTargetPowerRange(PortNumber port, T component) {
        Range<Long> power = getTxPowerRange(port, component);
        if (power == null) {
            return Optional.empty();
        }
        return Optional.of(Range.closed((double) power.lowerEndpoint(), (double) power.upperEndpoint()));
    }

    @Override
    public Optional<Range<Double>> getInputPowerRange(PortNumber port, T component) {
        Range<Long> power = getRxPowerRange(port, component);
        if (power == null) {
            return Optional.empty();
        }
        return Optional.of(Range.closed((double) power.lowerEndpoint(), (double) power.upperEndpoint()));
    }

    private Long acquireTargetPower(PortNumber port, T component) {
        if (component instanceof OchSignal) {
            return acquireChannelAttenuation(port, (OchSignal) component);
        }
        log.debug("Get port{} target power...", port);
        // get target gain
        Long targetGain = acquireTargetGain(port);
        if (targetGain == null) {
            return null;
        }
        // get current input power
        PortNumber activePort = acquireActivePort();
        if (activePort == null) {
            activePort = PortNumber.portNumber(5 - port.toLong(), null);
        }
        long powerIn = acquireCurrentPower(activePort, null, Direction.INGRESS);
        return powerIn + targetGain;
    }

    /** * This func extracts power info from optical-amplifier block
     * The one below extracts from circuit-packs block, whose data are more plausible
    private Long acquireCurrentPower(PortNumber port, T component) {
        if (component instanceof OchSignal) {
            return acquireChannelPower(port, (OchSignal) component);
        }
        log.debug("Get port{} current power...", port);
        String selection = getKeyInOrKeyOut(port);
        String name = getPortName(port);
        String reply = netconfGet(handler(), getCurrentPowerFilter(name, selection));
        String filter = String.format("%s.%s.%s", KEY_DATA_PORT, KEY_STATE, selection);
        HierarchicalConfiguration info = configAt(reply, filter);
        if (info == null) {
            return null;
        }
        return (long) (info.getDouble(KEY_INSTANT) * POWER_MULTIPLIER);
    }
    */

    private Long acquireCurrentPower(PortNumber port, T component, Direction dir) {
        if (component instanceof OchSignal) {
            return acquireChannelPower(port, (OchSignal) component);
        }
        log.debug("Get port{} current power...", port);
        String reply = netconfGet(handler(), getCurrentPowerFilter(port));
        HierarchicalConfiguration info = configAt(reply, KEY_DATA_ORGOPENRDMDEV_CIRPACKS_PORTS_PORT);
        if (info == null) {
            return null;
        }
        String portDir = getPortDir(port);
        if (portDir == null) {
            return null;
        }
        if ((dir.equals(Direction.INGRESS) && portDir.equals(KEY_PORTDIRECT_RX))
            || (dir.equals(Direction.EGRESS) && portDir.equals(KEY_PORTDIRECT_TX))) {
            return (long) (info.getDouble(KEY_PORTCURPWR) * POWER_MULTIPLIER);
        }
        return null;
    }

    private Long acquireChannelAttenuation(PortNumber port, OchSignal channel) {
        log.debug("Get port{} channel{} attenuation...", port, channel.channelSpacing());
        String reply = netconfGet(handler(), getChannelAttenuationFilter(port, channel));
        HierarchicalConfiguration info = configAt(reply, KEY_CONNS);
        if (info == null) {
            return null;
        }
        return (long) (info.getDouble(KEY_CHATT) * POWER_MULTIPLIER);
    }

    private Long acquireChannelPower(PortNumber port, OchSignal channel) {
        log.debug("Get port{} channel{} power...", port, channel.channelSpacing());
        String reply = netconfGet(handler(), getChannelPowerFilter(port, channel));
        HierarchicalConfiguration info = configAt(reply, KEY_DATA_CONNS);
        if (info == null) {
            return null;
        }
        return (long) (info.getDouble(KEY_CHPWR) * POWER_MULTIPLIER);
    }

    private Long acquireTargetGain(PortNumber port) {
        if (!getPortDir(port).equals(KEY_PORTDIRECT_TX)) {
            return null;
        }
        String name = getAmpName(port);
        String reply = netconfGet(handler(), getTargetGainFilter(name));
        String filter = String.format("%s.%s", KEY_DATA_PORT, KEY_CONFIG);
        HierarchicalConfiguration info = configAt(reply, filter);
        if (info == null) {
            return null;
        }
        return (long) (info.getDouble(KEY_TARGETGAIN) * POWER_MULTIPLIER);
    }

    private boolean setPortTargetGain(PortNumber port, long power) {
        log.debug("Set port{} target power...", port);
        String gain = outputToGain(port, power);
        String name = getAmpName(port);
        String cfg = setTargetGainFilter(name, gain);
        return netconfEditConfig(handler(), CFG_MODE_MERGE, cfg);
    }

    private boolean setChannelTargetPower(PortNumber port, OchSignal channel, long power) {
        log.debug("Set port{} channel{} attenuation.", port, channel.channelSpacing());
        FlowRuleService service = handler().get(FlowRuleService.class);
        Iterable<FlowEntry> entries = service.getFlowEntries(data().deviceId());
        for (FlowEntry entry : entries) {
            OplinkCrossConnect crossConnect = OplinkOpticalUtility.fromFlowRule(this, entry);
            // The channel port might be input port or output port.
            if ((port.equals(crossConnect.getInPort()) || port.equals(crossConnect.getOutPort())) &&
                    channel.spacingMultiplier() == crossConnect.getChannel()) {
                log.debug("Flow is found, modify the flow with attenuation.");
                // Modify attenuation in treatment
                TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                        .setOutput(crossConnect.getOutPort())
                        .extension(new OplinkAttenuation((int) power), data().deviceId())
                        .build();
                // Apply the new flow rule
                service.applyFlowRules(DefaultFlowRule.builder()
                        .forDevice(data().deviceId())
                        .makePermanent()
                        .withSelector(entry.selector())
                        .withTreatment(treatment)
                        .withPriority(entry.priority())
                        .withCookie(entry.id().value())
                        .build());
                return true;
            }
        }
        return false;
    }

    private Range<Long> getPowerRange(PortNumber port, String directionKey, String minKey, String maxKey) {
        // TODO
        // Optical protection switch does not support power range configuration, it'll reply error.
        // To prevent replying error log flooding from netconf session when polling all ports information,
        // use general power range of [-60, 60] instead.
        if (handler().get(DeviceService.class).getDevice(data().deviceId()).type()
                == Device.Type.FIBER_SWITCH) {
            return RANGE_GENERAL;
        }
        // hard-coded since there's no proper info in get-response
        if (getPortDir(port).equals(KEY_PORTDIRECT_RX)) {
            return null;
        }
        long minPower = (-11 * POWER_MULTIPLIER);
        long maxPower = (30 * POWER_MULTIPLIER);
        return Range.closed(minPower, maxPower);
    }

    private Range<Long> getTxPowerRange(PortNumber port, T component) {
        if (component instanceof Direction) {
            log.debug("Get target port{} power range...", port);
            return getPowerRange(port, KEY_PORTDIRECT_TX, KEY_PORTPWRCAPMINTX, KEY_PORTPWRCAPMAXTX);
        } else {
            log.debug("Get channel attenuation range...");
            return RANGE_ATT;
        }
    }

    private Range<Long> getRxPowerRange(PortNumber port, T component) {
        log.debug("Get input port{} power range...", port);
        return getPowerRange(port, KEY_PORTDIRECT_RX, KEY_PORTPWRCAPMINRX, KEY_PORTPWRCAPMAXRX);
    }




    private String getPortDir(PortNumber port) {
        String reply = netconfGet(handler(), getPortDirFilter(port));
        HierarchicalConfiguration info = configAt(reply, KEY_DATA_ORGOPENRDMDEV_CIRPACKS_PORTS_PORT);
        if (info == null) {
            return null;
        }
        return info.getString(KEY_PORTDIRECT);
    }

    private String outputToGain(PortNumber port, double powerOut) {
        // get active PA input port
        PortNumber activePort = acquireActivePort();
        if (activePort == null) { // this tells device is AMP, get corresponding Rx port
            activePort = PortNumber.portNumber(5 - port.toLong(), null);
        }
        double powerIn = (double) acquireCurrentPower(activePort, null, Direction.INGRESS);
        double gain = (powerOut - powerIn)/POWER_MULTIPLIER;
        /** 
         * with available testing devices, it seems that only PA is configurable,
         * and its acceptable target-gain range is different from spec in Yang,
         * for amp, it seems to be from 8.3 to 15
         * for ops, it seems to be from 6.8 to 13.8
         * Otherwise device sends error of "invalid argument".
         */
        return String.format("%.2f", gain);
    }

    private String getAmpName(PortNumber port) {
        String deviceType = getDeviceType();
        if (deviceType.equals(KEY_AMP)) {
            if (port.toLong() == 2 || port.toLong() == 3) {
                return KEY_BA;
            } else {
                return KEY_PA;
            }
        } else if (deviceType.equals(KEY_OPS)) {
            if (port.toLong() == 1 || port.toLong() == 4 || port.toLong() == 6) {
                return KEY_BA;
            } else {
                return KEY_PA;
            }
        }
        return null;
    }

    private String getDeviceType() {
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        List<Port> ports = deviceService.getPorts(handler().data().deviceId());
        if (ports.size() == 4) {
            return KEY_AMP;
        } else if (ports.size() == 6) {
            return KEY_OPS;
        } else {
            return null;
        }
    }

    private PortNumber acquireActivePort() {
        String filter = new StringBuilder(xmlOpen(KEY_OPTICALCONTROL_XMLNS))
                .append(xmlOpen(KEY_OPTICALMODULES))
                .append(xmlOpen(KEY_APS_XMLNS))
                .append(xmlOpen(KEY_APSMODULES))
                .append(xmlOpen(KEY_APSMODULE))
                .append(xmlOpen(KEY_STATE))
                .append(xmlEmpty(KEY_ACTIVEPATH))
                .append(xmlClose(KEY_STATE))
                .append(xmlClose(KEY_APSMODULE))
                .append(xmlClose(KEY_APSMODULES))
                .append(xmlClose(KEY_APS))
                .append(xmlClose(KEY_OPTICALMODULES))
                .append(xmlClose(KEY_OPTICALCONTROL))
                .toString();
        String reply = netconfGet(handler(), filter);
        log.debug("Service state replying, {}", reply);
        if (reply.contains(KEY_PRIMARY)) {
            return PORT_PRIMARY;
        } else if (reply.contains(KEY_SECONDARY)) {
            return PORT_SECONDARY;
        } else {
            return null;
        }
    }

    private String getTargetGainFilter(String name) {
        return new StringBuilder(xmlOpen(KEY_OPTICALCONTROL_XMLNS))
                .append(xml(KEY_WORKMODE, "auto"))
                .append(xmlOpen(KEY_OPTICALMODULES))
                .append(xmlOpen(KEY_OPTICALAMPLIFIER_XMLNS))
                .append(xmlOpen(KEY_AMPLIFIERS))
                .append(xmlOpen(KEY_AMPLIFIER))
                .append(xml(KEY_NAME, name))
                .append(xmlOpen(KEY_CONFIG))
                .append(xmlEmpty(KEY_TARGETGAIN))
                .append(xmlClose(KEY_CONFIG))
                .append(xmlClose(KEY_AMPLIFIER))
                .append(xmlClose(KEY_AMPLIFIERS))
                .append(xmlClose(KEY_OPTICALAMPLIFIER))
                .append(xmlClose(KEY_OPTICALMODULES))
                .append(xmlClose(KEY_OPTICALCONTROL))
                .toString();
    }

    // see comment in acquireCurrentPower(PortNumber, T)
    private String getCurrentPowerFilter(String name, String selection) {
        return new StringBuilder(xmlOpen(KEY_OPTICALCONTROL_XMLNS))
                .append(xml(KEY_WORKMODE, "auto"))
                .append(xmlOpen(KEY_OPTICALMODULES))
                .append(xmlOpen(KEY_OPTICALAMPLIFIER_XMLNS))
                .append(xmlOpen(KEY_AMPLIFIERS))
                .append(xmlOpen(KEY_AMPLIFIER))
                .append(xml(KEY_NAME, name))
                .append(xmlOpen(KEY_STATE))
                .append(xmlOpen(selection))
                .append(xmlEmpty(KEY_INSTANT))
                .append(xmlClose(selection))
                .append(xmlClose(KEY_STATE))
                .append(xmlClose(KEY_AMPLIFIER))
                .append(xmlClose(KEY_AMPLIFIERS))
                .append(xmlClose(KEY_OPTICALAMPLIFIER))
                .append(xmlClose(KEY_OPTICALMODULES))
                .append(xmlClose(KEY_OPTICALCONTROL))
                .toString();
    }

    private String getCurrentPowerFilter(PortNumber port) {
        return new StringBuilder(xmlOpen(KEY_ORGOPENRDMDEV_XMLNS))
                .append(xmlOpen(KEY_CIRPACKS))
                .append(xml(KEY_CIRPACKNAME, "cp-main-board"))
                .append(xmlOpen(KEY_PORTS))
                .append(xml(KEY_PORTID, Integer.toString((int)port.toLong())))
                .append(xmlOpen(KEY_PORT))
                .append(xmlEmpty(KEY_PORTCURPWR))
                .append(xmlClose(KEY_PORT))
                .append(xmlClose(KEY_PORTS))
                .append(xmlClose(KEY_CIRPACKS))
                .append(xmlClose(KEY_ORGOPENRDMDEV))
                .toString();
    }

    private String setTargetGainFilter(String name, String power) {
        return new StringBuilder(xmlOpen(KEY_OPTICALCONTROL_XMLNS))
                .append(xml(KEY_WORKMODE, "auto"))
                .append(xmlOpen(KEY_OPTICALMODULES))
                .append(xmlOpen(KEY_OPTICALAMPLIFIER_XMLNS))
                .append(xmlOpen(KEY_AMPLIFIERS))
                .append(xmlOpen(KEY_AMPLIFIER))
                .append(xml(KEY_NAME, name))
                .append(xmlOpen(KEY_CONFIG))
                .append(xml(KEY_TARGETGAIN, power))
                .append(xmlClose(KEY_CONFIG))
                .append(xmlClose(KEY_AMPLIFIER))
                .append(xmlClose(KEY_AMPLIFIERS))
                .append(xmlClose(KEY_OPTICALAMPLIFIER))
                .append(xmlClose(KEY_OPTICALMODULES))
                .append(xmlClose(KEY_OPTICALCONTROL))
                .toString();
    }

    private String getPowerRangeFilter(PortNumber port, String direction) {
        return new StringBuilder(xmlOpen(KEY_ORGOPENRDMDEV_XMLNS))
                .append(xmlOpen(KEY_CIRPACKS))
                .append(xml(KEY_CIRPACKNAME, "cp-main-board"))
                .append(xmlOpen(KEY_PORTS))
                .append(xml(KEY_PORTID, Integer.toString((int)port.toLong())))
                .append(xmlOpen(KEY_PORT))
                .append(xml(KEY_PORTDIRECT, direction))
                .append(xmlEmpty(KEY_PORTPROPERTY))
                .append(xmlClose(KEY_PORT))
                .append(xmlClose(KEY_PORTS))
                .append(xmlClose(KEY_CIRPACKS))
                .append(xmlClose(KEY_ORGOPENRDMDEV))
                .toString();
    }

    private String getPortDirFilter(PortNumber port) {
        return new StringBuilder(xmlOpen(KEY_ORGOPENRDMDEV_XMLNS))
                .append(xmlOpen(KEY_CIRPACKS))
                .append(xml(KEY_CIRPACKNAME, "cp-main-board"))
                .append(xmlOpen(KEY_PORTS))
                .append(xml(KEY_PORTID, Integer.toString((int)port.toLong())))
                .append(xmlOpen(KEY_PORT))
                .append(xmlEmpty(KEY_PORTDIRECT))
                .append(xmlClose(KEY_PORT))
                .append(xmlClose(KEY_PORTS))
                .append(xmlClose(KEY_CIRPACKS))
                .append(xmlClose(KEY_ORGOPENRDMDEV))
                .toString();
    }

    // FIXME below are filters that are not updated, no longer usable
    private String getChannelPowerFilter(PortNumber port, OchSignal channel) {
        return new StringBuilder(xmlOpen(KEY_OPENOPTICALDEV_XMLNS))
                .append(xmlOpen(KEY_OCMS))
                .append(xml(KEY_PORTID, Double.toString(port.toLong())))
                .append(xmlOpen(KEY_OCMSTATS))
                .append(xml(KEY_CHNUM, Integer.toString(channel.spacingMultiplier())))
                .append(xmlEmpty(KEY_CHSTATS))
                .append(xmlClose(KEY_OCMSTATS))
                .append(xmlClose(KEY_OCMS))
                .append(xmlClose(KEY_OPENOPTICALDEV))
                .toString();
    }

    private String getChannelAttenuationFilter(PortNumber port, OchSignal channel) {
        return new StringBuilder(xmlOpen(KEY_OPENOPTICALDEV_XMLNS))
                .append(xmlOpen(KEY_CONNS))
                .append(xml(KEY_CONNID, Integer.toString(channel.spacingMultiplier())))
                .append(xmlEmpty(KEY_CHATT))
                .append(xmlClose(KEY_CONNS))
                .append(xmlClose(KEY_OPENOPTICALDEV))
                .toString();
    }
}
