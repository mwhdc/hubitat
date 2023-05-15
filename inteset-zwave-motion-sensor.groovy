/*
*   Inteset Z-Wave Plus Motion Sensor "2-in-1" INT-SMMD-N1 aka INT-ZWAV-MTD
*   v1.2 2023-05-15 by mwhdc
*   https://github.com/mwhdc/hubitat/blob/main/inteset-zwave-motion-sensor.groovy
*   Based on HomeSeer HSM200 Multi-Sensor v1.0 by djdizzyd
*   https://github.com/djdizzyd/hubitat/blob/master/Drivers/HomeSeer/HSM200-Multi-Sensor.groovy
*/

import groovy.transform.Field

metadata {
    definition (name: "Inteset Z-Wave Plus Motion Sensor", namespace: "mwhdc", author: "mwhdc") {
        capability "Sensor"
        capability "MotionSensor"
        capability "IlluminanceMeasurement"
        capability "TemperatureMeasurement"
        capability "Battery"
        capability "Configuration"
        capability "Refresh"
        fingerprint mfr:"039A", prod:"0003", deviceId:"0106", inClusters:"0x5E,0x9F,0x55,0x86,0x73,0x85,0x8E,0x59,0x72,0x5A,0x80,0x84,0x30,0x71,0x31,0x70,0x6C", deviceJoinName: "Inteset Z-Wave Plus Motion Sensor"
    }
    preferences {
        configParams.each { input it.value.input }
        input name: "txtEnable", type: "bool", title: "Enable descriptive logging", defaultValue: true
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    }
}

@Field static Map configParams = [
        1: [input: [name: "configParam1", type: "number", title: "1: Motion Sensitivity", description: "0-99 (3)", range: "0..99", defaultValue: 3], parameterSize: 1],
        2: [input: [name: "configParam2", type: "enum", title: "2: Motion Window Time", description: "4-16 sec (12)", options: [0: "4 sec", 1: "8 sec", 2: "12 sec", 3: "16 sec"], defaultValue: 2], parameterSize: 1],
        3: [input: [name: "configParam3", type: "number", title: "3: Pulse Count", description: "0-3 (1)", range: "0..3", defaultValue: 1], parameterSize: 1],
        4: [input: [name: "configParam4", type: "number", title: "4: Motion Blind Time", description: "0-15 (15)", range: "0..15", defaultValue: 15], parameterSize: 1],
        5: [input: [name: "configParam5", type: "enum", title: "5: Motion Detection", description: "", options: [0: "Disable", 1: "Enable"], defaultValue: 1], parameterSize: 1],
        6: [input: [name: "configParam6", type: "number", title: "6: Motion Clear Time", description: "10-3600 sec (30)", range: "10..3600", defaultValue: 30], parameterSize: 2],
        7: [input: [name: "configParam7", type: "enum", title: "7: LED Indicator", description: "", options: [0: "Disable", 1: "Enable"], defaultValue: 1], parameterSize: 1],
        8: [input: [name: "configParam8", type: "enum", title: "8: Binary Sensor Report", description: "", options: [0: "Disable", 1: "Enable"], defaultValue: 0], parameterSize: 1],
        9: [input: [name: "configParam9", type: "enum", title: "9: Basic Set Level", description: "", options: [0: "Disable", 100: "Enable"], defaultValue: 100], parameterSize: 1],
        10: [input: [name: "configParam10", type: "number", title: "10: Light Sensor Measuring Interval", description: "30-3600 sec (180)", range: "30..3600", defaultValue: 180], parameterSize: 2],
        11: [input: [name: "configParam11", type: "number", title: "11: Light Intensity Differential Report", description: "1-127 lx (50)", range: "1..127", defaultValue: 50], parameterSize: 1],
        12: [input: [name: "configParam12", type: "number", title: "12: Light Intensity Threshold", description: "1-127 lx (50)", range: "1..127", defaultValue: 50], parameterSize: 1],
        13: [input: [name: "configParam13", type: "enum", title: "13: Light Intensity Associated", description: "", options: [0: "Disable", 1: "Enable"], defaultValue: 0], parameterSize: 1],
        14: [input: [name: "configParam14", type: "enum", title: "14: Motion Event Report Once", description: "", options: [0: "Disable", 1: "Enable"], defaultValue: 0], parameterSize: 1],
        99: [input: [name: "configParam99", type: "number", title: "99: Light Intensity Offset Calibration", description: "1-65536 (1000 or 5320)", range: "1..65536", defaultValue: 1000], parameterSize: 2]
]

