package deors.demos.microservices.configservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class BookrecedgeserviceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(BookrecedgeserviceIntegrationTest.class);

    protected static String TARGET_SERVER_URL;

    @BeforeAll
    public static void initEnvironment() {

        TARGET_SERVER_URL = getConfigurationProperty(
            "TARGET_SERVER_URL", "test.target.server.url", "http://localhost:8181/");

        logger.info("using target server at: " + TARGET_SERVER_URL);
    }

    private static String getConfigurationProperty(String envKey, String sysKey, String defValue) {

        String retValue = defValue;
        String envValue = System.getenv(envKey);
        String sysValue = System.getProperty(sysKey);
        // system property prevails over environment variable
        if (sysValue != null) {
            retValue = sysValue;
        } else if (envValue != null) {
            retValue = envValue;
        }
        return retValue;
    }

    private static boolean getConfigurationProperty(String envKey, String sysKey, boolean defValue) {

        boolean retValue = defValue;
        String envValue = System.getenv(envKey);
        String sysValue = System.getProperty(sysKey);
        // system property prevails over environment variable
        if (sysValue != null) {
            retValue = Boolean.parseBoolean(sysValue);
        } else if (envValue != null) {
            retValue = Boolean.parseBoolean(envValue);
        }
        return retValue;
    }

    @Test
    public void testEnvironment() {

        TestRestTemplate restTemplate = new TestRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            TARGET_SERVER_URL + "actuator/health",
            HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
            "call to /actuator/health should respond with code 200");

        assertTrue(response.getBody().contains("\"status\":\"UP\""),
            "actuator health endpoint should confirm that the service is up");
    }

    @Test
    public void testGetBookDefaultRecommendation() {

        TestRestTemplate restTemplate = new TestRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            TARGET_SERVER_URL + "bookrecedge",
            HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
            "call to /bookrecedge should respond with code 200");

        assertTrue(response.getBody().contains("\"title\":\"robots of dawn [test]\""),
            "returned data should include the default book title configured for tests");

        assertTrue(response.getBody().contains("\"author\":\"isaac asimov [test]\""),
            "returned data should include the default book author configured for tests");
    }
}
