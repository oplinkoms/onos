package org.onosproject.rest.resources;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsNotFound;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.onosproject.net.DeviceId.deviceId;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

// import java.io.IOException;
import java.io.InputStream;
// import java.net.URI;
// import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.rest.AbstractWebResource;

import org.onlab.util.ItemNotFoundException;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Path("roadm")
public class RoadmWebResource extends AbstractWebResource {

    final DeviceService deviceService = get(DeviceService.class);
    final FlowRuleService flowService = get(FlowRuleService.class);
    public static final String DEVICE_NOT_FOUND = "Device is not found";

    /**
     * Get all infrastructure ROADM devices. Returns array of ROADM devices in the system.
     *
     * @return array of all the ROADM devices in the system
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevices() {
        final ObjectNode root = mapper().createObjectNode();
        final ArrayNode devicesNode = root.putArray("devices");
        final Iterable<Device> devices = deviceService.getDevices();
        for (final Device device : devices) {
            if (device != null && device.type() == Device.Type.ROADM) {
                devicesNode.add(codec(Device.class).encode(device, this));
            }
        }

        return ok(root).build();
    }

    /**
     * Get ports of infrastructure ROADM device.
     * Returns details of the specified infrastructure ROADM device.
     *
     * @param id Roadm device identifier
     * @return 200 OK
     */
    @GET
    @Path("{id}/ports")
    public Response getRoadmPorts(@PathParam("id") String id) {
        Device device = nullIsNotFound(
            deviceService.getDevice(deviceId(id)), DEVICE_NOT_FOUND);
        List<Port> ports = checkNotNull(
            deviceService.getPorts(deviceId(id)), "Ports could not be retrieved");
        ObjectNode result = codec(Device.class).encode(device, this);
        result.set("ports", codec(Port.class).encode(ports, this));
        return ok(result).build();
    }

    /**
     * Get details of the specified port.
     * Returns pdetails of the specified port
     *
     * @param id device identifier
     * @param num port identifier
     * @return 200 OK
     */
    @GET
    @Path("{id}/port{num}")
    public Response getRoadmPortDesc(@PathParam("id") String id,
                                      @PathParam("num") long num) {

        Device device = nullIsNotFound(
            deviceService.getDevice(deviceId(id)), DEVICE_NOT_FOUND);
        Port port = checkNotNull(deviceService.getPort(deviceId(id),
                PortNumber.portNumber(num)), "Port could not be retrieved");
        ObjectNode result = codec(Port.class).encode(port, this);

        return ok(result).build();
    }

    /**
     * Get port power value.
     * Returns port power value
     *
     * @param id device identifier
     * @return 200 OK
     */
    @GET
    @Path("{id}/port{num}/power")
    public Response getRoadmPortPower(@PathParam("id") String id,
                                      @PathParam("num") long num) {

        Device device = nullIsNotFound(
            deviceService.getDevice(deviceId(id)), DEVICE_NOT_FOUND);
        Port port = checkNotNull(deviceService.getPort(deviceId(id),
                PortNumber.portNumber(num)), "Port could not be retrieved");
        ObjectNode result = mapper().createObjectNode();
        result.put("power", port.annotations().value("power"));
        return ok(result).build();
    }

    /**
     * Set port power value.
     *
     * @param id device identifier
     * @return 200 OK
     */
    @POST
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setRoadmPortPower(@PathParam("id") String id,
                                      InputStream input) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(input);
            deviceService.setPortPower(deviceId(id),
                    PortNumber.portNumber(nullIsIllegal(jsonTree.get("port"),
                        "port member is required").asInt()),
                    (float) nullIsIllegal(jsonTree.get("power"),
                        "power member is required").asDouble());
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).entity(e.toString()).build();
        }
    }

    /**
     * Get all channel power value.
     * Returns all channel power value
     *
     * @param did device identifier
     * @return 200 OK
     */
    @GET
    @Path("{did}/channels")
    public Response getRoadmAllChannelPower(@PathParam("did") String did) {

        final Iterable<FlowEntry> flowEntries =
                flowService.getFlowEntries(deviceId(did));

        if (!flowEntries.iterator().hasNext()) {
            throw new ItemNotFoundException(DEVICE_NOT_FOUND);
        }
        ObjectNode result = mapper().createObjectNode();
        final ArrayNode node = result.putArray("Packets of flow and power");
        for (final FlowEntry entry : flowEntries) {
            ObjectNode flow = codec(FlowEntry.class).encode(entry, this);
            flow.put("power", flowService.getFlowOuputPower(
                    deviceId(did), entry.id()));
            node.add(flow);
            // result.put(entry.id().value(), codec(FlowEntry.class).encode(entry, this));
            // result.put("power", flowService.getFlowOuputPower(
            //         deviceId(did), FlowId.valueOf(entry.id())));
        }
        return ok(result).build();
    }

    /**
     * Get channel power value.
     * Returns channel power value
     *
     * @param id device identifier
     * @return 200 OK
     */
    @GET
    @Path("{did}/{fid}/power")
    public Response getRoadmChannelPower(@PathParam("did") String did,
                                         @PathParam("fid") long fid) {

        //Float value = new Float(flowService.getFlowOuputPower(deviceId(did),
        //    FlowId.valueOf(fid)));
        float value = flowService.getFlowOuputPower(deviceId(did),
            FlowId.valueOf(fid));
        ObjectNode result = mapper().createObjectNode();
        result.put("power", value);
        return ok(result).build();
    }

    /**
     * Set channel power value.
     *
     * @param id device identifier
     * @return 200 OK
     */
    @PUT
    @Path("{id}/channel")
    public Response setRoadmChannelPower(@PathParam("id") String id) {

        Device device = nullIsNotFound(
            deviceService.getDevice(deviceId(id)), DEVICE_NOT_FOUND);

        ObjectNode result = codec(Device.class).encode(device, this);

        return ok(result).build();
    }
}
