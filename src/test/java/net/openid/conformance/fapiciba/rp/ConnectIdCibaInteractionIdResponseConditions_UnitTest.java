package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectIdCibaInteractionIdResponseConditions_UnitTest {

	private static final String INTERACTION_ID = "c770aef3-6784-41f7-8e0e-ff5f97bddb3a";

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@BeforeEach
	public void setUp() {
		env.putString("fapi_interaction_id", INTERACTION_ID);
	}

	@Test
	public void testRemovesTokenResponseInteractionId() {
		env.putObject("token_endpoint_response_headers", headersWithInteractionId());

		execute(new RemoveFAPIInteractionIdFromTokenEndpointResponse());

		assertThat(env.getObject("token_endpoint_response_headers")
			.has("x-fapi-interaction-id")).isFalse();
	}

	@Test
	public void testSetsDifferentTokenResponseInteractionId() {
		env.putObject("token_endpoint_response_headers", headersWithInteractionId());

		execute(new SetDifferentFAPIInteractionIdInTokenEndpointResponse());

		assertDifferentValidUuid(env.getString("token_endpoint_response_headers",
			"x-fapi-interaction-id"));
	}

	@Test
	public void testRemovesUserInfoResponseInteractionId() {
		env.putObject("user_info_endpoint_response_headers", headersWithInteractionId());

		execute(new RemoveFAPIInteractionIdFromUserInfoEndpointResponse());

		assertThat(env.getObject("user_info_endpoint_response_headers")
			.has("x-fapi-interaction-id")).isFalse();
	}

	@Test
	public void testSetsDifferentUserInfoResponseInteractionId() {
		env.putObject("user_info_endpoint_response_headers", headersWithInteractionId());

		execute(new SetDifferentFAPIInteractionIdInUserInfoEndpointResponse());

		assertDifferentValidUuid(env.getString("user_info_endpoint_response_headers",
			"x-fapi-interaction-id"));
	}

	private JsonObject headersWithInteractionId() {
		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", INTERACTION_ID);
		return headers;
	}

	private void assertDifferentValidUuid(String value) {
		assertThat(value).isNotEqualToIgnoringCase(INTERACTION_ID);
		assertThat(UUID.fromString(value)).isNotNull();
	}

	private void execute(Condition condition) {
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		condition.execute(env);
	}
}
