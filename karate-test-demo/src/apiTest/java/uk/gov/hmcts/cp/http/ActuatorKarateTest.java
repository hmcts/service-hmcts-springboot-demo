package uk.gov.hmcts.cp.http;

import com.intuit.karate.junit5.Karate;

class ActuatorKarateTest {

    @Karate.Test
    Karate testActuatorHealth() {
        return Karate.run("actuator-health").relativeTo(getClass());
    }
}