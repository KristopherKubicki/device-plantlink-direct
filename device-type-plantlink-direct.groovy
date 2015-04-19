/**
 *	PlantLink Direct
 *     This is a non-API implementation of OsoTech's PlantLink moisture sensor.  It converts the voltage
 *    reading on the device to a resistance reading.  The resistance is then used to calculate a moisture 
 *    estimate given a soil type.  Still very work in progress!
 *
 *	Author: Kristopher Kubicki, d8adrvn, OsoTech, SmartThings
 *	Date: 2015-04-15
 */
metadata {

	definition (name: "PlantLink Sensor", namespace: "KristopherKubicki", author: "kristopher@acm.org") {
		capability "Relative Humidity Measurement"
		capability "Battery"
		capability "Sensor"
        
        attribute "soilType","string"
        attribute "battery","number"
        attribute "voltage","number"
        attribute "humidity","number"
        attribute "status","string"

		fingerprint profileId: "0104", inClusters: "0000,0001,0003,0B04"
	}
    
    preferences {
		input "soilType", "enum", title: "Soil Type", options: ["Sand","Loamy Sand","Sandy Loam","Loam","Silty Loam","Silt","Sandy Clay Loam","Clay Loam","Silty Clay Loam","Silty Clay","Sandy Clay"], required: true, displayDuringSetup: true, defaultValue: "Loam"
    }
    
    simulator {
        status "moisture": "read attr - raw: C072010B040A0001290000, dni: D00F, endpoint: 01, cluster: 0B04, size: 0A, attrId: 0100, encoding: 29, value: 2100"
        status "battery": "read attr - raw: C0720100010A000021340A, dni: D00F, endpoint: 01, cluster: 0001, size: 0A, attrId: 0000, encoding: 21, value: 0a34"
    }

	tiles {
        valueTile("status", "device.status", inactiveLabel: True, decoration: "flat") {
            state "status", label:'${currentValue}'
        }
        
        // values change per soil type 
        
		valueTile("humidity", "device.humidity", width: 2, height: 2) {
			state("humidity", label:'${currentValue}%', unit:"",
				backgroundColors:[
					[value: 31, color: "#FF0000"],
					[value: 44, color: "#CC9900"],
					[value: 59, color: "#FF9900"],
					[value: 74, color: "#996600"],
					[value: 84, color: "#996633"],
					[value: 95, color: "#663300"],
					[value: 96, color: "#000099"]
				]
			)
		}
        valueTile("soilType", "device.soilType", inactiveLabel: false, decoration: "flat") {
            state "soilType", label:'${currentValue}', unit:""
        }
   		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
            state "battery", label:'${currentValue}% battery', unit:""
        }
        valueTile("voltage", "device.voltage", inactiveLabel: false, decoration: "flat") {
            state "voltage", label:'${currentValue}V', unit:""
        }

        main "humidity"
        details(["humidity","soilType","battery", "status","voltage"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "Parse description $description"
	def map = [:]
	if (description?.startsWith("read attr -")) {
		def descMap = parseDescriptionAsMap(description)
		if(descMap.cluster == "0B04" && descMap.attrId == "0100") {
			map.name = "humidity"
			map.value = calculateHumidity(descMap.value) as Integer
		} else if (descMap.cluster == "0001" && descMap.attrId == "0000") {
			map.name = "battery"
			map.value = calculateBattery(descMap.value) as Integer
		}
	}
    log.debug "Processed description $description"
    
    sendEvent(name: "soilType", value: settings.soilType)

	def result = null
	if (map) {
		result = createEvent(map)
	}
	return result
}

def parseDescriptionAsMap(description) {
	(description - "read attr - ").split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
}


// https://www.midwestlabs.com/wp-content/uploads/2012/09/137_estimating_soil_textures_by_cation_exch_cap_det.pdf
//
// http://www.decagon.com/support/datatrac-3-online-help-files/how-do-i-graph-plant-available-water/plant-available-water-how-do-i-determine-field-capacity-and-permanent-wilting-point/

private calculateHumidity(value) {
	//adc reading of 0x1ec0 produces a plant fuel level near 0
	//adc reading of 0x2100 produces a plant fuel level near 100%
    
    def percent = 0
    if(soilType == "Loamy Sand") { 
    	percent = (Integer.parseInt(value, 16) - 3000) / 12500
    }
    
    percent = (percent * 100) as Integer
    
    if(percent < 5) { 
    	sendEvent(name: "status", value: "No Soil!")
    } 
    else if(percent > 96) { 
    	sendEvent(name: "status", value: "Waterlogged!")
    } 
	percent
}

private calculateBattery(value) {
	def min = 2300
	def percent = (Integer.parseInt(value, 16) - min) / 10
   
	percent = Math.max(0.0, Math.min(percent, 100.0))
    if(percent < 5) {
    	sendEvent(name: "status", value: "Change Battery!")
    }
    def voltage = sprintf("%.2f",(4.167 + (percent / 100))/1.67)
    sendEvent(name: "voltage", value: voltage)
    
	percent
}



private hex(value) {
	new BigInteger(Math.round(value).toString()).toString(16)
}