@Field static Map ZWAVE_NOTIFICATION_TYPES=[0:"Reserved", 1:"Smoke", 2:"CO", 3:"CO2", 4:"Heat", 5:"Water", 6:"Access Control", 7:"Home Security", 8:"Power Management", 9:"System", 10:"Emergency", 11:"Clock", 12:"First"]

@Field static Map CMD_CLASS_VERS = [
     0x30: 2 // SENSOR_BINARY
    ,0x31: 7 // SENSOR_MULTILEVEL
    ,0x55: 2 // TRANSPORT_SERVICE
    ,0x59: 1 // ASSOCIATION_GRP_INFO
    ,0x5A: 1 // DEVICE_RESET_LOCALLY
    ,0x5E: 2 // ZWAVEPLUS_INFO
    ,0x6C: 1 // SUPERVISION
    ,0x70: 1 // CONFIGURATION
    ,0x71: 8 // ALARM
    ,0x72: 2 // MANUFACTURER_SPECIFIC
    ,0x73: 1 // POWERLEVEL
    ,0x80: 1 // BATTERY
    ,0x84: 2 // WAKE_UP
    ,0x85: 2 // ASSOCIATION
    ,0x86: 3 // VERSION
    ,0x8E: 3 // MULTI_CHANNEL_ASSOCIATION
    ,0x9F: 1 // SECURITY_2
]

void logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

void configure() {
    runIn(5,pollDeviceData)
}

void updated() {
    log.info "updated..."
    log.warn "description logging is: ${txtEnable == true}"
    log.warn "debug logging is: ${logEnable == true}"
    unschedule()
    if (logEnable) runIn(1800,logsOff)
    // List<hubitat.zwave.Command> cmds=[]
    // cmds.addAll(runConfigs())
    // sendToDevice(cmds)
    state.configUpdatePending=true
}

List<hubitat.zwave.Command> runConfigs() {
    List<hubitat.zwave.Command> cmds=[]
    configParams.each { param, data ->
        if (settings[data.input.name]) {
            cmds.addAll(configCmd(param, data.parameterSize, settings[data.input.name]))
        }
    }
    return cmds
}

List<hubitat.zwave.Command> pollConfigs() {
    List<hubitat.zwave.Command> cmds=[]
    configParams.each { param, data ->
        if (settings[data.input.name]) {
            cmds.add(zwave.configurationV1.configurationGet(parameterNumber: param.toInteger()))
        }
    }
    return cmds
}

List<hubitat.zwave.Command> configCmd(parameterNumber, size, scaledConfigurationValue) {
    List<hubitat.zwave.Command> cmds = []
    cmds.add(zwave.configurationV1.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), scaledConfigurationValue: scaledConfigurationValue.toInteger()))
    cmds.add(zwave.configurationV1.configurationGet(parameterNumber: parameterNumber.toInteger()))
    return cmds
}

void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
    if(configParams[cmd.parameterNumber.toInteger()]) {
        Map configParam=configParams[cmd.parameterNumber.toInteger()]
        int scaledValue
        cmd.configurationValue.reverse().eachWithIndex { v, index -> scaledValue=scaledValue | v << (8*index) }
        device.updateSetting(configParam.input.name, [value: "${scaledValue}", type: configParam.input.type])
    }
}

void zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpIntervalReport cmd) {
    state.wakeInterval=cmd.seconds
}

void zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpNotification cmd) {
    log.info "${device.displayName} woke up"
    List<hubitat.zwave.Command> cmds=[]
    cmds.add(zwave.batteryV1.batteryGet())
    if (state.configUpdatePending) {
        cmds.addAll(runConfigs())
        state.configUpdatePending=false
    }
    cmds.add(zwave.wakeUpV1.wakeUpNoMoreInformation())
    sendToDevice(cmds)
}

