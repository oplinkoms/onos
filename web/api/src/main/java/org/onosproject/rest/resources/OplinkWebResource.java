/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.rest.resources;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.Device;
import org.onosproject.net.Direction;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.List;

import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsNotFound;
import static org.onosproject.net.DeviceId.deviceId;

/**
 * Manage inventory of infrastructure devices.
 */
@Path("oplink")
public class OplinkWebResource extends AbstractWebResource {

    @Context
    private UriInfo uriInfo;

    private static final String DEVICE_NOT_FOUND = "Device is not found";

    /**
     * Gets all infrastructure devices.
     * Returns array of all discovered infrastructure devices.
     *
     * @return 200 OK with a collection of devices
     * @onos.rsModel DevicesGet
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevices() {
        Iterable<Device> devices = get(DeviceService.class).getDevices();
        return ok(encodeArray(Device.class, "devices", devices)).build();
    }

    /**
     * Gets details of infrastructure device.
     * Returns details of the specified infrastructure device.
     *
     * @param id device identifier
     * @return 200 OK with a device
     * @onos.rsModel DeviceGet
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevice(@PathParam("id") String id) {
        Device device = nullIsNotFound(get(DeviceService.class).getDevice(deviceId(id)),
                                       DEVICE_NOT_FOUND);
        return ok(codec(Device.class).encode(device, this)).build();
    }

    /**
     * Removes infrastructure device.
     * Administratively deletes the specified device from the inventory of
     * known devices.
     *
     * @param id device identifier
     * @return 200 OK with the removed device
     */
    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeDevice(@PathParam("id") String id) {
        Device device = nullIsNotFound(get(DeviceService.class).getDevice(deviceId(id)),
                                       DEVICE_NOT_FOUND);
        get(DeviceAdminService.class).removeDevice(deviceId(id));
        return ok(codec(Device.class).encode(device, this)).build();
    }

    /**
     * Gets ports of infrastructure device.
     * Returns details of the specified infrastructure device.
     *
     * @onos.rsModel DeviceGetPorts
     * @param id device identifier
     * @return 200 OK with a collection of ports of the given device
     */
    @GET
    @Path("{id}/ports")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevicePorts(@PathParam("id") String id) {
        DeviceService service = get(DeviceService.class);
        Device device = nullIsNotFound(service.getDevice(deviceId(id)), DEVICE_NOT_FOUND);
        List<Port> ports = checkNotNull(service.getPorts(deviceId(id)), "Ports could not be retrieved");
        ObjectNode result = codec(Device.class).encode(device, this);
        result.set("ports", codec(Port.class).encode(ports, this));
        return ok(result).build();
    }

    @GET
    @Path("{id}/power")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevicePower(@PathParam("id") String id) {
        DeviceService service = get(DeviceService.class);
        Device device = nullIsNotFound(service.getDevice(deviceId(id)), DEVICE_NOT_FOUND);
        List<Port> ports = checkNotNull(service.getPorts(deviceId(id)), "Ports could not be retrieved");
        PowerConfig<Direction> powerConfig = device.as(PowerConfig.class);
        ArrayNode arrayNode = mapper().createArrayNode();
        ports.forEach(port -> {
            ObjectNode node = mapper().createObjectNode()
                .put("port", port.number().toLong())
                .put("current power",
                    checkNotNull(powerConfig.currentPower(port.number(), Direction.ALL).get(), "null"));
            arrayNode.add(node);
        });
        ObjectNode result = mapper().createObjectNode();
        result.set("port power", arrayNode);
        return ok(result).build();
    }

    @GET
    @Path("{id}/{port}/power")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevicePower(@PathParam("id") String id,
                                   @PathParam("port") int port) {
        PortNumber portNumber = PortNumber.portNumber(port);
        DeviceService service = get(DeviceService.class);
        Device device = nullIsNotFound(service.getDevice(deviceId(id)), DEVICE_NOT_FOUND);
        PowerConfig<Direction> powerConfig = device.as(PowerConfig.class);
        // Long targetPower = powerConfig.getTargetPower(portNumber, Direction.ALL).get();
        Long currentPower = powerConfig.currentPower(portNumber, Direction.ALL).get();
        ObjectNode result = mapper().createObjectNode()
                .put("current power", checkNotNull(currentPower, "null"));
        return ok(result).build();
    }

    @POST
    @Path("{id}/power")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setDevicePower(@PathParam("id") String id,
                               InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            PortNumber portNumber = PortNumber.portNumber(jsonTree.get("port").asInt());
            Long power = (long) jsonTree.get("power").asInt();
            DeviceService service = get(DeviceService.class);
            Device device = nullIsNotFound(service.getDevice(deviceId(id)), DEVICE_NOT_FOUND);
            PowerConfig<Direction> powerConfig = device.as(PowerConfig.class);
            powerConfig.setTargetPower(portNumber, Direction.ALL, power);
            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path("oplink")
                    .path(id);
            return Response
                    .created(locationBuilder.build())
                    .build();
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
