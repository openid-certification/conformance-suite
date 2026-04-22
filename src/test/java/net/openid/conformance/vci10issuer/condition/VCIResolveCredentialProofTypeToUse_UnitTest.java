package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VCIResolveCredentialProofTypeToUse_UnitTest {

	private VCIResolveCredentialProofTypeToUse cond;

	@Mock
	private TestInstanceEventLog eventLog;

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIResolveCredentialProofTypeToUse();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		env.putObject("config", new JsonObject());
	}

	private JsonObject proofTypes(String... keys) {
		JsonObject obj = new JsonObject();
		for (String key : keys) {
			obj.add(key, new JsonObject());
		}
		return obj;
	}

	private JsonObject credentialConfigWithProofTypes(JsonObject proofTypesSupported) {
		JsonObject cc = new JsonObject();
		cc.add("cryptographic_binding_methods_supported", new JsonArray());
		cc.add("proof_types_supported", proofTypesSupported);
		return cc;
	}

	@Test
	public void testEvaluate_noCryptographicBinding_missingBothKeys_skipsResolution() {
		env.putObject("vci_credential_configuration", new JsonObject());

		cond.execute(env);

		assertFalse(env.getBoolean("vci_requires_cryptographic_binding"));
		assertNull(env.getString("vci_proof_type_key"));
	}

	@Test
	public void testEvaluate_noCryptographicBinding_missingProofTypesSupported_skipsResolution() {
		JsonObject cc = new JsonObject();
		cc.add("cryptographic_binding_methods_supported", new JsonArray());
		env.putObject("vci_credential_configuration", cc);

		cond.execute(env);

		assertFalse(env.getBoolean("vci_requires_cryptographic_binding"));
		assertNull(env.getString("vci_proof_type_key"));
	}

	@Test
	public void testEvaluate_emptyProofTypesSupported_throws() {
		env.putObject("vci_credential_configuration", credentialConfigWithProofTypes(proofTypes()));

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("empty"),
			"Expected error message to mention 'empty', got: " + err.getMessage());
	}

	@Test
	public void testEvaluate_requiredList_firstMatchWins() {
		env.putObject("vci_credential_configuration",
			credentialConfigWithProofTypes(proofTypes("jwt", "attestation")));
		env.putString("vci_required_proof_types", "jwt");

		cond.execute(env);

		assertEquals("jwt", env.getString("vci_proof_type_key"));
		assertTrue(env.getBoolean("vci_requires_cryptographic_binding"));
	}

	@Test
	public void testEvaluate_requiredList_preferenceOrder_secondWinsWhenFirstAbsent() {
		env.putObject("vci_credential_configuration",
			credentialConfigWithProofTypes(proofTypes("jwt")));
		// attestation preferred but not advertised; jwt advertised so jwt wins
		env.putString("vci_required_proof_types", "attestation,jwt");

		cond.execute(env);

		assertEquals("jwt", env.getString("vci_proof_type_key"));
	}

	@Test
	public void testEvaluate_requiredList_overridesHint_whenRequiredMatches() {
		env.putObject("vci_credential_configuration",
			credentialConfigWithProofTypes(proofTypes("jwt", "attestation")));
		env.putString("vci_required_proof_types", "jwt");
		env.putString("config", "vci.credential_proof_type_hint", "attestation");

		cond.execute(env);

		assertEquals("jwt", env.getString("vci_proof_type_key"));
	}

	@Test
	public void testEvaluate_requiredList_noneMatch_fallsBackToHint() {
		env.putObject("vci_credential_configuration",
			credentialConfigWithProofTypes(proofTypes("jwt")));
		env.putString("vci_required_proof_types", "attestation");
		env.putString("config", "vci.credential_proof_type_hint", "jwt");

		cond.execute(env);

		assertEquals("jwt", env.getString("vci_proof_type_key"));
	}

	@Test
	public void testEvaluate_requiredList_noneMatch_noHint_fallsBackToFirstAvailable() {
		env.putObject("vci_credential_configuration",
			credentialConfigWithProofTypes(proofTypes("jwt")));
		env.putString("vci_required_proof_types", "attestation");

		cond.execute(env);

		assertEquals("jwt", env.getString("vci_proof_type_key"));
	}

	@Test
	public void testEvaluate_requiredList_ignoresBlankAndWhitespaceEntries() {
		env.putObject("vci_credential_configuration",
			credentialConfigWithProofTypes(proofTypes("jwt", "attestation")));
		env.putString("vci_required_proof_types", " , attestation , ");

		cond.execute(env);

		assertEquals("attestation", env.getString("vci_proof_type_key"));
	}

	@Test
	public void testEvaluate_hint_advertised_wins() {
		env.putObject("vci_credential_configuration",
			credentialConfigWithProofTypes(proofTypes("jwt", "attestation")));
		env.putString("config", "vci.credential_proof_type_hint", "attestation");

		cond.execute(env);

		assertEquals("attestation", env.getString("vci_proof_type_key"));
	}

	@Test
	public void testEvaluate_hint_notAdvertised_throws() {
		env.putObject("vci_credential_configuration",
			credentialConfigWithProofTypes(proofTypes("jwt")));
		env.putString("config", "vci.credential_proof_type_hint", "attestation");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("not found"),
			"Expected error message to mention 'not found', got: " + err.getMessage());
	}

	@Test
	public void testEvaluate_noHint_noRequired_firstAvailableWins() {
		env.putObject("vci_credential_configuration",
			credentialConfigWithProofTypes(proofTypes("attestation", "jwt")));

		cond.execute(env);

		assertEquals("attestation", env.getString("vci_proof_type_key"));
	}

	@Test
	public void testEvaluate_setsResolvedProofTypeObject() {
		JsonObject proofTypesSupported = new JsonObject();
		JsonObject jwtEntry = new JsonObject();
		JsonArray algs = new JsonArray();
		algs.add("ES256");
		jwtEntry.add("proof_signing_alg_values_supported", algs);
		proofTypesSupported.add("jwt", jwtEntry);

		env.putObject("vci_credential_configuration",
			credentialConfigWithProofTypes(proofTypesSupported));

		cond.execute(env);

		assertEquals("jwt", env.getString("vci_proof_type_key"));
		JsonObject resolved = env.getObject("vci_proof_type");
		assertTrue(resolved.has("proof_signing_alg_values_supported"));
		assertEquals("ES256",
			OIDFJSON.getString(resolved.getAsJsonArray("proof_signing_alg_values_supported").get(0)));
	}
}
