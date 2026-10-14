package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VCICheckKeyAttestationJwksIfKeyAttestationIsRequired_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private VCICheckKeyAttestationJwksIfKeyAttestationIsRequired cond;

	@BeforeEach
	public void setUp() {
		cond = new VCICheckKeyAttestationJwksIfKeyAttestationIsRequired();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putBoolean("vci_requires_cryptographic_binding", true);
		env.putString("vci_proof_type_key", "jwt");
		env.putObject("vci_proof_type", JsonParser.parseString("{\"key_attestations_required\":{}}").getAsJsonObject());
	}

	@Test
	public void usesTheConfiguredKeyAttestationJwks() {
		env.putObject("config", JsonParser.parseString(
			"{\"client_attestation\":{\"key_attestation_jwks\":{\"keys\":[{\"kty\":\"oct\",\"k\":\"abc\"}]}}}").getAsJsonObject());

		cond.execute(env);

		assertThat(env.getObject("vci_key_attestation_jwks").toString()).isEqualTo("{\"keys\":[{\"kty\":\"oct\",\"k\":\"abc\"}]}");
	}

	@Test
	public void fallsBackToTheBuiltInWalletProviderCertificateWhenNoneIsConfigured() {
		env.putObject("config", new JsonObject());

		cond.execute(env);

		JsonObject jwks = env.getObject("vci_key_attestation_jwks");
		assertThat(jwks.getAsJsonArray("keys")).hasSize(1);
		assertThat(OIDFJSON.getString(jwks.getAsJsonArray("keys").get(0).getAsJsonObject().get("x5c").getAsJsonArray().get(0)))
			.isEqualTo(TestKeysAndCerts.getWalletAttestationSigner().getJwk().getX509CertChain().get(0).toString());
	}

	@Test
	public void doesNothingWhenNoKeyAttestationIsRequired() {
		env.putObject("vci_proof_type", new JsonObject());
		env.putObject("config", new JsonObject());

		cond.execute(env);

		assertThat(env.getObject("vci_key_attestation_jwks")).isNull();
	}
}
