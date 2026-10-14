package net.openid.conformance.vci10wallet.condition.clientattestation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class VCIRegisterClientAttestationTrustAnchor_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private VCIRegisterClientAttestationTrustAnchor cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIRegisterClientAttestationTrustAnchor();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		env.putObject("config", new JsonObject());
		env.putObject("vci", new JsonObject());
	}

	@Test
	public void registersTheConfiguredPem() {
		env.putString("config", "client_attestation.trust_anchor", "-----BEGIN CERTIFICATE-----");

		cond.execute(env);

		assertThat(env.getString("vci", "client_attestation_trust_anchor_pem")).isEqualTo("-----BEGIN CERTIFICATE-----");
	}

	@Test
	public void registersNothingWhenNoPemIsConfigured() {
		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
		assertThat(env.getString("vci", "client_attestation_trust_anchor_pem")).isNull();
	}
}
