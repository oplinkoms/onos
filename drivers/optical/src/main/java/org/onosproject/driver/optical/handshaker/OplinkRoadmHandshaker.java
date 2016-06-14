/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.driver.optical.handshaker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openflow.controller.OpenFlowOpticalSwitch;
import org.onosproject.openflow.controller.PortDescPropertyType;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.onosproject.openflow.controller.Dpid;
import org.projectfloodlight.openflow.protocol.OFCircuitPortStatus;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsReply;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsRequest;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFObject;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortOptical;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFExpPortAdjacencyRequest;
import org.projectfloodlight.openflow.protocol.OFExperimenterStatsReply;
import org.projectfloodlight.openflow.protocol.OFExpOpticalExtReply;
import org.projectfloodlight.openflow.protocol.OFExpPortAdjacencyReply;
import org.projectfloodlight.openflow.protocol.OFExpPortAdidOtn;

/**
 * Driver for Oplink single WSS 8D ROADM.
 *
 * Driver implements custom handshaker and supports for Optical channel Port based on OpenFlow OTN extension.
 * The device consists of Och ports, and performances wavelength cross-connect among the ports.
 */
public class OplinkRoadmHandshaker extends AbstractOpenFlowSwitch implements OpenFlowOpticalSwitch {
    private final AtomicBoolean driverHandshakeComplete = new AtomicBoolean(false);
    private List<OFPortOptical> opticalPorts;
    private static final ProviderId PID =
            new ProviderId("oplink", "org.onosproject.driver.handshaker", true);

    @Override
    public List<? extends OFObject> getPortsOf(PortDescPropertyType type) {
        return ImmutableList.copyOf(opticalPorts);
    }

    @Override
    /**
     * Returns a list of standard (Ethernet) ports.
     *
     * @return List of ports
     */
    public List<OFPortDesc> getPorts() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Set<PortDescPropertyType> getPortTypes() {
        return ImmutableSet.of(PortDescPropertyType.OPTICAL_TRANSPORT);
    }

    @Override
    public Boolean supportNxRole() {
        return false;
    }

    @Override
    public void startDriverHandshake() {
        log.warn("Starting driver handshake for sw {}", getStringId());
        if (startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        }
        startDriverHandshakeCalled = true;
        try {
            sendHandshakeOFExperimenterPortDescRequest();
        } catch (IOException e) {
            log.error("OPLK ROADM exception while sending experimenter port desc:", e);
        }
    }

