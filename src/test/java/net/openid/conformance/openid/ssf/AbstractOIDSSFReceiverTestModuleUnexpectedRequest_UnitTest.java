package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import jakarta.servlet.http.HttpServletRequest;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestModule.Result;
import net.openid.conformance.testmodule.TestModule.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A request to a path or method the emulated transmitter does not serve (an OAuth library
 * probing {@code /.well-known/openid-configuration}, a HEAD or OPTIONS request) must be
 * answered with a 404 and graded, and the test must go on waiting for the real interaction -
 * not end INTERRUPTED. The receiver module's request handlers already hold the test lock in
 * RUNNING state when they fall through to the unexpected-request handling.
 */
public class AbstractOIDSSFReceiverTestModuleUnexpectedRequest_UnitTest {

	/** Minimal concrete subclass; a stray request cannot be provoked by the CI pairings. */
	static class TestModule extends AbstractOIDSSFReceiverTestModule {
		boolean finished;

		@Override
		protected boolean isFinished() {
			return finished;
		}

		@Override
		public String getName() {
			// the log entry a probe writes names the module; there is no @PublishTestModule here
			return "unit-test-module";
		}

		void waitForRequests() {
			setStatus(Status.WAITING);
		}

		void putEnvObject(String key, JsonObject value) {
			env.putObject(key, value);
		}
	}

	private TestModule module;

	@BeforeEach
	public void setUp() {
		module = new TestModule();
		TestInstanceEventLog eventLog = mock(TestInstanceEventLog.class);
		TestInfoService infoService = mock(TestInfoService.class);
		module.setProperties("UNIT-TEST", Map.of("sub", "unit-test"), eventLog, null, infoService, null, null);
		module.putEnvObject("ssf", new JsonObject());
		module.waitForRequests();
	}

	private static JsonObject requestParts(String method) {
		JsonObject requestParts = new JsonObject();
		requestParts.addProperty("method", method);
		requestParts.add("headers", new JsonObject());
		requestParts.add("query_string_params", new JsonObject());
		return requestParts;
	}

	private static HttpServletRequest request(String method) {
		HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getMethod()).thenReturn(method);
		return req;
	}

	@Test
	public void unknownPathIsAnsweredWith404AndTheTestKeepsWaiting() {
		Object response = assertDoesNotThrow(() ->
			module.handleHttp("no-such-endpoint", request("GET"), null, null, requestParts("GET")));

		assertEquals(HttpStatus.NOT_FOUND, ((ResponseEntity<?>) response).getStatusCode());
		assertEquals(Status.WAITING, module.getStatus());
		assertEquals(Result.FAILED, module.getResult(), "a stray request is graded, but must not interrupt the test");
	}

	@Test
	public void unknownWellKnownPathIsAProbeAnsweredWith404WithoutAGrade() {
		// an OAuth client library trying the OIDC discovery document before the configured
		// token endpoint is conformant (RFC 8414 5, CAEPIOP 2.7.1)
		Object response = assertDoesNotThrow(() ->
			module.handleWellKnown("/.well-known/openid-configuration", request("GET"), null, null, requestParts("GET")));

		assertEquals(HttpStatus.NOT_FOUND, ((ResponseEntity<?>) response).getStatusCode());
		assertEquals(Status.WAITING, module.getStatus());
		assertNotEquals(Result.FAILED, module.getResult(), "a discovery probe must not be graded");
	}

	@Test
	public void headRequestIsAProbeAnsweredWith404WithoutAGrade() {
		Object response = assertDoesNotThrow(() ->
			module.handleHttp("streams", request("HEAD"), null, null, requestParts("HEAD")));

		assertEquals(HttpStatus.NOT_FOUND, ((ResponseEntity<?>) response).getStatusCode());
		assertEquals(Status.WAITING, module.getStatus());
		assertNotEquals(Result.FAILED, module.getResult(), "a HEAD probe must not be graded");
	}

	@Test
	public void optionsRequestIsAProbeAnsweredWith404WithoutAGrade() {
		Object response = assertDoesNotThrow(() ->
			module.handleHttp("events", request("OPTIONS"), null, null, requestParts("OPTIONS")));

		assertEquals(HttpStatus.NOT_FOUND, ((ResponseEntity<?>) response).getStatusCode());
		assertEquals(Status.WAITING, module.getStatus());
		assertNotEquals(Result.FAILED, module.getResult(), "an OPTIONS probe must not be graded");
	}

	@Test
	public void transmitterMetadataStaysAvailableAfterTheTestFinished() {
		JsonObject metadata = new JsonObject();
		metadata.addProperty("issuer", "https://transmitter.example");
		JsonObject ssf = new JsonObject();
		ssf.add("transmitter_metadata", metadata);
		module.putEnvObject("ssf", ssf);
		module.finished = true;

		Object response = assertDoesNotThrow(() ->
			module.handleWellKnown("/.well-known/ssf-configuration/test/a/alias", request("GET"), null, null, requestParts("GET")));

		assertEquals(HttpStatus.OK, ((ResponseEntity<?>) response).getStatusCode());
		assertEquals(metadata, ((ResponseEntity<?>) response).getBody());
		assertEquals(Status.WAITING, module.getStatus(), "a re-read after the test finished must not touch the status machine");
	}

	@Test
	public void signingKeysStayAvailableAfterTheTestFinished() throws Exception {
		RSAKey key = new RSAKeyGenerator(2048).keyID("k1").generate();
		module.putEnvObject("server_jwks", JsonParser.parseString(new JWKSet(key).toString(false)).getAsJsonObject());
		module.finished = true;

		Object response = assertDoesNotThrow(() ->
			module.handleHttp("jwks", request("GET"), null, null, requestParts("GET")));

		assertEquals(HttpStatus.OK, ((ResponseEntity<?>) response).getStatusCode());
		JsonObject jwks = (JsonObject) ((ResponseEntity<?>) response).getBody();
		assertEquals("k1", OIDFJSON.getString(jwks.getAsJsonArray("keys").get(0).getAsJsonObject().get("kid")));
		assertFalse(jwks.getAsJsonArray("keys").get(0).getAsJsonObject().has("d"), "only the public keys are served");
	}

	@Test
	public void otherRequestsAfterTheTestFinishedGet204() {
		module.finished = true;

		Object response = assertDoesNotThrow(() ->
			module.handleHttp("streams", request("DELETE"), null, null, requestParts("DELETE")));

		assertEquals(HttpStatus.NO_CONTENT, ((ResponseEntity<?>) response).getStatusCode());
	}
}
