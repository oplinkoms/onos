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

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.onosproject.drivers.oplink.OplinkNetconfUtility.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Flow rule programmable behaviour for oplink optical netconf devices.
 */
public class OplinkOpticalFlowRuleProgrammable
        extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    // key
    public static final String KEY_CHID = "wavelength-id";
    public static final String KEY_SRC = "source";
    public static final String KEY_DST = "destination";

    private static final String KEY_WAVELENGTHROUTER = "wavelength-router";
    private static final String KEY_XMLNS_WAVELENGTHROUTER = "xmlns=\"http://openconfig.net/yang/wavelength-router\"";
    private static final String KEY_WAVELENGTHROUTER_XMLNS = String.format("%s %s", KEY_WAVELENGTHROUTER, KEY_XMLNS_WAVELENGTHROUTER);
    private static final String KEY_MEDIACHANNELS = "media-channels";
    private static final String KEY_CHANNEL = "channel";
    private static final String KEY_DATA_WAVELENGTHROUTER_MEDIACHANNELS_CHANNEL = String.format("%s.%s.%s.%s", KEY_DATA, KEY_WAVELENGTHROUTER, KEY_MEDIACHANNELS, KEY_CHANNEL);
    private static final String KEY_CONFIG = "config";
    private static final String KEY_INDEX = "index";
    private static final String KEY_NAME = "name";
    private static final String KEY_SRC_CONFIG_PORTNAME = String.format("%s.%s.%s", KEY_SRC, KEY_CONFIG, KEY_PORTNAME);
    private static final String KEY_DST_CONFIG_PORTNAME = String.format("%s.%s.%s", KEY_DST, KEY_CONFIG, KEY_PORTNAME);
    private static final String KEY_LOWFREQ = "lower-frequency";
    private static final String KEY_UPFREQ = "upper-frequency";
    private static final String KEY_ADMINSTAT = "admin-status";
    private static final String KEY_ATTENCTRLMODE = "attenuation-control-mode";
    private static final String KEY_SPECPOWERPROF = "spectrum-power-profile";
    private static final String KEY_DIST = "distribution";
    private static final String KEY_ATTENVAL = "attenuation-value";
    private static final String KEY_XMLNS_ALIWAVELEN = "xmlns=\"http://com/alibaba/wavelength-router-ext\"";
    private static final String KEY_ATTENVAL_XMLNS = String.format("%s %s", KEY_ATTENVAL, KEY_XMLNS_ALIWAVELEN);


    // hard-coded entries
    private static final String KEY_SETATTEN = "ext:SET-ATTENUATION";


    // log
    private static final Logger log = getLogger(OplinkOpticalFlowRuleProgrammable.class);

    @Override
    public Collection<FlowEntry> getFlowEntries() {
        return parseConnections();
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        return applyConnections(rules);
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        return removeConnections(rules);
    }

    private String getChannelsFilter() {
        return new StringBuilder(xmlOpen(KEY_WAVELENGTHROUTER_XMLNS))
                .append(xmlOpen(KEY_MEDIACHANNELS))
                .append(xmlEmpty(KEY_CHANNEL))
                .append(xmlClose(KEY_MEDIACHANNELS))
                .append(xmlClose(KEY_WAVELENGTHROUTER))
                .toString();
    }

    private Collection<FlowEntry> parseConnections() {
        log.debug("Fetch connections...");
        String reply = netconfGet(handler(), getChannelsFilter());
        List<HierarchicalConfiguration> subtrees = configsAt(reply, KEY_DATA_WAVELENGTHROUTER_MEDIACHANNELS_CHANNEL);
        Collection<FlowEntry> list = new ArrayList<>();
        for (HierarchicalConfiguration connection : subtrees) {
            list.add(new DefaultFlowEntry(parseConnection(connection), FlowEntry.FlowEntryState.ADDED));
        }
        return list;
    }

    private FlowRule parseConnection(HierarchicalConfiguration cfg) {
        return OplinkOpticalUtility.toFlowRule(this,
                                               PortNumber.portNumber(cfg.getString(KEY_SRC_CONFIG_PORTNAME)),
                                               PortNumber.portNumber(cfg.getString(KEY_DST_CONFIG_PORTNAME)),
                                               cfg.getInt(KEY_INDEX));

    }

    private Collection<FlowRule> applyConnections(Collection<FlowRule> rules) {
        return rules.stream()
                .filter(c -> applyConnection(c))
                .collect(Collectors.toList());
    }

    private boolean applyConnection(FlowRule rule) {
        log.debug("Applying connection {}", rule);
        OplinkCrossConnect crossConnect = OplinkOpticalUtility.fromFlowRule(this, rule);
        // Build xml
        Integer connID = crossConnect.getChannel(); // TODO verify this ID is the right one
        String connIDStr = Integer.toString(connID);
        String channelName;
        if (connID <= 512)
            channelName = "wss-1-" + connID;
        else
            channelName = "wss-2-" + (connID - 512);
        String upperFrequency = "" + (1914 + connID)*100000;
        String lowerFrequency = "" + (1913 + connID)*100000;
        // See the end of this file for expected xml output
        String cfg = new StringBuilder(xmlOpen(KEY_WAVELENGTHROUTER_XMLNS))
                .append(xmlOpen(KEY_MEDIACHANNELS))
                .append(xmlOpen(KEY_CHANNEL))
                .append(xml(KEY_INDEX, connIDStr))
                .append(xmlOpen(KEY_CONFIG))
                .append(xml(KEY_INDEX, connIDStr))
                .append(xml(KEY_NAME, channelName))
                .append(xml(KEY_LOWFREQ, lowerFrequency))
                .append(xml(KEY_UPFREQ, upperFrequency))
                .append(xml(KEY_ATTENCTRLMODE, "ext:SET-ATTENUATION"))
                .append(xml(KEY_ADMINSTAT, "ENABLED"))
                .append(xmlClose(KEY_CONFIG))
                .append(xmlOpen(KEY_SRC))
                .append(xmlOpen(KEY_CONFIG))
                // TODO verify that the port has name
                .append(xml(KEY_PORTNAME, crossConnect.getInPort().name()))
                .append(xmlClose(KEY_CONFIG))
                .append(xmlClose(KEY_SRC))
                .append(xmlOpen(KEY_DST))
                .append(xmlOpen(KEY_CONFIG))
                // TODO verify that the port has name
                .append(xml(KEY_PORTNAME, crossConnect.getOutPort().name()))
                .append(xmlClose(KEY_CONFIG))
                .append(xmlClose(KEY_DST))
                .append(xmlOpen(KEY_SPECPOWERPROF))
                .append(xmlOpen(KEY_DIST))
                .append(xml(KEY_LOWFREQ, lowerFrequency))
                .append(xml(KEY_UPFREQ, upperFrequency))
                .append(xmlOpen(KEY_CONFIG))
                .append(xml(KEY_LOWFREQ, lowerFrequency))
                .append(xml(KEY_UPFREQ, upperFrequency))
                .append(xmlOpen(KEY_ATTENVAL_XMLNS))
                .append(crossConnect.getAttenuation())
                .append(xmlClose(KEY_ATTENVAL))
                .append(xmlClose(KEY_CONFIG))
                .append(xmlClose(KEY_DIST))
                .append(xmlClose(KEY_SPECPOWERPROF))
                .append(xmlClose(KEY_CHANNEL))
                .append(xmlClose(KEY_MEDIACHANNELS))
                .append(xmlClose(KEY_WAVELENGTHROUTER))
                .toString();
        return netconfEditConfig(handler(), CFG_MODE_MERGE, cfg);
    }

    private Collection<FlowRule> removeConnections(Collection<FlowRule> rules) {
        return rules.stream()
                .filter(c -> removeConnection(c))
                .collect(Collectors.toList());
    }

    private boolean removeConnection(FlowRule rule) {
        log.debug("Removing connection {}", rule);
        OplinkCrossConnect crossConnect = OplinkOpticalUtility.fromFlowRule(this, rule);
        // Build xml
        String connID = Integer.toString(crossConnect.getChannel());
        String cfg = new StringBuilder(xmlOpen(KEY_WAVELENGTHROUTER_XMLNS))
                .append(xmlOpen(KEY_MEDIACHANNELS))
                .append(xmlOpen(String.format("%s %s", KEY_CHANNEL, CFG_OPT_DELETE)))
                .append(xml(KEY_INDEX, connID))
//                .append(xmlOpen(KEY_SRC))
//                .append(xml(KEY_PORTID, crossConnect.getInPort().name()))
//                .append(xml(KEY_CHID, connID))
//                .append(xmlClose(KEY_SRC))
                .append(xmlClose(KEY_CHANNEL))
                .append(xmlClose(KEY_MEDIACHANNELS))
                .append(xmlClose(KEY_WAVELENGTHROUTER))
                .toString();
        return netconfEditConfig(handler(), CFG_MODE_NONE, cfg);
    }
}

