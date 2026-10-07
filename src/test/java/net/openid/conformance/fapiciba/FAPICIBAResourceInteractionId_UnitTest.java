package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class FAPICIBAResourceInteractionId_UnitTest {
	private static final String EXPECTED_ID = "c770aef3-6784-41f7-8e0e-ff5f97bddb3a";
	private static final String DIFFERENT_ID = "93bac548-d2de-4546-b106-880a5018460d";

	@Test
	public void testBrazilCreatesHeadersForBothClients() {
		var behavior = new OpenBankingBrazilCibaServerProfileBehavior();
		var env = new Environment();
		String previous = null;
		for (boolean second : new boolean[]{false, true}) {
			execute(new CreateEmptyResourceEndpointRequestHeaders(), env);
			execute(behavior.addResourceEndpointProfileHeaders(second), env);
			String id = env.getString("resource_endpoint_request_headers", "x-fapi-interaction-id");
			assertThat(id).isNotNull().isNotEqualTo(previous);
			assertThat(UUID.fromString(id).version()).isEqualTo(4);
			assertThat(id).isEqualTo(env.getString("fapi_interaction_id"));
			assertThat(env.getObject("resource_endpoint_request_headers").has("x-fapi-auth-date"))
				.isEqualTo(!second);
			previous = id;
		}
	}

	@Test
	public void testBrazilValidatesBothResponseIds() {
		var behavior = new OpenBankingBrazilCibaServerProfileBehavior();
		for (boolean second : new boolean[]{false, true}) {
			var env = new Environment();
			env.putString("fapi_interaction_id", EXPECTED_ID);
			var response = new JsonObject();
			env.putObject("resource_endpoint_response_headers", response);
			response.addProperty("x-fapi-interaction-id", EXPECTED_ID);
			assertThat(execute(behavior.validateResourceEndpointResponseHeaders(second), env))
				.isEqualTo(TestModule.Result.UNKNOWN);
			response.addProperty("x-fapi-interaction-id", "C770AEF3-6784-41F7-8E0E-FF5F97BDDB3A");
			assertThat(execute(behavior.validateResourceEndpointResponseHeaders(second), env))
				.isEqualTo(TestModule.Result.UNKNOWN);
			for (String invalid : List.of("not-a-uuid", DIFFERENT_ID)) {
				response.addProperty("x-fapi-interaction-id", invalid);
				assertThat(execute(behavior.validateResourceEndpointResponseHeaders(second), env))
					.isEqualTo(TestModule.Result.FAILED);
			}
			response.remove("x-fapi-interaction-id");
			assertThat(execute(behavior.validateResourceEndpointResponseHeaders(second), env))
				.isEqualTo(TestModule.Result.FAILED);
		}
	}

	@Test
	public void testPlainAndUkKeepOptionalSecondClientHeader() {
		for (var behavior : List.of(new FAPICIBAServerProfileBehavior(), new OpenBankingUkCibaServerProfileBehavior())) {
			var env = new Environment();
			execute(new CreateEmptyResourceEndpointRequestHeaders(), env);
			execute(behavior.addResourceEndpointProfileHeaders(false), env);
			assertThat(env.getString("resource_endpoint_request_headers", "x-fapi-interaction-id")).isNotNull();
			execute(new CreateEmptyResourceEndpointRequestHeaders(), env);
			execute(behavior.addResourceEndpointProfileHeaders(true), env);
			assertThat(env.getObject("resource_endpoint_request_headers").has("x-fapi-interaction-id")).isFalse();
			var response = new JsonObject();
			response.addProperty("x-fapi-interaction-id", DIFFERENT_ID);
			env.putObject("resource_endpoint_response_headers", response);
			assertThat(execute(behavior.validateResourceEndpointResponseHeaders(true), env))
				.isEqualTo(TestModule.Result.UNKNOWN);
			response.remove("x-fapi-interaction-id");
			assertThat(execute(behavior.validateResourceEndpointResponseHeaders(true), env))
				.isEqualTo(TestModule.Result.FAILED);
		}
	}

	private void execute(Condition condition, Environment env) {
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), Condition.ConditionResult.INFO);
		condition.execute(env);
	}

	private TestModule.Result execute(ConditionSequence sequence, Environment env) {
		return new SequenceModule().execute(sequence, env);
	}

	private static class SequenceModule extends AbstractFAPICIBAID1 {
		@Override
		public String getName() {
			return "resource-interaction-id-test";
		}

		Result execute(ConditionSequence sequence, Environment environment) {
			env = environment;
			setProperties("UNIT-TEST", Map.of(), BsonEncoding.testInstanceEventLog(), null,
				mock(TestInfoService.class), null, null);
			setStatus(Status.CONFIGURED);
			setStatus(Status.RUNNING);
			try {
				call(sequence);
				return getResult();
			} finally {
				clearLockIfHeld();
			}
		}
	}
}
