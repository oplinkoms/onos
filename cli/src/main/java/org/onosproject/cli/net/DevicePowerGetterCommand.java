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

@Command(scope = "onos", name = "device-power",
        description = "Gets the device power of the specified device.")
public class DevicePowerGetterCommand  extends AbstractShellCommand {

    @Argument(index = 0, name = "did", description = "Device ID",
            required = true, multiValued = false)
    String did = null;
    @Argument(index = 1, name = "port", description = "Port number",
            required = true, multiValued = false)
    Integer port = null;
    @Argument(index = 2, name = "channel", description = "Wavelength number",
            required = false, multiValued = false)
    Integer channel = null;
    private DeviceId deviceId;
    private PortNumber portNumber;

    @Override
    protected void execute() {
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

}
