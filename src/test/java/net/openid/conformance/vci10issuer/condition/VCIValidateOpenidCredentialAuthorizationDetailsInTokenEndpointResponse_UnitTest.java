package net.openid.conformance.vci10issuer.condition;

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
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VCIValidateOpenidCredentialAuthorizationDetailsInTokenEndpointResponse_UnitTest {

	private static final String CONFIG_ID = "oid4vc_natural_person_sd";
	private static final String IDENTIFIER = "oid4vc_natural_person_sd_0000";

	private VCIValidateOpenidCredentialAuthorizationDetailsInTokenEndpointResponse cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIValidateOpenidCredentialAuthorizationDetailsInTokenEndpointResponse();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		env.putObject("vci", new JsonObject());
		env.putString("vci_credential_configuration_id", CONFIG_ID);
	}

	private void givenAuthorizationDetails(String authorizationDetailsJson) {
		JsonObject resp = new JsonObject();
		resp.addProperty("access_token", "irrelevant");
		resp.add("authorization_details", JsonParser.parseString(authorizationDetailsJson));
		env.putObject("token_endpoint_response", resp);
	}

	@Test
	public void testEvaluate_validSingleEntry_succeeds() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"" + IDENTIFIER + "\"]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingAuthorizationDetails_fails() {
		env.putObject("token_endpoint_response", new JsonObject());

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("missing"));
	}

	@Test
	public void testEvaluate_authorizationDetailsNotArray_fails() {
		JsonObject resp = new JsonObject();
		resp.add("authorization_details", new JsonObject());
		env.putObject("token_endpoint_response", resp);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_entryNotObject_fails() {
		givenAuthorizationDetails("[\"not_an_object\"]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_entryMissingType_fails() {
		givenAuthorizationDetails("[{\"credential_configuration_id\":\"" + CONFIG_ID
			+ "\",\"credential_identifiers\":[\"" + IDENTIFIER + "\"]}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_nonStringType_failsCleanly() {
		// Issuer returns "type": 7 — must be reported as a conformance failure rather than
		// escape as a generic framework UnexpectedJsonTypeException.
		givenAuthorizationDetails("[{\"type\":7,\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"" + IDENTIFIER + "\"]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("not a string"));
	}

	@Test
	public void testEvaluate_otherTypeIgnored_butStillFailsIfNoMatchingOpenidEntry() {
		givenAuthorizationDetails("[{\"type\":\"some_other_type\",\"locations\":[\"https://example.com\"]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("matching"));
	}

	@Test
	public void testEvaluate_openidEntryMissingCredentialConfigurationId_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_identifiers\":[\"" + IDENTIFIER + "\"]}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_openidEntryEmptyCredentialConfigurationId_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\"\","
			+ "\"credential_identifiers\":[\"" + IDENTIFIER + "\"]}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_openidEntryMissingCredentialIdentifiers_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID + "\"}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_openidEntryEmptyCredentialIdentifiers_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[]}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_credentialIdentifierNonString_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[42]}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_credentialIdentifierEmptyString_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"\"]}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_duplicateCredentialConfigurationIds_fails() {
		givenAuthorizationDetails("["
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID + "\","
			+ "\"credential_identifiers\":[\"" + IDENTIFIER + "\"]},"
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID + "\","
			+ "\"credential_identifiers\":[\"another_one\"]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("Multiple"));
	}

	@Test
	public void testEvaluate_noEntryMatchesRequestedConfigId_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\"some_other_credential\","
			+ "\"credential_identifiers\":[\"some_other_identifier\"]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("matching"));
	}

	@Test
	public void testEvaluate_multipleOpenidEntries_oneMatchesRequest_succeeds() {
		givenAuthorizationDetails("["
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\"unrelated_credential\","
			+ "\"credential_identifiers\":[\"unrelated_identifier\"]},"
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID + "\","
			+ "\"credential_identifiers\":[\"" + IDENTIFIER + "\"]}]");

		cond.execute(env);
	}
}
