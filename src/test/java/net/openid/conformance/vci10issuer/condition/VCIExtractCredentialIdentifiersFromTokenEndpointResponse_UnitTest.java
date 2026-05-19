package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VCIExtractCredentialIdentifiersFromTokenEndpointResponse_UnitTest {

	private static final String CONFIG_ID = "oid4vc_natural_person_sd";
	private static final String IDENTIFIER = "oid4vc_natural_person_sd_0000";

	private VCIExtractCredentialIdentifiersFromTokenEndpointResponse cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIExtractCredentialIdentifiersFromTokenEndpointResponse();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		env.putObject("client", new JsonObject());
	}

	private JsonObject tokenResponseWithAuthorizationDetails(String json) {
		JsonObject resp = new JsonObject();
		resp.addProperty("access_token", "irrelevant");
		resp.add("authorization_details", JsonParser.parseString(json));
		return resp;
	}

	@Test
	public void testEvaluate_recordsIdentifiersFromAuthorizationDetails() {
		env.putObject("token_endpoint_response", tokenResponseWithAuthorizationDetails(
			"[{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID
				+ "\",\"credential_identifiers\":[\"" + IDENTIFIER + "\"]}]"));

		cond.execute(env);

		JsonElement byConfigIdEl = env.getElementFromObject("client",
			VCIExtractCredentialIdentifiersFromTokenEndpointResponse.CLIENT_ENV_KEY);
		assertNotNull(byConfigIdEl);
		assertTrue(byConfigIdEl.isJsonObject());
		JsonElement identifiersEl = byConfigIdEl.getAsJsonObject().get(CONFIG_ID);
		assertNotNull(identifiersEl);
		assertTrue(identifiersEl.isJsonArray());
		JsonArray identifiers = identifiersEl.getAsJsonArray();
		assertEquals(1, identifiers.size());
		assertEquals(IDENTIFIER, OIDFJSON.getString(identifiers.get(0)));
	}

	@Test
	public void testEvaluate_absentAuthorizationDetails_isNoOp_preservesExisting() {
		// Pre-populate client.credential_identifiers_by_config_id from a prior token response
		JsonObject byConfigId = new JsonObject();
		JsonArray prior = new JsonArray();
		prior.add(IDENTIFIER);
		byConfigId.add(CONFIG_ID, prior);
		env.getObject("client").add(
			VCIExtractCredentialIdentifiersFromTokenEndpointResponse.CLIENT_ENV_KEY, byConfigId);

		// Subsequent token response (e.g. negative test) has no authorization_details
		JsonObject resp = new JsonObject();
		resp.addProperty("error", "invalid_grant");
		env.putObject("token_endpoint_response", resp);

		cond.execute(env);

		JsonElement identifiersEl = env.getElementFromObject("client",
			VCIExtractCredentialIdentifiersFromTokenEndpointResponse.CLIENT_ENV_KEY)
			.getAsJsonObject().get(CONFIG_ID);
		assertNotNull(identifiersEl);
		assertEquals(IDENTIFIER, OIDFJSON.getString(identifiersEl.getAsJsonArray().get(0)));
	}

	@Test
	public void testEvaluate_refreshResponse_replacesIdentifiersForSameConfigId() {
		// Existing entry from prior token call
		JsonObject byConfigId = new JsonObject();
		JsonArray prior = new JsonArray();
		prior.add("old_identifier");
		byConfigId.add(CONFIG_ID, prior);
		env.getObject("client").add(
			VCIExtractCredentialIdentifiersFromTokenEndpointResponse.CLIENT_ENV_KEY, byConfigId);

		env.putObject("token_endpoint_response", tokenResponseWithAuthorizationDetails(
			"[{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID
				+ "\",\"credential_identifiers\":[\"" + IDENTIFIER + "\"]}]"));

		cond.execute(env);

		JsonArray identifiers = env.getElementFromObject("client",
			VCIExtractCredentialIdentifiersFromTokenEndpointResponse.CLIENT_ENV_KEY)
			.getAsJsonObject().get(CONFIG_ID).getAsJsonArray();
		assertEquals(1, identifiers.size());
		assertEquals(IDENTIFIER, OIDFJSON.getString(identifiers.get(0)));
	}

	@Test
	public void testEvaluate_authorizationDetailsNotArray_throws() {
		JsonObject resp = new JsonObject();
		resp.add("authorization_details", new JsonObject());
		env.putObject("token_endpoint_response", resp);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_authorizationDetailsWithNoOpenidCredentialEntry_preservesExisting() {
		env.putObject("token_endpoint_response", tokenResponseWithAuthorizationDetails(
			"[{\"type\":\"some_other_type\",\"locations\":[\"https://example.com\"]}]"));

		cond.execute(env);

		// No prior data, so no entry created
		assertNull(env.getElementFromObject("client",
			VCIExtractCredentialIdentifiersFromTokenEndpointResponse.CLIENT_ENV_KEY));
	}

	@Test
	public void testEvaluate_nonStringType_isSkippedWithoutCrash() {
		env.putObject("token_endpoint_response", tokenResponseWithAuthorizationDetails(
			"[{\"type\":7,\"credential_configuration_id\":\"" + CONFIG_ID + "\","
				+ "\"credential_identifiers\":[\"" + IDENTIFIER + "\"]}]"));

		cond.execute(env);

		// Non-string type means we can't classify the entry as openid_credential, so it's skipped.
		assertNull(env.getElementFromObject("client",
			VCIExtractCredentialIdentifiersFromTokenEndpointResponse.CLIENT_ENV_KEY));
	}

	@Test
	public void testEvaluate_nonStringCredentialConfigurationId_isSkippedWithoutCrash() {
		env.putObject("token_endpoint_response", tokenResponseWithAuthorizationDetails(
			"[{\"type\":\"openid_credential\",\"credential_configuration_id\":42,"
				+ "\"credential_identifiers\":[\"" + IDENTIFIER + "\"]}]"));

		cond.execute(env);

		assertNull(env.getElementFromObject("client",
			VCIExtractCredentialIdentifiersFromTokenEndpointResponse.CLIENT_ENV_KEY));
	}

	@Test
	public void testEvaluate_openidCredentialWithoutCredentialIdentifiers_isSkipped() {
		// Per OID4VCI 1.0 Final §6.2 credential_identifiers is REQUIRED, but the extractor
		// treats malformed/missing entries as no-ops so it can keep running; the spec
		// violation is surfaced as a separate FAILURE entry by
		// VCIValidateOpenidCredentialAuthorizationDetailsInTokenEndpointResponse.
		env.putObject("token_endpoint_response", tokenResponseWithAuthorizationDetails(
			"[{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID + "\"}]"));

		cond.execute(env);

		assertNull(env.getElementFromObject("client",
			VCIExtractCredentialIdentifiersFromTokenEndpointResponse.CLIENT_ENV_KEY));
	}
}
