package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class OpenBankingBrazilCibaForbiddenRequestParameterConditions_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@BeforeEach
	public void setUp() {
		env.putObject("authorization_endpoint_request", new JsonObject());
		env.putObject("request_object_claims", new JsonObject());
	}

	@Test
	public void testAddRequestedExp1sToAuthorizationEndpointRequest() {
		AddRequestedExp1sToAuthorizationEndpointRequest cond = new AddRequestedExp1sToAuthorizationEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);

		assertThat(env.getInteger("authorization_endpoint_request", "requested_expiry")).isEqualTo(1);
	}

	@Test
	public void testAddUserCodeToAuthorizationEndpointRequest() {
		AddUserCodeToAuthorizationEndpointRequest cond = new AddUserCodeToAuthorizationEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);

		assertThat(env.getString("authorization_endpoint_request", "user_code"))
			.isEqualTo(AddUserCodeToAuthorizationEndpointRequest.USER_CODE);
	}

	@Test
	public void testSetRequestObjectLoginHintToInvalidBrazilConsentId() {
		env.putString("consent_id", "urn:ofbr:consent:123e4567-e89b-12d3-a456-426614174005");
		env.getObject("request_object_claims").addProperty("login_hint", env.getString("consent_id"));

		SetRequestObjectLoginHintToInvalidBrazilConsentId cond =
			new SetRequestObjectLoginHintToInvalidBrazilConsentId();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);

		String loginHint = env.getString("request_object_claims", "login_hint");
		assertThat(loginHint)
			.isNotEqualTo(env.getString("consent_id"))
			.hasSameSizeAs(env.getString("consent_id"))
			.endsWith("0");
	}

	@Test
	public void testSetRequestObjectLoginHintToInvalidBrazilConsentIdAvoidsNoOpReplacement() {
		env.putString("consent_id", "urn:ofbr:consent:123e4567-e89b-12d3-a456-426614174000");

		SetRequestObjectLoginHintToInvalidBrazilConsentId cond =
			new SetRequestObjectLoginHintToInvalidBrazilConsentId();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);

		assertThat(env.getString("request_object_claims", "login_hint"))
			.isEqualTo("urn:ofbr:consent:123e4567-e89b-12d3-a456-426614174001");
	}

	@Test
	public void testCheckInvalidLoginHintErrorSucceeds() {
		JsonObject response = JsonParser.parseString("{\"error\":\"invalid_login_hint\"}").getAsJsonObject();
		env.putObject("backchannel_authentication_endpoint_response", response);

		CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidLoginHint cond =
			new CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidLoginHint();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);
	}

	@Test
	public void testCheckInvalidLoginHintErrorFailsForDifferentError() {
		JsonObject response = JsonParser.parseString("{\"error\":\"invalid_request\"}").getAsJsonObject();
		env.putObject("backchannel_authentication_endpoint_response", response);

		CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidLoginHint cond =
			new CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidLoginHint();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'error' field has unexpected value");
	}
}
