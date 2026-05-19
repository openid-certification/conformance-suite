package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicatesReorderedParams_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicatesReorderedParams cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicatesReorderedParams();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setupEnvironment(String authEndpoint, String clientId, String requestUri) {
		JsonObject authRequest = new JsonObject();
		authRequest.addProperty("client_id", clientId);
		env.putObject("authorization_endpoint_request", authRequest);

		JsonObject requestObjectClaims = new JsonObject();
		requestObjectClaims.addProperty("client_id", clientId);
		env.putObject("request_object_claims", requestObjectClaims);

		JsonObject server = new JsonObject();
		server.addProperty("authorization_endpoint", authEndpoint);
		env.putObject("server", server);

		env.putString("request_uri", requestUri);
	}

	@Test
	public void testReverseAlphabeticalOrder() {
		setupEnvironment("https://wallet.example.com/authorize", "https://verifier.example.com", "urn:ietf:params:oauth:request_uri:abc123");

		cond.execute(env);

		String result = env.getString("redirect_to_authorization_endpoint");
		int clientIdPos = result.indexOf("client_id=");
		int requestUriPos = result.indexOf("request_uri=");
		assertTrue(clientIdPos >= 0, "URL should contain client_id: " + result);
		assertTrue(requestUriPos >= 0, "URL should contain request_uri: " + result);
		// reverse alphabetical: r > c, so request_uri comes first
		assertTrue(requestUriPos < clientIdPos, "request_uri should appear before client_id in reverse order: " + result);
	}

	@Test
	public void testStandardConditionUsesAlphabeticalOrder() {
		var standardCond = new BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates();
		standardCond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		setupEnvironment("https://wallet.example.com/authorize", "https://verifier.example.com", "urn:ietf:params:oauth:request_uri:abc123");

		standardCond.execute(env);

		String result = env.getString("redirect_to_authorization_endpoint");
		int clientIdPos = result.indexOf("client_id=");
		int requestUriPos = result.indexOf("request_uri=");
		// alphabetical: c < r, so client_id comes first
		assertTrue(clientIdPos < requestUriPos, "Standard condition should put client_id before request_uri (alphabetical): " + result);
	}
}
