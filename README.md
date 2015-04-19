# PlantLink Direct Device
SmartThings Direct Implementation of the PlantLink Moisture Sensor.  This is an "offline" implementation of <a href='http://myplantlink.com/check-out-page/'>OsoTech's PlantLink device</a>.

This is literally my third attempt at creating a PlantLink device type for SmartThings. I previously created a device that only read the PlantLink API, and the device was not paired with the SmartThings Hub.  Dan Widing at OsoTech then worked with me to create a second device and connector SmartApp that would pair the Link with the ST Hub, but then communicate with the PlantLink API to get the resistivity measurements to then calculate moisture.  Although this was a noble goal, it had a lot of complexity that was difficult to work through. 

This attempt calculates the resistance of the soil based on the internal voltage reading from the PlantLink device.  OsoTech very generously provided me with guidance on how to calculate the resistance given this measurement.  The final step will be to calculate the moisture percentage, which the device does not do in its current state yet. 

