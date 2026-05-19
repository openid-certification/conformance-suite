package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
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
public class VCIEnsureCredentialIdentifiersUnchangedAcrossTokenResponses_UnitTest {

	private static final String CONFIG_ID = "oid4vc_natural_person_sd";

	private VCIEnsureCredentialIdentifiersUnchangedAcrossTokenResponses cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureCredentialIdentifiersUnchangedAcrossTokenResponses();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		env.putObject("client", new JsonObject());
	}

	private void givenStoredIdentifiers(String configId, String... identifiers) {
		JsonObject byConfigId = new JsonObject();
		JsonArray arr = new JsonArray();
		for (String id : identifiers) {
			arr.add(id);
		}
		byConfigId.add(configId, arr);
		env.getObject("client").add(
			VCIExtractCredentialIdentifiersFromTokenEndpointResponse.CLIENT_ENV_KEY, byConfigId);
	}

	private void givenResponseAuthorizationDetails(String authorizationDetailsJson) {
		JsonObject resp = new JsonObject();
		resp.add("authorization_details", JsonParser.parseString(authorizationDetailsJson));
		env.putObject("token_endpoint_response", resp);
	}

	@Test
	public void testEvaluate_firstResponse_noPriorState_succeeds() {
		givenResponseAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"id_a\"]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_responseAuthorizationDetailsAbsent_isNoOp() {
		givenStoredIdentifiers(CONFIG_ID, "id_a");
		env.putObject("token_endpoint_response", new JsonObject());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_sameSet_succeeds() {
		givenStoredIdentifiers(CONFIG_ID, "id_a", "id_b");
		givenResponseAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"id_a\",\"id_b\"]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_sameSetDifferentOrder_succeeds() {
		givenStoredIdentifiers(CONFIG_ID, "id_a", "id_b");
		givenResponseAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"id_b\",\"id_a\"]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_differentIdentifier_fails() {
		givenStoredIdentifiers(CONFIG_ID, "id_a");
		givenResponseAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"id_b\"]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("differ"));
	}

	@Test
	public void testEvaluate_supersetIdentifiers_fails() {
		givenStoredIdentifiers(CONFIG_ID, "id_a");
		givenResponseAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"id_a\",\"id_b\"]}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_subsetIdentifiers_fails() {
		givenStoredIdentifiers(CONFIG_ID, "id_a", "id_b");
		givenResponseAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"id_a\"]}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_configIdNotPriorlyStored_isNoOp() {
		givenStoredIdentifiers(CONFIG_ID, "id_a");
		givenResponseAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\"another_credential\","
			+ "\"credential_identifiers\":[\"id_x\"]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_nonOpenidEntryIgnored() {
		givenStoredIdentifiers(CONFIG_ID, "id_a");
		givenResponseAuthorizationDetails("[{\"type\":\"some_other_type\"},"
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\"" + CONFIG_ID + "\","
			+ "\"credential_identifiers\":[\"id_a\"]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_nonStringType_isSkippedWithoutCrash() {
		givenStoredIdentifiers(CONFIG_ID, "id_a");
		givenResponseAuthorizationDetails("[{\"type\":7,\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"different\"]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_nonStringConfigurationId_isSkippedWithoutCrash() {
		givenStoredIdentifiers(CONFIG_ID, "id_a");
		givenResponseAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":42,"
			+ "\"credential_identifiers\":[\"different\"]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_sameMultisetWithDuplicates_succeeds() {
		givenStoredIdentifiers(CONFIG_ID, "id_a", "id_a", "id_b");
		givenResponseAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"id_b\",\"id_a\",\"id_a\"]}]");

		cond.execute(env);
	}
}