void pollDeviceData() {
    List<hubitat.zwave.Command> cmds = []
    cmds.add(zwave.versionV2.versionGet())
    cmds.add(zwave.manufacturerSpecificV2.deviceSpecificGet(deviceIdType: 1))
    cmds.addAll(processAssociations())
    cmds.addAll(pollConfigs())
    // cmds.add(zwave.configurationV1.configurationSet(parameterNumber: 1, size: 1, scaledConfigurationValue: 1))
    cmds.add(zwave.batteryV1.batteryGet())
    cmds.add(zwave.wakeUpV1.wakeUpIntervalGet())
    cmds.add(zwave.sensorMultilevelV6.sensorMultilevelGet(scale: 1, sensorType: 3))
    cmds.add(zwave.notificationV8.notificationGet(notificationType: 7, event:0))
    // cmds.add(zwave.sensorMultilevelV6.sensorMultilevelGet(scale: (location.temperatureScale=="F"?1:0), sensorType: 1))
    sendToDevice(cmds)
}

void refresh() {
    List<hubitat.zwave.Command> cmds=[]
    cmds.add(zwave.batteryV1.batteryGet())
    cmds.add(zwave.sensorMultilevelV6.sensorMultilevelGet(scale: 1, sensorType: 3))
    cmds.add(zwave.notificationV8.notificationGet(notificationType: 7, event:0))
    // cmds.add(zwave.sensorMultilevelV6.sensorMultilevelGet(scale: (location.temperatureScale=="F"?1:0), sensorType: 1))
    sendToDevice(cmds)
}

void installed() {
    if (logEnable) log.debug "installed..."
}

void eventProcess(Map evt) {
    if (device.currentValue(evt.name).toString() != evt.value.toString() || !eventFilter) {
        evt.isStateChange=true
        sendEvent(evt)
    }
}

void zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) {
    if (logEnable) log.debug "${cmd}"
    Map evt = [isStateChange:false]
    switch (cmd.sensorType) {
        case 1:
            evt.name = "temperature"
            evt.value = cmd.scaledSensorValue.toInteger()
            evt.unit = cmd.scale==0?"C":"F"
            evt.isStateChange=true
            evt.descriptionText="${device.displayName} temperature is ${evt.value}"
            break
        case 3:
            evt.name = "illuminance"
            evt.value = cmd.scaledSensorValue.toInteger()
            evt.unit = "lux"
            evt.isStateChange=true
            evt.descriptionText="${device.displayName} illuminance is ${evt.value} lx"
            break
    }
    if (evt.isStateChange) {
        if (txtEnable) log.info evt.descriptionText
        eventProcess(evt)
    }
}

void zwaveEvent(hubitat.zwave.commands.notificationv8.NotificationReport cmd) {
    Map evt = [isStateChange:false]
    // log.info "Notification: " + ZWAVE_NOTIFICATION_TYPES[cmd.notificationType.toInteger()]
    if (cmd.notificationType==7) {
        // home security
        switch (cmd.event) {
            case 0:
                // state idle
                if (cmd.eventParametersLength > 0) {
                    switch (cmd.eventParameter[0]) {
                        case 7:
                            evt.name = "motion"
                            evt.value = "inactive"
                            evt.isStateChange = true
                            evt.descriptionText = "${device.displayName} motion is ${evt.value}"
                            break
                        case 8:
                            evt.name = "motion"
                            evt.value = "inactive"
                            evt.isStateChange = true
                            evt.descriptionText = "${device.displayName} motion is ${evt.value}"
                            break
                    }
                } else {
                    evt.name = "motion"
                    evt.value = "inactive"
                    evt.descriptionText = "${device.displayName} motion is ${evt.value}"
                    evt.isStateChange = true
                }
                break
            case 7:
                // motion detected (location provided)
                evt.name = "motion"
                evt.value = "active"
                evt.isStateChange = true
                evt.descriptionText = "${device.displayName} motion is ${evt.value}"
                break
            case 8:
                // motion detected
                evt.name = "motion"
                evt.value = "active"
                evt.isStateChange = true
                evt.descriptionText = "${device.displayName} motion is ${evt.value}"
                break
            case 254:
                // unknown event/state
                log.warn "Device sent unknown event / state notification"
                break
        }
    }
    if (evt.isStateChange) {
        if (txtEnable) log.info evt.descriptionText
        eventProcess(evt)
    }
}

void zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    hubitat.zwave.Command encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand)
    }
}

void parse(String description) {
    if (logEnable) log.debug "parse:${description}"
    hubitat.zwave.Command cmd = zwave.parse(description, CMD_CLASS_VERS)
    if (cmd) {
        zwaveEvent(cmd)
    }
}

void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd) {
    if (logEnable) log.debug "Supervision get: ${cmd}"
    hubitat.zwave.Command encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand)
    }
    sendToDevice(new hubitat.zwave.commands.supervisionv1.SupervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0))
}

void zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
    Map evt = [name: "battery", unit: "%"]
    if (cmd.batteryLevel == 0xFF) {
        evt.descriptionText = "${device.displayName} battery is low"
        evt.value = "1"
    } else {
        evt.descriptionText = "${device.displayName} battery is ${cmd.batteryLevel}%"
        evt.value = "${cmd.batteryLevel}"
    }
    eventProcess(evt)
}

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) {
    if (logEnable) log.debug "Device Specific Report: ${cmd}"
    switch (cmd.deviceIdType) {
        case 1:
            // serial number
            def serialNumber=""
            if (cmd.deviceIdDataFormat==1) {
                cmd.deviceIdData.each { serialNumber += hubitat.helper.HexUtils.integerToHexString(it & 0xff,1).padLeft(2, '0')}
            } else {
                cmd.deviceIdData.each { serialNumber += (char) it }
            }
            device.updateDataValue("serialNumber", serialNumber)
            break
    }
}

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
    if (logEnable) log.debug "version2 report: ${cmd}"
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

void sendToDevice(List<hubitat.zwave.Command> cmds) {
    sendHubCommand(new hubitat.device.HubMultiAction(commands(cmds), hubitat.device.Protocol.ZWAVE))
}

void sendToDevice(hubitat.zwave.Command cmd) {
    sendHubCommand(new hubitat.device.HubAction(secureCommand(cmd), hubitat.device.Protocol.ZWAVE))
}

void sendToDevice(String cmd) {
    sendHubCommand(new hubitat.device.HubAction(secureCommand(cmd), hubitat.device.Protocol.ZWAVE))
}

List<String> commands(List<hubitat.zwave.Command> cmds, Long delay=300) {
    return delayBetween(cmds.collect{ secureCommand(it) }, delay)
}

String secureCommand(hubitat.zwave.Command cmd) {
    secureCommand(cmd.format())
}

String secureCommand(String cmd) {
    String encap=""
    if (getDataValue("zwaveSecurePairingComplete") != "true") {
        return cmd
    } else {
        encap = "988100"
    }
    return "${encap}${cmd}"
}

void zwaveEvent(hubitat.zwave.Command cmd) {
    if (logEnable) log.debug "skip:${cmd}"
}

List<hubitat.zwave.Command> setDefaultAssociation() {
    List<hubitat.zwave.Command> cmds=[]
    cmds.add(zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: zwaveHubNodeId))
    cmds.add(zwave.associationV2.associationGet(groupingIdentifier: 1))
    return cmds
}

List<hubitat.zwave.Command> processAssociations(){
    List<hubitat.zwave.Command> cmds = []
    cmds.addAll(setDefaultAssociation())
    if (logEnable) log.debug "processAssociations cmds: ${cmds}"
    return cmds
}

void zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
    if (logEnable) log.debug "${device.label?device.label:device.name}: ${cmd}"
    List<String> temp = []
    if (cmd.nodeId != []) {
        cmd.nodeId.each {
            temp.add(it.toString().format( '%02x', it.toInteger() ).toUpperCase())
        }
    }
    updateDataValue("zwaveAssociationG${cmd.groupingIdentifier}", "$temp")
}

void zwaveEvent(hubitat.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
    if (logEnable) log.debug "${device.label?device.label:device.name}: ${cmd}"
    log.info "${device.label?device.label:device.name}: Supported association groups: ${cmd.supportedGroupings}"
    state.associationGroups = cmd.supportedGroupings
}
