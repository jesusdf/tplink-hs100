definition(
  name: "TPLink HS100 Service Manager",
  namespace: "arhea",
  author: "Alex Rhea",
  description: "This discovers TPLink HS100 Smart Plugs via the TPLink HS100 Hub.",
  category: "Lights & Switches",
  iconUrl: "https://lh3.googleusercontent.com/e3xV1TP8CpuDqcCvKsVTWaW1SJG-8uOCOs5x-fmdhz53lj3iQfFO_PjC8rQpOJnX0Lc=w300",
  iconX2Url: "https://lh3.googleusercontent.com/e3xV1TP8CpuDqcCvKsVTWaW1SJG-8uOCOs5x-fmdhz53lj3iQfFO_PjC8rQpOJnX0Lc=w300",
  iconX3Url: "https://lh3.googleusercontent.com/e3xV1TP8CpuDqcCvKsVTWaW1SJG-8uOCOs5x-fmdhz53lj3iQfFO_PjC8rQpOJnX0Lc=w300",
  singleInstance: true
)


preferences {
  input "TPLinkHubHost", "text", title: "Home Hub Host"
  input "TPLinkHubPort", "text", title: "Home Hub Port"
}

def installed() {
  log.debug "[TPLink][App][Installed] - ${settings}"
  initialize()
}

def updated() {
  log.debug "[TPLink][App][Updated] - ${settings}"
  initialize()
}

def uninstalled() {
  log.debug "[TPLink][App][Uninstalled] - ${settings}"
  tplinkRemovePlugs(getChildDevices())
}

def initialize() {
  log.debug "[TPLink][App][Initialize] - ${settings}"
  tplinkListPlugs()
  runEvery5Minutes("tplinkListPlugs")
}

def tplinkListPlugs() {
  log.debug "[TPLink][App] - List Plugs"

  sendHubCommand(new physicalgraph.device.HubAction(
    method: "GET",
    path: "/plugs",
    headers: [
      HOST: "${TPLinkHubHost}:${TPLinkHubPort}"
    ],
    null,
    [callback: tplinkRecievePlugs]
  ))
}

def tplinkGetPlug(device) {
  log.debug "[TPLink][App] - Get Plug ${device.name}"

  sendHubCommand(new physicalgraph.device.HubAction(
    method: "GET",
    path: "/plugs/${device.deviceNetworkId}",
    headers: [
      HOST: "${TPLinkHubHost}:${TPLinkHubPort}"
    ],
    null,
    [callback: tplinkReceivePlug]
  ))
}

def tplinkTurnOnPlug(device) {
  log.debug "[TPLink][App] - Turn On ${device.name}"

  sendHubCommand(new physicalgraph.device.HubAction(
    method: "GET",
    path: "/plugs/${device.deviceNetworkId}/on",
    headers: [
      HOST: "${TPLinkHubHost}:${TPLinkHubPort}"
    ],
    null,
    [callback: tplinkReceivePlug]
  ))
}

def tplinkTurnOffPlug(device) {
  log.debug "[TPLink][App] - Turn Off ${device.name}"

  sendHubCommand(new physicalgraph.device.HubAction(
    method: "GET",
    path: "/plugs/${device.deviceNetworkId}/off",
    headers: [
      HOST: "${TPLinkHubHost}:${TPLinkHubPort}"
    ],
    null,
    [callback: tplinkReceivePlug]
  ))
}

def tplinkReceivePlug(physicalgraph.device.HubResponse hubResponse) {
  log.debug "[TPLink][App] - Receive Plug ${hubResponse.json}"
  tplinkProcessPlug(hubResponse.json)
}

def tplinkRecievePlugs(physicalgraph.device.HubResponse hubResponse) {
  log.debug "[TPLink][App] - Receive Plugs ${hubResponse.json}"
  hubResponse.json.each { plug ->
    tplinkProcessPlug(plug)
  }
}

def tplinkProcessPlug(json) {
  log.debug "[TPLink][App] - Process Plug ${json}"

  try {

    def found = getChildDevice(json.sysInfo.deviceId)

    if(found) {
      log.debug "[TPLink][App] - Updating Existing Plug ${json.sysInfo.alias}"

      found.name = "${json.sysInfo.alias}"
      found.label = "${json.sysInfo.alias}"
      found.data.host = "${json.connectionInfo.host}"
      found.data.port = "${json.connectionInfo.port}"

    } else {
      log.debug "[TPLink][App] - Adding New Plug ${json.sysInfo.alias}"

      found = addChildDevice("arhea", "TPLink HS-100", json.sysInfo.deviceId, null, [
        "name": "TPLink.${json.sysInfo.deviceId}",
        "label": "${json.sysInfo.alias}",
        "data": [
          "host": "${json.connectionInfo.host}",
          "port": "${json.connectionInfo.port}"
        ],
        "completedSetup": true
      ])

    }

    if(json.sysInfo.relay_state == 1) {
      found.handleOn()
    } else {
      found.handleOff()
    }

  } catch(e) {
    log.error "[TPLink][App] - Error processing device: ${e}"
  }
}

def tplinkRemovePlugs(plugs) {
  plugs.each {
    deleteChildDevice(it.deviceNetworkId)
  }
}
