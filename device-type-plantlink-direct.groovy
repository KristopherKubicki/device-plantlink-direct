/**
 *	PlantLink Direct Sensor
 *     This is a non-API implementation of OsoTech's PlantLink moisture sensor.  It converts the voltage
 *    reading on the device to a resistance reading.  The resistance is then used to calculate a moisture 
 *    estimate percentage given a soil type.  The device also has a status for triggering alerts
 *
 *	Author: Kristopher Kubicki, d8adrvn, OsoTech, SmartThings
 *	Date: 2015-06-18
 */
metadata {

	definition (name: "PlantLink Direct Sensor", namespace: "KristopherKubicki", author: "kristopher@acm.org") {
		capability "Relative Humidity Measurement"
		capability "Battery"
		capability "Sensor"
        capability "Water Sensor"
        
        attribute "waterNeeds","string"
        attribute "soilType","string"
        attribute "battery","number"
        attribute "voltage","number"
        attribute "humidity","number"
        attribute "resistivity","number"
        attribute "status","string"

		fingerprint profileId: "0104", inClusters: "0000,0001,0003,0B04"
	}
    
    preferences {
		input "soilType", "enum", title: "Soil Type", options: ["Sand","Loamy Sand","Sandy Loam","Loam","Silty Loam","Silt","Sandy Clay Loam","Clay Loam","Silty Clay Loam","Silty Clay","Sandy Clay"], required: false, displayDuringSetup: true, defaultValue: "Loam"
        input "waterNeeds", "enum", title: "Water Needs", options: ["Very Low","Low","Moderate","High"], required: false, displayDuringSetup: true, defaultValue: "Moderate"
    }
    
    simulator {
        status "moisture": "read attr - raw: C072010B040A0001290000, dni: D00F, endpoint: 01, cluster: 0B04, size: 0A, attrId: 0100, encoding: 29, value: 2100"
        status "battery": "read attr - raw: C0720100010A000021340A, dni: D00F, endpoint: 01, cluster: 0001, size: 0A, attrId: 0000, encoding: 21, value: 0a34"
    }

	tiles {
        valueTile("status", "device.status", inactiveLabel: True, decoration: "flat") {
            state "status", label:'${currentValue}'
        }
        
		valueTile("humidity", "device.humidity", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
			state("humidity", label:'${currentValue}%', unit:"", icon: "st.Outdoor.outdoor23",
				backgroundColors:[
					[value: 2, color: "#FF0000"],
					[value: 10, color: "#CC9900"],
					[value: 20, color: "#996600"],
					[value: 30, color: "#996633"],
					[value: 40, color: "#663300"],
					[value: 50, color: "#000099"]
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
        valueTile("resistivity", "device.resistivity", inactiveLabel: false, decoration: "flat") {
            state "resistivity", label:'${currentValue}µΩ', unit:""
        }
        standardTile("water", "device.water", width: 1, height: 1) {
			state "dry", icon:"st.alarm.water.wet", backgroundColor:"#FF0000"
            state "ok", icon:"st.alarm.water.dry", backgroundColor:"#FFFFFF"
			state "wet", icon:"st.alarm.water.wet", backgroundColor:"#0066FF"
		}

        main "humidity"
        details(["humidity","soilType","battery", "water","status","voltage","resistivity"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "Parse description $description"
    
    def result = null
	if (description?.startsWith("read attr -")) {
		def descMap = parseDescriptionAsMap(description)
		if(descMap.cluster == "0B04" && descMap.attrId == "0100") {
            result = createEvent(name: "humidity", value: calculateHumidity(descMap.value) as Integer)
		} else if (descMap.cluster == "0001" && descMap.attrId == "0000") {
            result = createEvent(name: "battery", value: calculateBattery(descMap.value) as Integer)
		}
	}

	if(device.currentValue("soilType") != settings.soilType) { 
    	sendEvent(name: "soilType", value: settings.soilType)
    }
    if(device.currentValue("waterNeeds") != settings.waterNeeds) { 
   		sendEvent(name: "waterNeeds", value: settings.waterNeeds)
   	}
	
	return result
}

def parseDescriptionAsMap(description) {
	(description - "read attr - ").split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
}


private calculateHumidity(value) {
	
    log.debug "HUMID: $value"
    // exponential curve fit to PlantLink data.  Potentially more accurate than Oso's interpretation
    def resistivity = (1500000 * 2.71828 ** (-0.000888 * Integer.parseInt(value, 16))) as Integer
    if(resistivity > 100000) { 
    	sendEvent(name: "resistivity", value: "∞")
    	sendEvent(name: "status", value: "No Soil!")
        sendEvent(name: "water", value: "error")
        return 0
    }
    else { 
	    sendEvent(name: "resistivity", value: resistivity)
    }
    
    def percent = resistivityToPercent(resistivity)
    
    def moisturePercent = percentToWarn(percent)

	moisturePercent
}

// Originally it seemed the battery voltage was a big component of the 
//  resistivity calculation.  It seems like its probably less than 5% after 
// looking at it more 
private calculateBattery(value) {
	def min = 2300
	def percent = (Integer.parseInt(value, 16) - min) / 10
   
//   	log.debug "VOLTAGE: $percent"
	percent = Math.max(0.0, Math.min(percent, 100.0))
    if(percent < 5) {
    	sendEvent(name: "status", value: "Change Battery!")
    }
    def voltage = sprintf("%.2f",(4.073 + (percent / 100))/1.67)
    sendEvent(name: "voltage", value: voltage)
    
	percent
}

// Calculated by hand.  This was real hard
private resistivityToPercent(resistivity) { 

//log.debug "RESIST: $resistivity"

	def percent = 0.00
    if(resistivity == 0) { 
    	return percent
    }
	else if(soilType == "Loamy Sand" || soilType == "Sand" || soilType == "Sandy Loam") {
		percent = (3.1916 * resistivity ** -0.412)
    }
    else if(soilType == "Clay Loam" || soilType == "Silty Clay Loam" || soilType == "Sandy Clay" || soilType == "Clay") { 
    	percent = (1.6356 * resistivity ** -0.297)
    }
    else {
    	percent = (5.6398 * resistivity ** -0.504)
    }
    
    if(percent < 0) { 
    	percent = 0
    }
 	percent = percent * 100
    
	return percent
}

// 
// I had to pull these values from the PlantLink dashboard
// 
private percentToWarn(percent) { 

//	    log.debug "PERCENT: $percent"
	def moisturePercent = 0.00
    if(soilType == "Silty Clay") { 
            moisturePercent = (percent - 21.4) / (39.4 - 21.4)
    }
    else if(soilType == "Sandy Clay") { 
            moisturePercent = (percent - 32.2) / (40 - 32.2) 
    }
    else if(soilType == "Silty Clay Loam") { 
            moisturePercent = (percent - 32.5) / (44.5 - 32.5)
    }
    else if(soilType == "Clay Loam") {  
            moisturePercent = (percent - 31.1) / (42 - 31.1)
    }
    else if(soilType == "Sandy Clay Loam") { 
            moisturePercent = (percent - 22) / (39.5 - 22)
    }
    else if(soilType == "Silty Loam") { 
            moisturePercent = (percent - 20.1) / (39.5 - 20.1)
    }
    else if(soilType == "Loam" || soilType == "") { 
            moisturePercent = (percent - 23.1) / (37 - 23.1)
    }
    else if(soilType == "Silt") { 
            moisturePercent = (percent - 17.5) / (39 - 17.5)
    }
    else if(soilType == "Sandy Loam") { 
            moisturePercent = (percent - 13) / (31.5 - 13)
    }
    else if(soilType == "Loamy Sand") { 
            moisturePercent = (percent - 9.6) / (29 - 9.6)
    }
    else if(soilType == "Sand") { 
			moisturePercent = (percent - 7.5) / (28 - 7.5)
    }
    moisturePercent = moisturePercent * 100 as Integer
    if(moisturePercent < 1) { 
    	moisturePercent = 0
    }
    if(moisturePercent > 80) { 
    	moisturePercent = 80
    }
    
   if(waterNeeds == "High" && moisturePercent < 20) { 
    	sendEvent(name: "status", value: "Too Dry")
    	sendEvent(name: "water", value: "dry")
    }
    else if(waterNeeds == "Moderate" && moisturePercent < 15) {
    	sendEvent(name: "status", value: "Too Dry")
    	sendEvent(name: "water", value: "dry")
    }
    else if(waterNeeds == "Low" && moisturePercent < 10) { 
    	sendEvent(name: "status", value: "Too Dry")
    	sendEvent(name: "water", value: "dry")
    }
    else if(moisturePercent < 5) { 
    	sendEvent(name: "status", value: "Too Dry")
    	sendEvent(name: "water", value: "dry")
    }
    else if(moisturePercent > 50) { 
    	sendEvent(name: "status", value: "Too Wet")
    	sendEvent(name: "water", value: "wet")
    }
    else { 
    	sendEvent(name: "status", value: "OK")
    	sendEvent(name: "water", value: "ok")
    }
//log.debug "MPERCENT: $moisturePercent"

	moisturePercent
}

