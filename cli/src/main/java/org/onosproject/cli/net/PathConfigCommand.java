package org.onosproject.cli.net;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.behaviour.protection.ProtectedTransportEndpointState;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.behaviour.protection.ProtectionConfigBehaviour;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@Command(scope = "onos", name = "path-config",
        description = "get/set path of OPS")
public class PathConfigCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "operation", description = "get/edit",
            required = true, multiValued = false)
    private String operation = null;

    @Argument(index = 1, name = "connection-point", description = "\"{DeviceID}/0\"",
            required = true)
    private String connectPoint = null;

    @Argument(index = 2, name = "mode", description = "force/manual/auto",
            required = false)
    private String mode = null;

    @Argument(index = 3, name = "path", description = "0 for PRIMARY, 1 for SECONDARY",
            required = false)
    private Integer path = null;

    @Override
    protected void doExecute() {
        DeviceService deviceService = get(DeviceService.class);
        ConnectPoint cp = ConnectPoint.deviceConnectPoint(connectPoint);
        Device device = deviceService.getDevice(cp.deviceId());
        ProtectionConfigBehaviour protectionConfig = device.as(ProtectionConfigBehaviour.class);
        if (operation.equals("edit")) {
            if (mode == null) {
                print("For edit op, mode needs to be specified");
                return;
            }
            if (!mode.equals("auto") && path == null) {
                print("For non-auto mode, path needs to be specified");
                return;
            }
            if (mode.equals("force")) {
                protectionConfig.switchToForce(cp, path);
            } else if (mode.equals("manual")) {
                protectionConfig.switchToManual(cp, path);
            } else if (mode.equals("auto")) {
                protectionConfig.switchToAutomatic(cp);
            } else {
                print("Unsupported mode specified");
            }
        } else if (operation.equals("get")) {
            Map<ConnectPoint, ProtectedTransportEndpointState> map;
            try {
                map = protectionConfig.getProtectionEndpointStates().get();
            } catch (InterruptedException e1) {
                log.error("Interrupted.", e1);
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException e1) {
                log.error("Exception caught.", e1);
                return;
            }
            ProtectedTransportEndpointState state = map.get(cp);
            print(state.pathStates().get(state.workingPathIndex()).id().toString());
        } else {
            print("operation needs to be get or edit");
        }
    }
}
