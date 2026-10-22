package net.openid.conformance.vci10wallet.condition.clientattestation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VCIRegisterClientAttestationTrustAnchor_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private VCIRegisterClientAttestationTrustAnchor cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIRegisterClientAttestationTrustAnchor();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		env.putObject("config", new JsonObject());
	}

	@Test
	public void registersTheConfiguredPem() {
		env.putString("config", "client_attestation.trust_anchor", "-----BEGIN CERTIFICATE-----");

		cond.execute(env);

		assertThat(env.getString("client_attestation_trust_anchor_pem")).isEqualTo("-----BEGIN CERTIFICATE-----");
	}

	@Test
	public void rejectsThePemTogetherWithTheWalletList() {
		env.putString("config", "client_attestation.trust_anchor", "-----BEGIN CERTIFICATE-----");
		env.putString("config", LoteSlot.WALLET.configKey, "https://lote.example.com/wallet.jwt");

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class).hasMessageContaining("More than one trust source");
	}

	@Test
	public void registersNothingWhenNoPemIsConfigured() {
		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
		assertThat(env.getString("client_attestation_trust_anchor_pem")).isNull();
	}
}
