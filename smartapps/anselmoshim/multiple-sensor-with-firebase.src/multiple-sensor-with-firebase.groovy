/**
 *  Copyright 2015 SmartThings
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
 *  Left It Open
 *
 *  Author: SmartThings
 *  Date: 2013-05-09
 */
 
//package org.codehaus.groovy.runtime.ResourceGroovyMethods
//package org.codehaus.groovy.control.MultipleCompilationErrorsException


definition(
    name: "Multiple Sensor with Firebase",
    namespace: "anselmoshim",
    author: "Anselmo Shim",
    description: "TBA",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage%402x.png"
)

preferences {

	section("When sink is on") {
    	input name: "sinkSwitch", type: "capability.switch", title: "Device?"
    }

	section("When stove is on") {
    	input name: "stoveSwitch", type: "capability.switch", title: "Device?"
    }

	section("When movement is detected...") {
		input name: "motion", type: "capability.motionSensor", title: "Device?"
	}	

	section("Door/Window open or closed") {
		input name: "contact", type: "capability.contactSensor", title: "Device?"
	}
}

def initialize() {
	subscribe(contact, "contact", doorStatus)
    subscribe(motion, "motion", motionStatus)
	subscribe(sinkSwitch, "switch", sinkStatus)
    subscribe(stoveSwitch, "switch", stoveStatus) 
}

def installed() {
	log.trace "installed()"
	initialize()
}

def updated() {
	log.trace "updated()"
	unsubscribe()
    initialize()
}
    
def doorClosed = ' { "door" : "close" } '
def doorOpen = ' { "door" : "open" } '

//def firebaseUrl = "https://home-risk.firebaseio.com/.json"
//def connection = firebaseUrl.toURL().openConnection()

//def firebaseUrl = new url("https://home-risk.firebaseio.com/.json");
//def connection = firebaseUrl.openConnection()
//connection.setDoOutput(true)
//connection.setRequestMethod("POST");

def sinkStatus(evt) {
	log.debug("crash here")
	if((sinkSwitch.valueOf("switch") == "on") && (motionStatus.valueOf("motion") == "inactive"))  {
    log.debug("Sink On & Motion Inactive")
    //FIX
    //minus score
    }
    else {
    log.debug("sink else")
    }

}

def stoveStatus(evt) {
	log.debug("stove start")
	if((sinkSwitch.valueOf("switch") == "on") && (motionStatus.valueOf("motion") == "inactive"))  {
    log.debug("Stove On & Motion Inactive")
    //FIX
    //deduct score
    }
    else {
    log.debug("stove else")
	}
}

def doorStatus(evt) {
	log.debug "doorStatus Loop"

  if(contact.value == "open") {
    log.debug("Door Opened")
    connection.outputStream.withWriter { Writer writer -> writer << doorOpen; };
    def response = connection.inputStream.withReader { Reader reader -> reader.text }
    return response;
	}
  if(contact.value == "closed") {
    log.debug("Door Closed")
    connection.outputStream.withWriter { Writer writer -> writer << doorClosed; };
    def response = connection.inputStream.withReader { Reader reader -> reader.text }
    return response;
	}
}

def motionStatus(evt) {
	if (evt.value == "active") {
	log.debug "${motion.label} is on"
	} else if (evt.value == "inactive") {
	log.debug "${motion.label} is off"
	}
}

def doorOpenTooLong(evt) {
	def contactState = contact.currentState("contact")
    def freq = (frequency != null && frequency != "") ? frequency * 60 : 600

	if (contactState.value == "open") {
		def elapsed = now() - contactState.rawDateCreated.time
		def threshold = ((openThreshold != null && openThreshold != "") ? openThreshold * 60000 : 60000) - 1000
		if (elapsed >= threshold) {
			log.debug "Contact has stayed open long enough since last check ($elapsed ms):  calling sendMessage()"
			sendMessage()
            runIn(freq, doorOpenTooLong, [overwrite: false])
		} else {
			log.debug "Contact has not stayed open long enough since last check ($elapsed ms):  doing nothing"
		}
	} else {
		log.warn "doorOpenTooLong() called but contact is closed:  doing nothing"
	}
}