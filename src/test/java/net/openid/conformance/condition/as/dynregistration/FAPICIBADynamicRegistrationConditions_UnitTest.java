package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FAPICIBADynamicRegistrationConditions_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@Test
	public void cibaGrantAcceptsValidValue() {
		JsonArray grantTypes = new JsonArray();
		grantTypes.add("urn:openid:params:grant-type:ciba");
		assertValid(new FAPICIBAEnsureRegistrationRequestContainsCibaGrantType(), "grant_types", grantTypes);
	}

	@Test
	public void cibaGrantRejectsMissingValue() {
		assertInvalid(new FAPICIBAEnsureRegistrationRequestContainsCibaGrantType(), null, null);
	}

	@Test
	public void cibaGrantRejectsWrongType() {
		assertInvalid(new FAPICIBAEnsureRegistrationRequestContainsCibaGrantType(),
			"grant_types", "urn:openid:params:grant-type:ciba");
	}

	@Test
	public void cibaGrantRejectsInvalidValue() {
		JsonArray grantTypes = new JsonArray();
		grantTypes.add("authorization_code");
		assertInvalid(new FAPICIBAEnsureRegistrationRequestContainsCibaGrantType(), "grant_types", grantTypes);
	}

	@Test
	public void pingModeAcceptsValidValue() {
		assertValid(new FAPICIBAEnsureRegistrationRequestUsesPingMode(),
			"backchannel_token_delivery_mode", "ping");
	}

	@Test
	public void pingModeRejectsMissingValue() {
		assertInvalid(new FAPICIBAEnsureRegistrationRequestUsesPingMode(), null, null);
	}

	@Test
	public void pingModeRejectsWrongType() {
		assertInvalid(new FAPICIBAEnsureRegistrationRequestUsesPingMode(),
			"backchannel_token_delivery_mode", true);
	}

	@Test
	public void pingModeRejectsInvalidValue() {
		assertInvalid(new FAPICIBAEnsureRegistrationRequestUsesPingMode(),
			"backchannel_token_delivery_mode", "poll");
	}

	@Test
	public void notificationEndpointAcceptsValidValue() {
		assertValid(new FAPICIBAEnsureRegistrationRequestNotificationEndpointIsHttps(),
			"backchannel_client_notification_endpoint", "https://client.example/notify");
	}

	@Test
	public void notificationEndpointRejectsMissingValue() {
		assertInvalid(new FAPICIBAEnsureRegistrationRequestNotificationEndpointIsHttps(), null, null);
	}

	@Test
	public void notificationEndpointRejectsWrongType() {
		assertInvalid(new FAPICIBAEnsureRegistrationRequestNotificationEndpointIsHttps(),
			"backchannel_client_notification_endpoint", 42);
	}

	@Test
	public void notificationEndpointRejectsInvalidValue() {
		assertInvalid(new FAPICIBAEnsureRegistrationRequestNotificationEndpointIsHttps(),
			"backchannel_client_notification_endpoint", "http://client.example/notify");
	}

	@Test
	public void requestSigningAcceptsValidValue() {
		assertValid(new FAPICIBAEnsureRegistrationRequestSigningAlgIsPS256(),
			"backchannel_authentication_request_signing_alg", "PS256");
	}

	@Test
	public void requestSigningRejectsMissingValue() {
		assertInvalid(new FAPICIBAEnsureRegistrationRequestSigningAlgIsPS256(), null, null);
	}

	@Test
	public void requestSigningRejectsWrongType() {
		assertInvalid(new FAPICIBAEnsureRegistrationRequestSigningAlgIsPS256(),
			"backchannel_authentication_request_signing_alg", new JsonObject());
	}

	@Test
	public void requestSigningRejectsInvalidValue() {
		assertInvalid(new FAPICIBAEnsureRegistrationRequestSigningAlgIsPS256(),
			"backchannel_authentication_request_signing_alg", "RS256");
	}

	@Test
	public void userCodeAcceptsFalseValue() {
		assertValid(new FAPICIBAEnsureRegistrationRequestUserCodeIsAbsentOrFalse(),
			"backchannel_user_code_parameter", false);
	}

	@Test
	public void userCodeAcceptsMissingValue() {
		assertValid(new FAPICIBAEnsureRegistrationRequestUserCodeIsAbsentOrFalse(), null, null);
	}

	@Test
	public void userCodeRejectsWrongType() {
		assertInvalid(new FAPICIBAEnsureRegistrationRequestUserCodeIsAbsentOrFalse(),
			"backchannel_user_code_parameter", "false");
	}

	@Test
	public void userCodeRejectsInvalidValue() {
		assertInvalid(new FAPICIBAEnsureRegistrationRequestUserCodeIsAbsentOrFalse(),
			"backchannel_user_code_parameter", true);
	}

	private void assertValid(AbstractCondition condition, String fieldName, Object value) {
		Environment env = environmentWithRegistrationField(fieldName, value);
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		assertDoesNotThrow(() -> condition.execute(env));
	}

	private void assertInvalid(AbstractCondition condition, String fieldName, Object value) {
		Environment env = environmentWithRegistrationField(fieldName, value);
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	private Environment environmentWithRegistrationField(String fieldName, Object value) {
		Environment env = new Environment();
		JsonObject request = new JsonObject();
		if (fieldName != null) {
			if (value instanceof String stringValue) {
				request.addProperty(fieldName, stringValue);
			} else if (value instanceof Boolean booleanValue) {
				request.addProperty(fieldName, booleanValue);
			} else if (value instanceof Number numberValue) {
				request.addProperty(fieldName, numberValue);
			} else if (value instanceof JsonArray arrayValue) {
				request.add(fieldName, arrayValue);
			} else if (value instanceof JsonObject objectValue) {
				request.add(fieldName, objectValue);
			}
		}
		env.putObject("dynamic_registration_request", request);
		return env;
	}
}
