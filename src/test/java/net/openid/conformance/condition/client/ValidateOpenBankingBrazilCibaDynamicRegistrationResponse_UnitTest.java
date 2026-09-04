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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
			  "redirect_uris": [
			    "https://client.example/callback",
			    "https://client.example/alternative-callback"
			  ],
			  "jwks_uri": "https://directory.example/client.jwks",
			  "backchannel_token_delivery_mode": "ping",
			  "backchannel_client_notification_endpoint": "https://client.example/notify",
			  "backchannel_authentication_request_signing_alg": "PS256",
			  "backchannel_user_code_parameter": false,
			  "token_endpoint_auth_method": "private_key_jwt",
			  "token_endpoint_auth_signing_alg": "PS256",
			  "id_token_signed_response_alg": "PS256",
			  "id_token_encrypted_response_alg": "RSA-OAEP",
			  "id_token_encrypted_response_enc": "A256GCM",
			  "tls_client_certificate_bound_access_tokens": true
			}
			""").getAsJsonObject();
		response = request.deepCopy();
		response.addProperty("client_id", "registered-client");
		JsonObject softwareStatement = JsonParser.parseString("""
			{
			  "claims": {
			    "software_jwks_uri": "https://directory.example/client.jwks",
			    "software_redirect_uris": [
			      "https://client.example/callback",
			      "https://client.example/alternative-callback"
			    ]
			  }
			}
			""").getAsJsonObject();
		env.putObject("dynamic_registration_request", request);
		env.putObject("client", response);
		env.putObject("software_statement_assertion", softwareStatement);
	}

	@Test
	public void acceptsMatchingOpenBankingBrazilCibaMetadata() {
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void acceptsRedirectUrisThatAreASubsetOfSoftwareStatementUris() {
		response.add("redirect_uris", JsonParser.parseString("""
			[
			  "https://client.example/callback"
			]
			"""));

		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void rejectsRedirectUriOutsideSoftwareStatement() {
		response.add("redirect_uris", JsonParser.parseString("""
			[
			  "https://client.example/callback",
			  "https://attacker.example/callback"
			]
			"""));

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("not contained in software_redirect_uris");
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
	public void rejectsOpaqueHttpsNotificationEndpointWithoutHost() {
		response.addProperty("backchannel_client_notification_endpoint", "https:notify");

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
	public void rejectsInlineJwks() {
		response.add("jwks", JsonParser.parseString("{\"keys\":[]}"));

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void acceptsRequestJwksUriSubstitutionWhenResponseUsesSoftwareStatementValue() {
		env.getObject("dynamic_registration_request")
			.addProperty("jwks_uri", "https://client.example/requested.jwks");

		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void rejectsJwksUriThatDoesNotMatchSoftwareStatement() {
		response.addProperty("jwks_uri", "https://directory.example/different-client.jwks");

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("does not match software_jwks_uri");
	}

	@Test
	public void acceptsProfilePermittedAuthenticationMethodSubstitution() {
		response.addProperty("token_endpoint_auth_method", "self_signed_tls_client_auth");
		response.remove("token_endpoint_auth_signing_alg");

		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void rejectsAuthenticationMethodOutsideProfile() {
		response.addProperty("token_endpoint_auth_method", "client_secret_basic");

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsNonPs256PrivateKeyJwtAuthentication() {
		response.addProperty("token_endpoint_auth_signing_alg", "RS256");

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void acceptsProfileValuesSubstitutedForRequestedValues() {
		JsonObject request = env.getObject("dynamic_registration_request");
		request.addProperty("backchannel_authentication_request_signing_alg", "RS256");
		request.addProperty("id_token_signed_response_alg", "RS256");
		request.addProperty("id_token_encrypted_response_alg", "RSA-OAEP-256");

		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void rejectsChangedIdTokenEncryptionAlgorithm() {
		response.addProperty("id_token_encrypted_response_alg", "RSA-OAEP-256");

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsIdTokenSigningAlgorithmOutsideProfile() {
		response.addProperty("id_token_signed_response_alg", "RS256");

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsMissingIdTokenEncryptionMethod() {
		response.remove("id_token_encrypted_response_enc");

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsUnboundAccessTokens() {
		response.addProperty("tls_client_certificate_bound_access_tokens", false);

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsMissingRequiredResponseMetadata() {
		response.remove("jwks_uri");

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("does not contain required metadata: jwks_uri");
	}
}
