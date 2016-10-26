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
package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Direction;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;

import static org.onosproject.net.ChannelSpacing.CHL_12P5GHZ;
import static org.onosproject.net.GridType.DWDM;

/**
 * Command that gets the configuration of the specified type from the specified
 * device. If configuration cannot be retrieved it prints an error string.
 *
 * This is a temporary development tool for use until yang integration is complete.
 * This uses a not properly specified behavior. DO NOT USE AS AN EXAMPLE.
 */

@Command(scope = "onos", name = "oplink-netconf",
        description = "Debug command for oplink netconf devices.")
public class OplinkNetconfDebugCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "selection", description = "descs | pow-get | pp-set | cp-set",
            required = true, multiValued = false)
    String selection = null;
    @Argument(index = 1, name = "did", description = "Device ID",
            required = true, multiValued = false)
    String did = null;
    @Argument(index = 2, name = "port", description = "Port number",
            required = false, multiValued = false)
    Integer port = null;
    @Argument(index = 3, name = "channel", description = "Wavelength number",
            required = false, multiValued = false)
    Integer channel = null;
    @Argument(index = 4, name = "power", description = "Channel power",
            required = false, multiValued = false)
    Float power = null;
    private DeviceId deviceId;
    private PortNumber portNumber;

    @Override
    protected void execute() {
        print(selection);
        print(did);
        if (selection.equals("descs")) {
            getDeviceDescription();
        } else if (selection.equals("pow-get")) {
            getDevicePower();
        } else if (selection.equals("pp-set")) {
            setPortPower();
        } else if (selection.equals("cp-set")) {
            setChPower();
        } else {
            print("Invalid oplink debug command.");
        }
    }

    private void setPortPower() {
        DriverService service = get(DriverService.class);
        deviceId = DeviceId.deviceId(did);
        DriverHandler h = service.createHandler(deviceId);
        portNumber = PortNumber.portNumber(port);
        PowerConfig  powerConfig = h.behaviour(PowerConfig.class);
        long powerValue = (long) (power * 100);
        powerConfig.setTargetPower(portNumber, Direction.ALL, powerValue);
    }

    private void setChPower() {
        DriverService service = get(DriverService.class);
        deviceId = DeviceId.deviceId(did);
        DriverHandler h = service.createHandler(deviceId);
        portNumber = PortNumber.portNumber(port);
        PowerConfig  powerConfig = h.behaviour(PowerConfig.class);
        long powerValue = (long) (power * 100);
        OchSignal ochSig = new OchSignal(DWDM, CHL_12P5GHZ, channel, 4);
        powerConfig.setTargetPower(portNumber, ochSig, powerValue);
    }

    private void getDevicePower() {
        DriverService service = get(DriverService.class);
        deviceId = DeviceId.deviceId(did);
        DriverHandler h = service.createHandler(deviceId);
        portNumber = PortNumber.portNumber(port);
        PowerConfig  powerConfig = h.behaviour(PowerConfig.class);
        Long value = (Long) powerConfig.getTargetPower(portNumber, Direction.ALL).get();
        String strPower = "Power of port" + port + " is " + value.doubleValue() / 100 + "dBm";
        print(strPower);
        if (channel == null) {
            return;
        }
        OchSignal ochSig = new OchSignal(DWDM, CHL_12P5GHZ, channel, 4);
        value = (Long) powerConfig.getTargetPower(portNumber, ochSig).get();
        strPower = "Power of channel" + channel + " is " + value.doubleValue() / 100 + "dBm";
        print(strPower);
    }

    private void getDeviceDescription() {
        DriverService service = get(DriverService.class);
        deviceId = DeviceId.deviceId(did);
        DriverHandler h = service.createHandler(deviceId);
        DeviceDescriptionDiscovery portConfig = h.behaviour(DeviceDescriptionDiscovery.class);
        print(portConfig.discoverPortDetails().toString());
    }
}
