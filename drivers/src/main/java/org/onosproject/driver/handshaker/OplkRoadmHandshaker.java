package org.onosproject.driver.handshaker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.onosproject.net.Device;
import org.onosproject.openflow.controller.OpenFlowOpticalSwitch;
import org.onosproject.openflow.controller.PortDescPropertyType;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.projectfloodlight.openflow.protocol.OFCircuitPortStatus;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsReply;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsRequest;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFObject;
import org.projectfloodlight.openflow.protocol.OFOplinkChannelPowerRequest;
import org.projectfloodlight.openflow.protocol.OFOplinkPortPowerRequest;
import org.projectfloodlight.openflow.protocol.OFPortOptical;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFType;

public class OplkRoadmHandshaker extends AbstractOpenFlowSwitch implements OpenFlowOpticalSwitch {
    private final AtomicBoolean driverHandshakeComplete = new AtomicBoolean(false);
    private long barrierXidToWaitFor = -1;
    private List<OFPortOptical> opticalPorts;

    @Override
    public List<? extends OFObject> getPortsOf(PortDescPropertyType type) {
        return ImmutableList.copyOf(opticalPorts);
    }

    @Override
    public Set<PortDescPropertyType> getPortTypes() {
        return ImmutableSet.of(PortDescPropertyType.OPTICAL_TRANSPORT);
    }

    @Override
    public Boolean supportNxRole() {
        // TODO Auto-generated method stub
        return null;
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
            log.error("OPLK ROADM exception while sending experimenter port desc:",
                     e.getMessage());
            e.printStackTrace();
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
                if (m.getXid() == barrierXidToWaitFor) {
                    log.debug("OPLK ROADM Received barrier response");
                }
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

        }
    }

    public void processOFPortStatus(OFCircuitPortStatus ps) {
        log.debug("OPLK ROADM ..OF Port Status :", ps);
    }

    @Override
    public Device.Type deviceType() {
        return Device.Type.ROADM;
    }

    @Override
    public final void sendMsg(OFMessage m) {
        OFMessage newMsg = m;

        if (m.getType() == OFType.STATS_REQUEST) {
            OFStatsRequest sr = (OFStatsRequest) m;
            log.debug("Oplink rebuilding stats request type {}", sr.getStatsType());
            switch (sr.getStatsType()) {
                case FLOW:
                    //send the original message first
                    super.sendMsg(m);

                    //Then create Oplink experiment stats message
                    OFOplinkChannelPowerRequest chRequest = this.factory().buildOplinkChannelPowerRequest()
                            .setXid(sr.getXid())
                            .setFlags(sr.getFlags())
                            .build();
                    newMsg = chRequest;
                    break;
                case PORT:
                    OFOplinkPortPowerRequest pRequest = this.factory().buildOplinkPortPowerRequest()
                    .setXid(sr.getXid())
                    .setFlags(sr.getFlags())
                    .build();
                    newMsg = pRequest;
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
        log.warn("OPLK ROADM : Sending experimented circuit port stats " +
                 "message " +
                 "{}",
                 circuitPortsRequest.toString());
        this.sendHandshakeMessage(circuitPortsRequest);
    }

    /**
     * Builds list of OFPortOptical ports based on the multi-part circuit ports reply.
     *
     * Ensure the optical transport port's signal type is configured correctly.
     *
     * @param wPorts OF reply with circuit ports
     */
    private void createOpticalPortList(OFCircuitPortsReply wPorts) {
        opticalPorts = new ArrayList<>();
        opticalPorts.addAll(wPorts.getEntries());
    }
}
