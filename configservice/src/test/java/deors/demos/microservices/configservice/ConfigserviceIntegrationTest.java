package deors.demos.microservices.configservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ConfigserviceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(ConfigserviceIntegrationTest.class);

    protected static String TARGET_SERVER_URL;

    @BeforeClass
    public static void initEnvironment() {

        TARGET_SERVER_URL = getConfigurationProperty(
            "TARGET_SERVER_URL", "test.target.server.url", "http://localhost:8888/");

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

        assertEquals("call to /actuator/health should respond with code 200",
            HttpStatus.OK, response.getStatusCode());

        assertTrue("actuator health endpoint should confirm that the service is up",
            response.getBody().contains("\"status\":\"UP\""));
    }

    @Test
    public void testRemoteConfiguration() {

        TestRestTemplate restTemplate = new TestRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            TARGET_SERVER_URL + "bookrecservice/default",
            HttpMethod.GET, entity, String.class);

        assertEquals("call to /bookrecservice/default should respond with code 200",
            HttpStatus.OK, response.getStatusCode());

        assertTrue("config should contain the key with the right Spring Boot app name",
            response.getBody().contains("\"name\":\"bookrecservice\""));

        assertTrue("config should contain the key with the default HTTP port for the service",
            response.getBody().contains("\"server.port\":\"${PORT:8080}\""));
    }
}
