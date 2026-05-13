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
public class VCIWarnOnAuthorizationDetailsInTokenEndpointResponseConventions_UnitTest {

	private static final String CONFIG_ID = "oid4vc_natural_person_sd";
	private static final String IDENTIFIER = "oid4vc_natural_person_sd_0000";

	private VCIWarnOnAuthorizationDetailsInTokenEndpointResponseConventions cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIWarnOnAuthorizationDetailsInTokenEndpointResponseConventions();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
		env.putObject("vci", new JsonObject());
		env.putString("vci_credential_configuration_id", CONFIG_ID);
	}

	private void givenAuthorizationDetails(String authorizationDetailsJson) {
		JsonObject resp = new JsonObject();
		resp.add("authorization_details", JsonParser.parseString(authorizationDetailsJson));
		env.putObject("token_endpoint_response", resp);
	}

	@Test
	public void testEvaluate_validResponse_succeeds() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"" + IDENTIFIER + "\"]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_authorizationDetailsAbsent_isNoOp() {
		env.putObject("token_endpoint_response", new JsonObject());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_unrequestedConfigId_warns() {
		givenAuthorizationDetails("["
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID + "\","
			+ "\"credential_identifiers\":[\"" + IDENTIFIER + "\"]},"
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\"some_other_credential\","
			+ "\"credential_identifiers\":[\"some_other_identifier\"]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("conventions"));
	}

	@Test
	public void testEvaluate_duplicateIdentifiersInArray_warns() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"" + IDENTIFIER + "\",\"" + IDENTIFIER + "\"]}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_unknownTypeEntry_warns() {
		givenAuthorizationDetails("["
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID + "\","
			+ "\"credential_identifiers\":[\"" + IDENTIFIER + "\"]},"
			+ "{\"type\":\"some_other_type\",\"locations\":[\"https://example.com\"]}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_unknownFieldOnEntry_warns() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"" + IDENTIFIER + "\"],"
			+ "\"unknown_field\":\"value\"}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_knownClaimsField_doesNotWarn() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"" + IDENTIFIER + "\"],"
			+ "\"claims\":[]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rfc9396CommonFields_doNotWarn() {
		// RFC 9396 §2.2 common fields — any authorization_details type MAY include these.
		// The suite itself emits 'locations' in VCIGenerateRichAuthorizationRequestForCredential.
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"" + IDENTIFIER + "\"],"
			+ "\"locations\":[\"https://issuer.example.com\"],"
			+ "\"actions\":[\"read\"],"
			+ "\"datatypes\":[\"x\"],"
			+ "\"identifier\":\"res-1\","
			+ "\"privileges\":[\"basic\"]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_credentialIdentifierAppearsInTwoEntries_warns() {
		// Pre-populate the request with two requested credential_configuration_ids so the
		// "unrequested config_id" finding does not also fire and pollute this assertion.
		JsonObject rar = new JsonObject();
		rar.add("payload", JsonParser.parseString("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID + "\"},"
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\"second_credential\"}]"));
		env.putObject("rar", rar);

		givenAuthorizationDetails("["
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID + "\","
			+ "\"credential_identifiers\":[\"shared_identifier\"]},"
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\"second_credential\","
			+ "\"credential_identifiers\":[\"shared_identifier\"]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("conventions"));
	}

	@Test
	public void testEvaluate_nonStringType_doesNotCrash() {
		givenAuthorizationDetails("[{\"type\":7,\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"" + IDENTIFIER + "\"]}]");

		// Non-string type is treated as null (not openid_credential, not a known unrequested type).
		// We must not crash; whether a finding is raised depends on collected unrequested-types
		// which won't include null. So this should pass clean.
		cond.execute(env);
	}

	@Test
	public void testEvaluate_nonStringConfigurationId_doesNotCrash() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":42,"
			+ "\"credential_identifiers\":[\"" + IDENTIFIER + "\"]}]");

		// Non-string credential_configuration_id is silently skipped for the unrequested-config-id check.
		cond.execute(env);
	}

	@Test
	public void testEvaluate_rarPayloadDefinesAdditionalRequestedConfigIds_doesNotWarn() {
		// Simulate the request having included two credential_configuration_ids via authorization_details
		JsonObject rar = new JsonObject();
		rar.add("payload", JsonParser.parseString("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID + "\"},"
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\"second_credential\"}]"));
		env.putObject("rar", rar);

		givenAuthorizationDetails("["
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID + "\","
			+ "\"credential_identifiers\":[\"" + IDENTIFIER + "\"]},"
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\"second_credential\","
			+ "\"credential_identifiers\":[\"second_identifier\"]}]");

		cond.execute(env);
	}
}
