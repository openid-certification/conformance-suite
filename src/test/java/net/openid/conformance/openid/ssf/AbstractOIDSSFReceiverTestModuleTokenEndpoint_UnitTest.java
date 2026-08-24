package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestModule.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the emulated token endpoint used in dynamic auth mode issues a token
 * for every well-formed client_credentials request. Receivers may request a
 * fresh token per SSF operation (e.g. one for stream creation and another for
 * stream deletion), so a second request must not trip the stale
 * {@code client_authentication} guard in
 * {@code ExtractClientCredentialsFromFormPost} ("Found existing client
 * authentication").
 */
public class AbstractOIDSSFReceiverTestModuleTokenEndpoint_UnitTest {

	private static final String CLIENT_ID = "ssf-test-client";
	private static final String CLIENT_SECRET = "ssf-test-secret";

	/** Minimal concrete subclass so the protected token endpoint handler can be exercised. */
	static class TestModule extends AbstractOIDSSFReceiverTestModule {
		@Override
		protected boolean isFinished() {
			return false;
		}

		void putEnvObject(String key, JsonObject value) {
			env.putObject(key, value);
		}

		ResponseEntity<?> tokenRequest(HttpServletRequest req, String requestId) {
			return handleTokenEndpointRequest(req, requestId);
		}
	}

	private TestModule module;

	@BeforeEach
	public void setUp() {
		module = new TestModule();
		TestInstanceEventLog eventLog = mock(TestInstanceEventLog.class);
		TestInfoService infoService = mock(TestInfoService.class);
		module.setProperties("UNIT-TEST", Map.of("sub", "unit-test"), eventLog, null, infoService, null, null);
		module.setupClientSecretPost();

		JsonObject client = new JsonObject();
		client.addProperty("client_id", CLIENT_ID);
		client.addProperty("client_secret", CLIENT_SECRET);
		module.putEnvObject("client", client);

		module.putEnvObject("ssf", new JsonObject());
	}

	private ResponseEntity<?> postTokenRequest(String requestId) {
		module.putEnvObject(requestId, JsonParser.parseString("""
			{
				"body_form_params": {
					"grant_type": "client_credentials",
					"client_id": "%s",
					"client_secret": "%s",
					"scope": "ssf.manage"
				}
			}
			""".formatted(CLIENT_ID, CLIENT_SECRET)).getAsJsonObject());

		HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getMethod()).thenReturn("POST");
		return module.tokenRequest(req, requestId);
	}

	@Test
	public void issuesTokenOnRepeatedRequestsWithoutFlaggingExistingClientAuthentication() {
		ResponseEntity<?> first = postTokenRequest("token_request_1");
		assertEquals(HttpStatus.OK, first.getStatusCode());

		ResponseEntity<?> second = postTokenRequest("token_request_2");
		assertEquals(HttpStatus.OK, second.getStatusCode());

		JsonObject secondBody = (JsonObject) second.getBody();
		assertNotNull(secondBody);
		assertNotNull(OIDFJSON.getString(secondBody.get("access_token")));

		assertNotEquals(Result.FAILED, module.getResult(),
			"second token request must not be flagged as a failure (stale client_authentication)");
	}
}