    @Override
    public boolean isDriverHandshakeComplete() {
        return driverHandshakeComplete.get();
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {

        if (!startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeNotStarted();
        }

        if (driverHandshakeComplete.get()) {
            throw new SwitchDriverSubHandshakeCompleted(m);
        }

        switch (m.getType()) {
            case BARRIER_REPLY:
                log.debug("OPLK ROADM Received barrier response");
                break;
            case ERROR:
                log.error("Switch {} Error {}", getStringId(), m);
                break;
            case FEATURES_REPLY:
                break;
            case FLOW_REMOVED:
                break;
            case GET_ASYNC_REPLY:
                break;
            case PACKET_IN:
                break;
            case PORT_STATUS:
                processOFPortStatus((OFCircuitPortStatus) m);
                break;
            case QUEUE_GET_CONFIG_REPLY:
                break;
            case ROLE_REPLY:
                break;
            case STATS_REPLY:
                OFStatsReply stats = (OFStatsReply) m;
                if (stats.getStatsType() == OFStatsType.EXPERIMENTER) {
                    log.warn("OPLK ROADM : Received multipart (port desc) reply message {}", m);
                    //OTN Optical extension 1.0 port-desc
                    createOpticalPortList((OFCircuitPortsReply) m);
                    driverHandshakeComplete.set(true);
                }
                break;
            default:
                log.warn("Received message {} during switch-driver " +
                                 "subhandshake " + "from switch {} ... " +
                                 "Ignoring message", m,
                         getStringId());
                break;
        }
    }

    private void processOFPortStatus(OFCircuitPortStatus ps) {
        log.info("OPLK ROADM ..OF Port Status :", ps);
    }

    @Override
    public Device.Type deviceType() {
        return Device.Type.ROADM;
    }

    @Override
    public final void sendMsg(OFMessage m) {
        OFMessage newMsg = m;
        //Stub for later enhancement.
        if (m.getType() == OFType.STATS_REQUEST) {
            OFStatsRequest osr = (OFStatsRequest) m;
            switch (osr.getStatsType()) {
                case PORT:
                    //Send original message first
                    super.sendMsg(m);
                    //Send experiment message
                    OFExpPortAdjacencyRequest request = this.factory().buildExpPortAdjacencyRequest()
                    .setXid(osr.getXid())
                    .setFlags(osr.getFlags())
                    .build();
                    newMsg = request;
                    // log.info("Send OFOplinkPortAdjacencyRequest message: {}", request);
                    break;
                default:
                    break;
            }
        }
        super.sendMsg(newMsg);
    }

    private void sendHandshakeOFExperimenterPortDescRequest() throws IOException {
        // send multi part message for port description for optical switches
        OFCircuitPortsRequest circuitPortsRequest = factory()
                .buildCircuitPortsRequest().setXid(getNextTransactionId())
                .build();
        log.info("OPLK ROADM : Sending experimented circuit port stats " +
                 "message " +
                 "{}",
                 circuitPortsRequest);
        this.sendHandshakeMessage(circuitPortsRequest);
    }

    /**
     * Builds list of OFPortOptical ports based on the multi-part circuit ports reply.
     * Ensure the optical transport port's signal type is configured correctly.
     *
     * @param wPorts OF reply with circuit ports
     */
    private void createOpticalPortList(OFCircuitPortsReply wPorts) {
        opticalPorts = new ArrayList<>();
        opticalPorts.addAll(wPorts.getEntries());
    }

    @Override
    public void processExperimenterStats(OFMessage msg) {
        //TODO: add process experimenter message here
        switch ((int) ((OFExperimenterStatsReply) msg).getExperimenter()) {
            case 0xFF000007:
                // Process msg that experimenter = 0xff000007L
                OFExpOpticalExtReply optExtReply = (OFExpOpticalExtReply) msg;
                if (optExtReply.getSubtype() == 2) {
                    portAdjacencyDiscovery((OFExpPortAdjacencyReply) msg);
                }
                break;
            case 0xff000088:
                // Process msg that experimenter = 0xff000088L
                break;
            default:
                break;
        }
    }

    private void portAdjacencyDiscovery(OFExpPortAdjacencyReply msg) {
        LinkProviderRegistry registry = checkNotNull(handler().get(LinkProviderRegistry.class));
        LinkProvider provider = new OplinkLinkProvider();
        try {
            LinkProviderService providerService = registry.register(provider);
            // log.info(reply);
            msg.getEntries().forEach(
                ad -> {
                    // Current node information
                    ConnectPoint dst = new ConnectPoint(handler().data().deviceId(),
                            PortNumber.portNumber(ad.getPortNo().getPortNumber()));
                    ad.getProperties().forEach(
                        adid -> {
                            List<OFExpPortAdidOtn> otns = adid.getAdids();
                            if (otns != null && otns.size() > 0) {
                                OFExpPortAdidOtn otn = otns.get(0);
                                // ITU-T G.7714 ETH MAC Format
                                ChannelBuffer buffer = ChannelBuffers.buffer(16);
                                otn.getOpspecEth().write16Bytes(buffer);
                                long mac = buffer.getLong(2) << 4 >>> 16;
                                int port = (int) (buffer.getLong(8) << 4 >>> 32);
                                ConnectPoint src = new ConnectPoint(
                                        DeviceId.deviceId(Dpid.uri(new Dpid(mac))),
                                        PortNumber.portNumber(port));
                                // Create optical links
                                providerService.linkDetected(
                                        new DefaultLinkDescription(src, dst, Link.Type.OPTICAL));
                                providerService.linkDetected(
                                        new DefaultLinkDescription(dst, src, Link.Type.OPTICAL));
                            }
                        }
                    );
                }
            );
        } finally {
            registry.unregister(provider);
        }
    }

    // Token provider entity
    private static final class OplinkLinkProvider implements LinkProvider {
        @Override
        public ProviderId id() {
            return PID;
        }
    }
}
