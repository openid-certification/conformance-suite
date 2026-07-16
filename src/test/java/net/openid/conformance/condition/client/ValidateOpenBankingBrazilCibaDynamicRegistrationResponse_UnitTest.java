package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidateOpenBankingBrazilCibaDynamicRegistrationResponse_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private ValidateOpenBankingBrazilCibaDynamicRegistrationResponse condition;
	private JsonObject response;

	@BeforeEach
	public void setUp() {
		condition = new ValidateOpenBankingBrazilCibaDynamicRegistrationResponse();
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		JsonObject request = JsonParser.parseString("""
			{
			  "grant_types": ["urn:openid:params:grant-type:ciba"],
			  "redirect_uris": ["https://client.example/callback"],
			  "jwks_uri": "https://directory.example/client.jwks",
			  "backchannel_token_delivery_mode": "ping",
			  "backchannel_client_notification_endpoint": "https://client.example/notify",
			  "backchannel_authentication_request_signing_alg": "PS256",
			  "backchannel_user_code_parameter": false,
			  "token_endpoint_auth_method": "private_key_jwt",
			  "token_endpoint_auth_signing_alg": "PS256",
			  "id_token_signed_response_alg": "PS256",
			  "tls_client_certificate_bound_access_tokens": true
			}
			""").getAsJsonObject();
		response = request.deepCopy();
		response.addProperty("client_id", "registered-client");
		env.putObject("dynamic_registration_request", request);
		env.putObject("client", response);
	}

	@Test
	public void acceptsMatchingOpenBankingBrazilCibaMetadata() {
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void acceptsOmittedUserCodeMetadataAsFalse() {
		response.remove("backchannel_user_code_parameter");

		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void rejectsMissingCibaGrant() {
		response.add("grant_types", JsonParser.parseString("[\"client_credentials\"]"));

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsChangedDeliveryMode() {
		response.addProperty("backchannel_token_delivery_mode", "poll");

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsMissingOrChangedNotificationEndpoint() {
		response.addProperty("backchannel_client_notification_endpoint", "http://client.example/notify");

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsChangedRequestSigningAlgorithm() {
		response.addProperty("backchannel_authentication_request_signing_alg", "RS256");

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsUserCodeSupport() {
		response.addProperty("backchannel_user_code_parameter", true);

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsInlineJwksOrChangedJwksUri() {
		response.add("jwks", JsonParser.parseString("{\"keys\":[]}"));

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsChangedAuthenticationMetadata() {
		response.addProperty("token_endpoint_auth_method", "tls_client_auth");

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsMissingRequiredResponseMetadata() {
		response.remove("jwks_uri");

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
