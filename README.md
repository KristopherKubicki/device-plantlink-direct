# PlantLink Direct Device
SmartThings Direct Implementation of the PlantLink Moisture Sensor.   This is an "offline" implementation of Oso Tech's <a href='http://myplantlink.com/check-out-page/'>PlantLink device</a>.  This is a very affordable dynamic range Zigbee moisture sensor that only costs $30.  The battery life is excellent, the durability is fairly good -- just don't waterlog them for too long or leave them outside in the winter.  

<img src='https://cloud.githubusercontent.com/assets/478212/7313447/f4745eee-ea1e-11e4-8027-0a08c809019b.jpg'>

This SmartThings device type removes the need for the PlantLink Hub.  It saves you money, but you also lose some of the Oso Tech functionality like being able to specify alerts based on your plant type.  This implementation still allows you to get alerts based on your soil type, which is also very important. 

You can set alerts based on Moisture (Dry/Wet) or by a Relative Humidity percentage.  Both are supported by the device capabilities and should be compatible with most SmartApps. 

# Installation

* Open the <a href='https://graph.api.smartthings.com/ide/device/create'>SmartThings Device Types</a> console.  Click "From Code" and paste the <a href='https://github.com/KristopherKubicki/device-plantlink-direct/blob/master/device-type-plantlink-direct.groovy'>device type</a>. 
* Open your SmartThings UI and click "Add new device"
* Pull the batteries from your Link and wait 30 seconds.  Then insert the batteries and hit the only button on the device. 
* SmartThings should instantly recognize your Plant Link and will report your first measurement within 20 minutes.  
* When you open the new device for the first time, hit "Preferences"

<img width='320' src='https://cloud.githubusercontent.com/assets/478212/7313692/4f379cf8-ea22-11e4-82fc-bf8569ce6cac.png'>

* From here you can pick a soil type.  Picking the wrong soil type will give you a completely different reading so try to get close!

<img width='320' src='https://cloud.githubusercontent.com/assets/478212/7313689/4bdae0b0-ea22-11e4-8c37-f3c41c537440.png'>
<img width='320' src='https://cloud.githubusercontent.com/assets/478212/7313690/4bdc1c8c-ea22-11e4-8021-1c4c27fcfafd.png'>
* That's it, you're done!  Wait 15-20 minutes and you will get your first reading.  Each additional reading will come in every 15 minutes or so until the Link runs out of batteries. 

# Picking Soil

This device works by calculating the resistance of an electrical current through the soil.  Dry soil is known to resist at a certain value.  The resistance can change depending on the soil salinity, temperature and, most importantly, moisture.  Soil types are defined in the United States by this handy chart. 

<center><img src='https://cloud.githubusercontent.com/assets/478212/7313765/e4cb7956-ea22-11e4-9285-72d3a541a0b6.jpg'></center>

If you don't know what kind of soil you have, CSU has this nifty chart:

<center><img src='https://cloud.githubusercontent.com/assets/478212/7313864/b9795a7e-ea23-11e4-8883-9039b66f8615.jpg'></center>

Source: <a href='http://www.ext.colostate.edu/mg/gardennotes/214.html'>Colorado Master Gardener Program</a>

# Troubleshooting

After interacting with Plant Link for some time, there are a few caveats I noticed.  The Plant Links will not mesh with each other, or any other battery powered Zigbee device.  You can use a Zigbee wall switch like a <a href='https://shop.smartthings.com/#!/products/smartpower-outlet'>SmartThings SmartPower</a> to mesh your network.  However, if you put your Links far away from the SmartThings Hub, you will probably have dropped reports from time to time.  

Getting the Links to pair can be tricky if you have already paired the devices with a Hub.  Just keep repeating the process, it will work if you are patient.  

Exposing your Links to excessive water can be a bad thing.  The housing is reasonably water proof, but if you leave it in standing water, particularly with fluctuating temperatures, water will get into the PCB and start to corode the components.  Mine have taken a pretty solid beating, but you will get more life out of your Links if you keep them away from standing water. 

# Notes

This is literally my third attempt at creating a PlantLink device type for SmartThings. I previously created a device that only read the PlantLink API, and the device was not paired with the SmartThings Hub.  Dan Widing at Oso Tech then worked with me to create a second device and connector SmartApp that would pair the Link with the ST Hub, but then communicate with the PlantLink API to get the resistivity measurements to then calculate moisture.  Although this was a noble goal, it had a lot of complexity that was difficult to work through. 

This attempt calculates the resistance of the soil based on the internal voltage reading from the PlantLink device.  Oso Tech very generously provided me with guidance on how to calculate the resistance given this measurement. 

# Bugs

The PlantLink device does not broadcast out a moisture reading.  It instead broadcasts a voltage that has to be converted to a resistance value based on a whole bunch of assumptions, including the soil type (which you defined) and temperature, salinity, etc. 

Temperature is currently not taken into account with this device.  This will not affect most Links that are inside, or Links that stay between 50°F and 100°F.  At 32°F, the PlantLink will report at or near 0%.  In between 32°F and 50°F, your PlantLink will probably report a lower moisture % than really exists. 

License
-------
Copyright (c) 2015, Kristopher Kubicki, Oso Tech, The Product Manufactory, SmartThings and Stan Dotson
All rights reserved.
