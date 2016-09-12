/**
 *  eagle owl new
 *
 *  Copyright 2016 Chris Evans
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "eagle-owl", namespace: "chrisduffer", author: "Chris Evans") {
		capability "Polling"
		capability "Power Meter"
		capability "Refresh"
	}  

	simulator {
	}
    
    preferences {
    	input("ip", "string", title:"IP Address", description: "192.168.1.126", required: true, displayDuringSetup: true)
        input("liveDataUrl", "string", title:"eagle-owl Live Data Url", description: "/eagle-owl/live_data.php", required: true, displayDuringSetup: true)
	}
    
	tiles {
		valueTile("power", "device.power", width: 2, height: 2) {
			state "default", label:'${currentValue} W', unit:"W",
            backgroundColors:[
            // These probably need to work
            [value: 0, color: "#44b621"],
            [value: 100, color: "#44b621"],
            [value: 800, color: "#f1d801"],
            [value: 1000, color: "#d04e00"],
            [value: 1100, color: "#bc2323"]
        	]
		}
        
        valueTile("lastdata", "device.lastdata",) {
			state "default", label:'Last Update: ${currentValue}'
		}
        
        standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
		main (["power",])
		details(["power", "lastdata", "refresh"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
    def descMap = parseDescriptionAsMap(description)
    def body
    //log.debug "descMap: ${descMap}"
    
    if (descMap["body"]) body = new String(descMap["body"].decodeBase64())
    log.debug "body: ${body}"
    def parts = body.split(' ')
    sendEvent(name: "power", value: parts[3])
    sendEvent(name: "lastdata", value: "${parts[0]} ${parts[1]}")
}

// handle commands
def poll() {
	log.debug "Executing 'poll'"
	refresh()
}

def refresh() {
	log.debug "Executing 'refresh'"
    
    def cmds = []
    cmds << getAction(liveDataUrl)
    log.debug "refresh cmds: ${cmds}"
    return cmds
}

def installed() {
	log.debug "installed()"
	configure()
}

def updated() {
	log.debug "updated()"
    configure()
}

def configure() {
	log.debug "configure() ip: ${ip} liveDataUrl: ${liveDataUrl}"
    if (ip != null && ip != "") state.dni = setDeviceNetworkId(ip, "80")
    if (ip == null || ip == "") log.error "ip not configured"
    if (liveDataUrl == null || liveDataUrl == "") log.error "liveDataUrl not configured"
}

private setDeviceNetworkId(ip, port = null){
    def myDNI
    if (port == null) {
        myDNI = ip
    } else {
  	    def iphex = convertIPtoHex(ip)
  	    def porthex = convertPortToHex(port)
        myDNI = "$iphex:$porthex"
    }
    log.debug "Device Network Id set to ${myDNI}"
    return myDNI
}

private updateDNI() { 
    if (device.deviceNetworkId != state.dni) {
        device.deviceNetworkId = state.dni
    }
}

private getAction(uri){ 
  updateDNI()
 
  def headers = getHeader()

  def hubAction = new physicalgraph.device.HubAction(
    method: "GET",
    path: uri,
    headers: headers
  )
  return hubAction    
}

private getHeader(){
    def headers = [:]
    headers.put("Host", getHostAddress())
    headers.put("Content-Type", "application/x-www-form-urlencoded")
    return headers
}

private getHostAddress() {
    if (ip != null && ip != ""){
        return "${ip}:80"
    }
    else if(getDeviceDataByName("ip") && getDeviceDataByName("port")){
        return "${getDeviceDataByName("ip")}:${getDeviceDataByName("port")}"
    }else{
	    return "${ip}:80"
    }
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}

def parseDescriptionAsMap(description) {
	description.split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
        
        if (nameAndValue.length == 2) map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
        else map += [(nameAndValue[0].trim()):""]
	}
}