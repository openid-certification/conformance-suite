package net.openid.conformance.vci10wallet.condition;

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
public class VCIValidateOpenidCredentialAuthorizationDetailsInIncomingRequest_UnitTest {

	private static final String CONFIG_ID = "oid4vc_natural_person_sd";

	private VCIValidateOpenidCredentialAuthorizationDetailsInIncomingRequest cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIValidateOpenidCredentialAuthorizationDetailsInIncomingRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		givenSupportedCredentialConfigurations(CONFIG_ID);
	}

	private void givenSupportedCredentialConfigurations(String... configIds) {
		JsonObject supported = new JsonObject();
		for (String id : configIds) {
			supported.add(id, new JsonObject());
		}
		JsonObject metadata = new JsonObject();
		metadata.add("credential_configurations_supported", supported);
		env.putObject("credential_issuer_metadata", metadata);
	}

	private void givenAuthorizationDetails(String authorizationDetailsJson) {
		JsonObject req = new JsonObject();
		req.add("authorization_details", JsonParser.parseString(authorizationDetailsJson));
		env.putObject("effective_authorization_endpoint_request", req);
	}

	@Test
	public void testEvaluate_validEntry_succeeds() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID + "\"}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_validEntryWithLocations_succeeds() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"locations\":[\"https://issuer.example.com\"]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingAuthorizationDetails_fails() {
		env.putObject("effective_authorization_endpoint_request", new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_authorizationDetailsNotArray_fails() {
		JsonObject req = new JsonObject();
		req.add("authorization_details", new JsonObject());
		env.putObject("effective_authorization_endpoint_request", req);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_entryNotObject_fails() {
		givenAuthorizationDetails("[\"string_entry\"]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingType_fails() {
		givenAuthorizationDetails("[{\"credential_configuration_id\":\"" + CONFIG_ID + "\"}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_nonStringType_fails() {
		givenAuthorizationDetails("[{\"type\":7,\"credential_configuration_id\":\"" + CONFIG_ID + "\"}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("not a string"));
	}

	@Test
	public void testEvaluate_noOpenidCredentialEntries_fails() {
		givenAuthorizationDetails("[{\"type\":\"payment_initiation\",\"locations\":[\"x\"]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("openid_credential"));
	}

	@Test
	public void testEvaluate_openidEntryMissingCredentialConfigurationId_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\"}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_openidEntryEmptyCredentialConfigurationId_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\"\"}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_duplicateConfigIds_fails() {
		givenAuthorizationDetails("["
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID + "\"},"
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID + "\"}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("Multiple"));
	}

	@Test
	public void testEvaluate_requestContainsCredentialIdentifiers_fails() {
		// credential_identifiers is response-only per OID4VCI §6.2 — wallet must not send it.
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"x\"]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("response-only"));
	}

	@Test
	public void testEvaluate_configIdNotInSupportedMetadata_fails() {
		givenSupportedCredentialConfigurations("some_other_id");
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\"}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("not advertised"));
	}

	@Test
	public void testEvaluate_credentialConfigurationsSupportedMissing_fails() {
		env.putObject("credential_issuer_metadata", new JsonObject());
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\"}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_claimsNotArray_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"claims\":\"not_an_array\"}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("'claims' is present but not a JSON array"));
	}

	@Test
	public void testEvaluate_claimsIsObject_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"claims\":{\"path\":[\"x\"]}}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_claimsEmptyArray_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"claims\":[]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("empty array"));
	}

	@Test
	public void testEvaluate_claimsContainsNonObjectEntry_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"claims\":[\"not_an_object\"]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("is not a JSON object"));
	}

	@Test
	public void testEvaluate_validClaims_succeeds() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"claims\":[{\"path\":[\"given_name\"]}]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_validClaimsWithMandatoryBoolean_succeeds() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"claims\":[{\"path\":[\"given_name\"],\"mandatory\":true}]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_claimsDescriptionMissingPath_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"claims\":[{}]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("missing required 'path'"));
	}

	@Test
	public void testEvaluate_claimsDescriptionPathNotArray_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"claims\":[{\"path\":\"given_name\"}]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("'path' is not a JSON array"));
	}

	@Test
	public void testEvaluate_claimsDescriptionPathEmptyArray_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"claims\":[{\"path\":[]}]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("'path' is an empty array"));
	}

	@Test
	public void testEvaluate_claimsDescriptionMandatoryNotBoolean_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"claims\":[{\"path\":[\"given_name\"],\"mandatory\":\"true\"}]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("'mandatory' is not a boolean"));
	}

	@Test
	public void testEvaluate_pathComponentBoolean_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"claims\":[{\"path\":[true]}]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("invalid element"));
	}

	@Test
	public void testEvaluate_pathComponentObject_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"claims\":[{\"path\":[{}]}]}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_pathComponentArray_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"claims\":[{\"path\":[[]]}]}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_pathComponentFractionalNumber_fails() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"claims\":[{\"path\":[1.5]}]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("invalid element"));
	}

	@Test
	public void testEvaluate_pathComponentsMixedValidTypes_succeeds() {
		// string, null, and integer are all valid per Appendix C
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"claims\":[{\"path\":[\"namespace\",null,0]}]}]");

		cond.execute(env);
	}
}
