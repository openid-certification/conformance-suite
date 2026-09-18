package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FAPICIBAResourceInteractionId_UnitTest {
	private static final String EXPECTED_ID = "c770aef3-6784-41f7-8e0e-ff5f97bddb3a";
	private static final String DIFFERENT_ID = "93bac548-d2de-4546-b106-880a5018460d";

	@Test
	public void testBrazilCreatesHeadersForBothClients() throws Exception {
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
	public void testBrazilValidatesBothResponseIds() throws Exception {
		var behavior = new OpenBankingBrazilCibaServerProfileBehavior();
		for (boolean second : new boolean[]{false, true}) {
			var env = new Environment();
			env.putString("fapi_interaction_id", EXPECTED_ID);
			var response = new JsonObject();
			env.putObject("resource_endpoint_response_headers", response);
			response.addProperty("x-fapi-interaction-id", EXPECTED_ID);
			execute(behavior.validateResourceEndpointResponseHeaders(second), env);
			response.addProperty("x-fapi-interaction-id", "C770AEF3-6784-41F7-8E0E-FF5F97BDDB3A");
			execute(behavior.validateResourceEndpointResponseHeaders(second), env);
			for (String invalid : List.of("not-a-uuid", DIFFERENT_ID)) {
				response.addProperty("x-fapi-interaction-id", invalid);
				assertThatThrownBy(() -> execute(behavior.validateResourceEndpointResponseHeaders(second), env))
					.isInstanceOf(ConditionError.class);
			}
			response.remove("x-fapi-interaction-id");
			assertThatThrownBy(() -> execute(behavior.validateResourceEndpointResponseHeaders(second), env))
				.isInstanceOf(ConditionError.class);
			ConditionSequence checks = behavior.validateResourceEndpointResponseHeaders(second);
			checks.evaluate();
			assertThat(checks.getTestExecutionUnits()).hasSize(2);
			for (var unit : checks.getTestExecutionUnits()) {
				assertThat(((ConditionCallBuilder) unit).getOnFail()).isEqualTo(Condition.ConditionResult.FAILURE);
			}
		}
	}

	@Test
	public void testPlainAndUkKeepOptionalSecondClientHeader() throws Exception {
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
			execute(behavior.validateResourceEndpointResponseHeaders(true), env);
			response.remove("x-fapi-interaction-id");
			assertThatThrownBy(() -> execute(behavior.validateResourceEndpointResponseHeaders(true), env))
				.isInstanceOf(ConditionError.class);
		}
	}

	private void execute(Condition condition, Environment env) {
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), Condition.ConditionResult.INFO);
		condition.execute(env);
	}

	private void execute(ConditionSequence sequence, Environment env) throws Exception {
		if (sequence == null) {
			return;
		}
		sequence.evaluate();
		for (var unit : sequence.getTestExecutionUnits()) {
			var call = (ConditionCallBuilder) unit;
			execute(call.getConditionClass().getDeclaredConstructor().newInstance(), env);
		}
	}
}
