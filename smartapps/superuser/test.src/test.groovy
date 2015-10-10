/**
 *  Poop
 *
 *  Copyright 2014 Anselmo Shim
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
 
definition(
    name: "Test",
    namespace: "",
    author: "Anselmo Shim",
    description: "Flux for home.",
    category: "Health & Wellness",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
}

preferences {
 	section("When all of these people leave home") {
    input "people", "capability.presenceSensor", multiple: true
  }

  section("Change to this mode to...") {
    input "newAwayMode",    "mode", title: "Everyone is away"
    input "newSunsetMode",  "mode", title: "At least one person home and nightfall"
    input "newSunriseMode", "mode", title: "At least one person home and sunrise"
    input "switches", "capability.switchLevel", title: "Switches", required: false, multiple: true //intensity level \ light level
  }

  section("Away threshold (defaults to 10 min)") {
    input "awayThreshold", "decimal", title: "Number of minutes", required: false
  }

  section("Zip code (for sunrise/sunset)") {
    input "zip", "decimal", title: "Zip code", required: false
  }

  section("Notifications") {
    input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
  }
}

def installed() {
 	init()
}

def updated() {
	unsubscribe()
 	init()
}

def init() {
  	subscribe(people, "presence", presence)
	checkSun();
}

//compares sunset/sunrise time against current time
def checkSun() {
  def zip     = settings.zip as String
  def sunInfo = getSunriseAndSunset(zipCode: zip)
  def current = now()

  if(sunInfo.sunrise.time > current ||
     sunInfo.sunset.time  < current) {
     state.sunMode = newSunsetMode
  }
  else {
    state.sunMode = newSunriseMode
  }

  log.info("Sunset: ${sunInfo.sunset.time}")
  log.info("Sunrise: ${sunInfo.sunrise.time}")
  log.info("Current: ${current}")
  log.info("sunMode: ${state.sunMode}")

  if(current < sunInfo.sunrise.time) {
    runIn(((sunInfo.sunrise.time - current) / 1000).toInteger(), setSunrise)
  }

  if(current < sunInfo.sunset.time) {
    runIn(((sunInfo.sunset.time - current) / 1000).toInteger(), setSunset)
  }

  schedule(timeTodayAfter(new Date(), "01:00", location.timeZone), checkSun)
  if (state.sunMode == "SunriseMode") {
	setSunrise()
  }
  else {
	setSunset()
  }
}

def setSunrise() {
  changeSunMode(newSunriseMode);
  	log.debug "Sunset handler"
    switches?.setLevel(20)
    state.Level = "20"
    // if state.motion is false
}

def setSunset() {
  changeSunMode(newSunsetMode);
    log.info "Executing sunrise handler"
    switches?.setLevel(0)
    state.Level = "off"
}

//sets the newMode depending on sunrise or sunset
def changeSunMode(newMode) {
  state.sunMode = newMode

  if(everyoneIsAway() && (location.mode == newAwayMode)) {
    log.debug("Mode is away, not evaluating")
  }

  else if(location.mode != newMode) {
    def message = "${app.label} changed your mode to '${newMode}'"
    send(message)
    setLocationMode(newMode)
  }

  else {
    log.debug("Mode is the same, not evaluating")
  }
}

//motion presence stuff below
def presence(evt) {
  if(evt.value == "not present") {
    log.debug("Checking if everyone is away")

    if(everyoneIsAway()) {
      log.info("Starting ${newAwayMode} sequence")
      def delay = (awayThreshold != null && awayThreshold != "") ? awayThreshold * 60 : 10 * 60
      runIn(delay, "setAway")
    }
  }

  else {
    if(location.mode != state.sunMode) {
      log.debug("Checking if anyone is home")

      if(anyoneIsHome()) {
        log.info("Starting ${state.sunMode} sequence")

        changeSunMode(state.sunMode)
      }
    }

    else {
      log.debug("Mode is the same, not evaluating")
    }
  }
}

def setAway() {
  if(everyoneIsAway()) {
    if(location.mode != newAwayMode) {
      def message = "${app.label} changed your mode to '${newAwayMode}' because everyone left home"
      log.info(message)
      send(message)
      setLocationMode(newAwayMode)
    }

    else {
      log.debug("Mode is the same, not evaluating")
    }
  }

  else {
    log.info("Somebody returned home before we set to '${newAwayMode}'")
  }
}

private everyoneIsAway() {
  def result = true

  if(people.findAll { it?.currentPresence == "present" }) {
    result = false
  }

  log.debug("everyoneIsAway: ${result}")

  return result
}

private anyoneIsHome() {
  def result = false

  if(people.findAll { it?.currentPresence == "present" }) {
    result = true
  }

  log.debug("anyoneIsHome: ${result}")

  return result
}

private send(msg) {
  if(sendPushMessage != "No") {
    log.debug("Sending push message")
    sendPush(msg)
  }

  log.debug(msg)
}