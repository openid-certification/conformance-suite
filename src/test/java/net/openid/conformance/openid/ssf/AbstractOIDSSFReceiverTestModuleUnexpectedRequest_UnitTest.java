package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.TestModule.Result;
import net.openid.conformance.testmodule.TestModule.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
		@Override
		protected boolean isFinished() {
			return false;
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
	public void unknownWellKnownPathIsAnsweredWith404AndTheTestKeepsWaiting() {
		Object response = assertDoesNotThrow(() ->
			module.handleWellKnown("/.well-known/openid-configuration", request("GET"), null, null, requestParts("GET")));

		assertEquals(HttpStatus.NOT_FOUND, ((ResponseEntity<?>) response).getStatusCode());
		assertEquals(Status.WAITING, module.getStatus());
	}
}
