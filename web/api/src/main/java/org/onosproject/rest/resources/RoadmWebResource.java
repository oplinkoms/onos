package org.onosproject.rest.resources;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsNotFound;
import static org.onosproject.net.DeviceId.deviceId;

import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.rest.AbstractWebResource;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Path("roadm")
public class RoadmWebResource extends AbstractWebResource {

    final DeviceService service = get(DeviceService.class);
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
        final Iterable<Device> devices = get(DeviceService.class).getDevices();
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
        DeviceService service = get(DeviceService.class);
        Device device = nullIsNotFound(service.getDevice(deviceId(id)), DEVICE_NOT_FOUND);
        List<Port> ports = checkNotNull(service.getPorts(deviceId(id)), "Ports could not be retrieved");
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

        Device device = nullIsNotFound(service.getDevice(deviceId(id)), DEVICE_NOT_FOUND);
        Port port = checkNotNull(service.getPort(deviceId(id),
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

        Device device = nullIsNotFound(service.getDevice(deviceId(id)), DEVICE_NOT_FOUND);
        Port port = checkNotNull(service.getPort(deviceId(id),
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
    @PUT
    @Path("{id}/port")
    public Response setRoadmPortPower(@PathParam("id") String id,  InputStream stream) {

        Device device = nullIsNotFound(service.getDevice(deviceId(id)), DEVICE_NOT_FOUND);

        ObjectNode result = codec(Device.class).encode(device, this);

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
    @Path("{id}/channel")
    public Response getRoadmChannelPower(@PathParam("id") String id) {

        Device device = nullIsNotFound(service.getDevice(deviceId(id)), DEVICE_NOT_FOUND);

        ObjectNode result = codec(Device.class).encode(device, this);

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

        Device device = nullIsNotFound(service.getDevice(deviceId(id)), DEVICE_NOT_FOUND);

        ObjectNode result = codec(Device.class).encode(device, this);

        return ok(result).build();
    }
}
