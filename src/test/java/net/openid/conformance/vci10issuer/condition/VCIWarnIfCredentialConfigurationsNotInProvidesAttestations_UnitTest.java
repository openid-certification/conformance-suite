package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class VCIWarnIfCredentialConfigurationsNotInProvidesAttestations_UnitTest {

	private VCIWarnIfCredentialConfigurationsNotInProvidesAttestations cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIWarnIfCredentialConfigurationsNotInProvidesAttestations();
		cond.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.INFO);
	}

	@Test
	public void acceptsEveryConfigurationProvided() {
		cond.execute(env(IssuerInfoTestFixtures.providesAttestations().toString(), IssuerInfoTestFixtures.credentialConfigurations()));
	}

	@Test
	public void acceptsASuperset() {
		cond.execute(env("""
			[
			  { "format": "dc+sd-jwt", "meta": { "vct_values": ["urn:eudi:pid:1", "urn:other"] } },
			  { "format": "mso_mdoc", "meta": { "doctype_value": "eu.europa.ec.eudi.pid.1" } },
			  { "format": "mso_mdoc", "meta": { "doctype_value": "org.iso.18013.5.1.mDL" } }
			]
			""", IssuerInfoTestFixtures.credentialConfigurations()));
	}

	@Test
	public void rejectsAConfigurationNotProvided() {
		assertThrows(ConditionError.class, () -> cond.execute(env("""
			[ { "format": "dc+sd-jwt", "meta": { "vct_values": ["urn:eudi:pid:1"] } } ]
			""", IssuerInfoTestFixtures.credentialConfigurations())));
	}

	@Test
	public void rejectsAMatchingTypeInAnotherFormat() {
		assertThrows(ConditionError.class, () -> cond.execute(env("""
			[
			  { "format": "dc+sd-jwt", "meta": { "vct_values": ["urn:eudi:pid:1"] } },
			  { "format": "dc+sd-jwt", "meta": { "doctype_value": "eu.europa.ec.eudi.pid.1" } }
			]
			""", IssuerInfoTestFixtures.credentialConfigurations())));
	}

	@Test
	public void doesNotCompareOtherFormats() {
		cond.execute(env("[]", JsonParser.parseString("""
			{ "w3c": { "format": "jwt_vc_json", "credential_definition": { "type": ["VerifiableCredential"] } } }
			""").getAsJsonObject()));
	}

	private static Environment env(String providesAttestations, JsonObject configurations) {
		Environment env = new Environment();
		JsonObject dataset = new JsonObject();
		dataset.add("providesAttestations", JsonParser.parseString(providesAttestations));
		env.putObject(VCIEnsureIssuerInfoContainsOneRegistrarDataset.ENV_KEY, dataset);
		JsonObject metadata = new JsonObject();
		metadata.add("credential_configurations_supported", configurations);
		JsonObject vci = new JsonObject();
		vci.add("credential_issuer_metadata", metadata);
		env.putObject("vci", vci);
		return env;
	}
}