// adapted from https://github.com/oplinkoms/yang-model/blob/new_sysrepo/oplink/optical/openconfig-wavelength-router-startup-MEIWU_100GHz_example.xml
// <channel>
//			<index>1</index>
//			<config>
//				<index>1</index>
//				<name>wss-1-1</name>
//				<lower-frequency>191400000</lower-frequency>
//				<upper-frequency>191500000</upper-frequency>
//				<attenuation-control-mode>ext:SET-ATTENUATION</attenuation-control-mode>
//				<admin-status>ENABLED</admin-status>
//			</config>
//			<source>
//				<config>
//					<port-name>In01</port-name>
//				</config>
//			</source>
//			<dest>
//				<config>
//					<port-name>Add1_Com1</port-name>
//				</config>
//			</dest>
//			<spectrum-power-profile>
//				<distribution>
//					<lower-frequency>191400000</lower-frequency>
//					<upper-frequency>191500000</upper-frequency>
//					<config>
//						<lower-frequency>191400000</lower-frequency>
//						<upper-frequency>191500000</upper-frequency>
//						<attenuation-value xmlns="http://com/alibaba/wavelength-router-ext">0.00</attenuation-value>
//					</config>
//				</distribution>
//			</spectrum-power-profile>
//		</channel>
//
// wss-1-index for index <= 512
// wss-2-(index-512) for index > 512
// upper-frequency = 191400000 + index*100000
// lower-frequency = 191300000 + index*100000