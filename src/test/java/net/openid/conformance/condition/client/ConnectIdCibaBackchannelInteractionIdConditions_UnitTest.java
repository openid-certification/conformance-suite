package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConnectIdCibaBackchannelInteractionIdConditions_UnitTest {

	private static final String INTERACTION_ID = "c770aef3-6784-41f7-8e0e-ff5f97bddb3a";

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@BeforeEach
	public void setUp() {
		env.putString("fapi_interaction_id", INTERACTION_ID);
	}

	@Test
	public void testAddsInteractionIdToRequest() {
		env.putObject("backchannel_authentication_endpoint_request_headers", new JsonObject());

		execute(new AddFAPIInteractionIdToBackchannelAuthenticationEndpointRequest());

		assertThat(env.getString("backchannel_authentication_endpoint_request_headers",
			"x-fapi-interaction-id")).isEqualTo(INTERACTION_ID);
	}

	@Test
	public void testAcceptsMatchingInteractionIdResponse() {
		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", INTERACTION_ID.toUpperCase());
		env.putObject("backchannel_authentication_endpoint_response_headers", headers);

		execute(new CheckForFAPIInteractionIdInBackchannelAuthenticationResponse());
		execute(new EnsureMatchingFAPIInteractionIdBackchannelAuthenticationEndpoint());
	}

	@Test
	public void testRejectsMissingInteractionIdResponse() {
		env.putObject("backchannel_authentication_endpoint_response_headers", new JsonObject());

		assertThatThrownBy(() ->
			execute(new CheckForFAPIInteractionIdInBackchannelAuthenticationResponse()))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("not found");
	}

	@Test
	public void testRejectsMalformedInteractionIdResponse() {
		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", "not-a-uuid");
		env.putObject("backchannel_authentication_endpoint_response_headers", headers);

		assertThatThrownBy(() ->
			execute(new CheckForFAPIInteractionIdInBackchannelAuthenticationResponse()))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("not a UUID");
	}

	@Test
	public void testRejectsMismatchedInteractionIdResponse() {
		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", "93bac548-d2de-4546-b106-880a5018460d");
		env.putObject("backchannel_authentication_endpoint_response_headers", headers);

		assertThatThrownBy(() ->
			execute(new EnsureMatchingFAPIInteractionIdBackchannelAuthenticationEndpoint()))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("Mismatch");
	}

	private void execute(Condition condition) {
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		condition.execute(env);
	}
}
