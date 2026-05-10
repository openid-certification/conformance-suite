package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VCICreateCredentialRequest_UnitTest {

	private static final String CONFIG_ID = "oid4vc_natural_person_sd";
	private static final String IDENTIFIER = "oid4vc_natural_person_sd_0000";

	private VCICreateCredentialRequest cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCICreateCredentialRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		env.putObject("config", new JsonObject());
		env.putObject("vci", new JsonObject());
		env.putObject("client", new JsonObject());
		env.putString("vci_credential_configuration_id", CONFIG_ID);
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

	@Test
	public void testEvaluate_usesFirstIdentifier_whenStoredForConfigId() {
		givenStoredIdentifiers(CONFIG_ID, IDENTIFIER, "another_identifier");

		cond.execute(env);

		JsonObject req = env.getObject("vci_credential_request_object");
		assertNotNull(req);
		assertEquals(IDENTIFIER, OIDFJSON.getString(req.get("credential_identifier")));
		assertFalse(req.has("credential_configuration_id"));
	}

	@Test
	public void testEvaluate_fallsBackToConfigurationId_whenNoStoredIdentifiers() {
		// client object exists but credential_identifiers_by_config_id is absent

		cond.execute(env);

		JsonObject req = env.getObject("vci_credential_request_object");
		assertNotNull(req);
		assertEquals(CONFIG_ID, OIDFJSON.getString(req.get("credential_configuration_id")));
		assertFalse(req.has("credential_identifier"));
	}

	@Test
	public void testEvaluate_fallsBackToConfigurationId_whenStoredButNotForThisConfigId() {
		givenStoredIdentifiers("some_other_config", "some_other_identifier");

		cond.execute(env);

		JsonObject req = env.getObject("vci_credential_request_object");
		assertEquals(CONFIG_ID, OIDFJSON.getString(req.get("credential_configuration_id")));
		assertFalse(req.has("credential_identifier"));
	}

	@Test
	public void testEvaluate_throwsWhenStoredIdentifiersListIsEmpty() {
		givenStoredIdentifiers(CONFIG_ID);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_omitsProofs_whenCryptographicBindingNotRequired() {
		givenStoredIdentifiers(CONFIG_ID, IDENTIFIER);

		cond.execute(env);

		assertNull(env.getElementFromObject("vci_credential_request_object", "proofs"));
	}

	@Test
	public void testEvaluate_includesProofs_whenCryptographicBindingRequired() {
		givenStoredIdentifiers(CONFIG_ID, IDENTIFIER);
		env.putBoolean("vci_requires_cryptographic_binding", true);
		JsonObject proofs = new JsonObject();
		JsonArray jwts = new JsonArray();
		jwts.add("eyJ-fake-jwt");
		proofs.add("jwt", jwts);
		env.putObject("credential_request_proofs", proofs);

		cond.execute(env);

		assertTrue(env.getElementFromObject("vci_credential_request_object", "proofs").isJsonObject());
		assertEquals("eyJ-fake-jwt", OIDFJSON.getString(env.getElementFromObject(
			"vci_credential_request_object", "proofs.jwt").getAsJsonArray().get(0)));
	}
}
