package net.openid.conformance.testmodule;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.sequence.Steps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class Endpoint_UnitTest {

	private final List<String> executed = new ArrayList<>();

	private EndpointModule module;

	@BeforeEach
	public void setUp() {
		module = new EndpointModule();
		module.setProperties("UNIT-TEST", Map.of("sub", "unit-test"), mock(TestInstanceEventLog.class), null,
			mock(TestInfoService.class), null, null);
		module.configure(new JsonObject(), null, null, null);
		module.start();
	}

	private static JsonObject request(String method) {
		return JsonParser.parseString("{\"method\": \"" + method + "\"}").getAsJsonObject();
	}

	@Test
	public void serve_runsTheStepsAndReturnsTheResponseTheyBuild() {
		Endpoint endpoint = Endpoint.of(
			Steps.action("Store method", env -> env.putString("method", env.getString(Endpoint.CURRENT_REQUEST, "method"))),
			env -> "response to " + env.getString("method"));

		assertEquals("response to GET", module.serve(endpoint, request("GET")));
		assertEquals("response to POST", module.serve(endpoint, request("POST")));
	}

	@Test
	public void serve_waitsForTheNextRequestUnlessTheEndpointFinishesTheTest() {
		module.serve(Endpoint.of(Steps.of(null), env -> "ok"), request("GET"));

		assertEquals(TestModule.Status.WAITING, module.getStatus());
	}

	@Test
	public void finishingTestWhen_decidesFromTheEnvironmentTheStepsLeft() {
		Endpoint endpoint = Endpoint.of(Steps.of(null), env -> "ok")
			.finishingTestWhen(env -> Integer.valueOf(2).equals(env.getInteger("count")));
		Environment env = new Environment();

		assertFalse(endpoint.finishesTest().test(env));
		env.putInteger("count", 2);
		assertTrue(endpoint.finishesTest().test(env));
		assertTrue(endpoint.finishingTest().finishesTest().test(new Environment()));
	}

	@Test
	public void requestAs_mapsTheKeysToTheRequestOnlyWhileTheBodyRuns() {
		Endpoint endpoint = Endpoint.of(
			Endpoint.requestAs("token_endpoint_request",
				Steps.action("Read request", env -> executed.add(env.getString("token_endpoint_request", "method")))),
			env -> env.getObject("token_endpoint_request") == null ? "unmapped" : "mapped");

		assertEquals("unmapped", module.serve(endpoint, request("POST")));
		assertEquals(List.of("POST"), executed);
	}

	@Test
	public void precededBy_runsTheChecksBeforeTheSteps() {
		Endpoint endpoint = Endpoint.of(Steps.action("steps", env -> executed.add("steps")), env -> "ok")
			.precededBy(Steps.action("checks", env -> executed.add("checks")));

		module.serve(endpoint, request("GET"));

		assertEquals(List.of("checks", "steps"), executed);
		assertFalse(endpoint.finishesTest().test(new Environment()));
	}

	@PublishTestModule(
		testName = "endpoint unit test module",
		displayName = "endpoint unit test module",
		profile = "UNIT-TEST"
	)
	public static class EndpointModule extends AbstractTestModule {
		@Override
		public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
			setStatus(Status.CONFIGURED);
		}

		@Override
		public void start() {
			setStatus(Status.RUNNING);
			setStatus(Status.WAITING);
		}
	}
}
