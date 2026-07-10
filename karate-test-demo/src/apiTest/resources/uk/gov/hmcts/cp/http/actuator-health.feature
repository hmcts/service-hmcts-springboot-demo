Feature: check actuator health of the running docker-compose stack

  Scenario: actuator health reports UP

    Given url baseUrl + '/actuator/health'
    When method get
    Then status 200
    And match response.status == 'UP'