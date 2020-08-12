# ONOS README for OPLINK
`onos>` indicates the following command should be run in ONOS shell. 

## ONOS basics
Please follow the ONOS readme to install compilation requirements. 
Consider add the ONOS developer environment to your bash profile. 

First enter the ONOS shell and activate oplink driver
```
$ onos localhost
onos> app activate oplink
```

To add an oplink device, if ONOS is running on localhost, run 
```bash
$ onos-netcfg localhost <path_to_json_config_file>
```

The json config should be in the following format:
```json
{
  "devices": {
    "netconf:<ip>:<port>": {
      "netconf": {
        "ip": "<ip>",
        "port": <port>,
        "username": "<username>",
        "password": "<password>"
      },
      "basic": {
        "driver": "oplink-netconf"
      }
    }
  }
}
```

If ONOS is running on a remote machine, replace `localhost` with its ip addr.

## What it does
The oplink driver implements a number of interfaces specified by ONOS. 
These interfaces communicate with the devices via xml-formatted netconf messages, according to a specific yang model. 

For instance, when an oplink device is added, ONOS invokes discoverDeviceDetails() and discoverPortDetails() of OplinkOpticalDeviceDescription to record info in newly created objects. 

## Some useful app/command
### roadm app
The roadm app can give a complete view of device and port info. 
To invoke it, run 
```
onos> app activate roadm
```
and in the menu (top left) of ONOS GUI, click `Optical UI`.

### `power-config`
`power-config` command can get and set the power of ports' power. 

### `path-config`
`path-config` command can get and set the active path of OPS devices.

To see command's help, run
```
onos> <command> --help
```


## Get started on developing
https://wiki.onosproject.org/display/ONOS/Basic+ONOS+Tutorial for basic UI interaction

https://wiki.onosproject.org/display/ONOS/NETCONF for more details on adding netconf devices

https://wiki.onosproject.org/pages/viewpage.action?pageId=28836246 for setting up Intellij

Can use org.onosproject.cli.net.PowerConfigCommand as a template to add your own command, if you would like to invoke specific methods. 
