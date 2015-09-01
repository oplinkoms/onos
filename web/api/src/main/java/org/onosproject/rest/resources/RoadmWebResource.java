package org.onosproject.rest.resources;

import static org.onlab.util.Tools.nullIsNotFound;
import static org.onosproject.net.DeviceId.deviceId;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;
import org.onosproject.rest.AbstractWebResource;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Path("roadm")
public class RoadmWebResource extends AbstractWebResource {

    final DeviceService service = get(DeviceService.class);
    public static final String DEVICE_NOT_FOUND = "Device is not found";

    /**
     * Get port power value.
     * Returns port power value
     *
     * @param id device identifier
     * @return 200 OK
     */
    @GET
    @Path("{id}/port")
    public Response getRoadmPortPower(@PathParam("id") String id) {

        Device device = nullIsNotFound(service.getDevice(deviceId(id)), DEVICE_NOT_FOUND);

        ObjectNode result = codec(Device.class).encode(device, this);

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
    public Response setRoadmPortPower(@PathParam("id") String id) {

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
