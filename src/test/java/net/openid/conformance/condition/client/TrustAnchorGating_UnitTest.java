package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
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

/**
 * The HAIP preconditions that used to demand a PEM trust anchor accept a list of trusted
 * entities URL for the same role instead, and RegisterCredentialTrustAnchor records that either
 * is configured.
 */
public class TrustAnchorGating_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@BeforeEach
	public void setUp() {
		env.putObject("config", new JsonObject());
	}

	private <T extends AbstractCondition> T cond(T cond) {
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		return cond;
	}

	@Test
	public void credentialTrustAnchorAcceptsPemOrEitherCredentialList() {
		assertThatThrownBy(() -> cond(new EnsureCredentialTrustAnchorConfigured()).execute(env))
			.isInstanceOf(ConditionError.class).hasMessageContaining("'PID providers LoTE URL'");

		env.putString("config", LoteSlot.MDL.configKey, "https://lote.example.com/mdl.jwt");
		assertThatCode(() -> cond(new EnsureCredentialTrustAnchorConfigured()).execute(env)).doesNotThrowAnyException();

		env.putString("config", LoteSlot.MDL.configKey, "");
		env.putString("config", "credential.trust_anchor_pem", "-----BEGIN CERTIFICATE-----");
		assertThatCode(() -> cond(new EnsureCredentialTrustAnchorConfigured()).execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void statusListTrustAnchorAcceptsPemOrEitherCredentialList() {
		assertThatThrownBy(() -> cond(new EnsureStatusListTrustAnchorConfigured()).execute(env))
			.isInstanceOf(ConditionError.class).hasMessageContaining("'Status List Trust Anchor'");

		env.putString("config", LoteSlot.PID.configKey, "https://lote.example.com/pid.jwt");
		assertThatCode(() -> cond(new EnsureStatusListTrustAnchorConfigured()).execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void keyAttestationTrustAnchorAcceptsPemOrTheWalletList() {
		assertThatThrownBy(() -> cond(new EnsureKeyAttestationTrustAnchorConfigured()).execute(env))
			.isInstanceOf(ConditionError.class).hasMessageContaining("'Wallet providers LoTE URL'");

		env.putString("config", LoteSlot.WALLET.configKey, "https://lote.example.com/wallet.jwt");
		assertThatCode(() -> cond(new EnsureKeyAttestationTrustAnchorConfigured()).execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void clientAttestationTrustAnchorAcceptsPemLegacyPemOrTheWalletList() {
		assertThatThrownBy(() -> cond(new EnsureClientAttestationTrustAnchorConfigured()).execute(env))
			.isInstanceOf(ConditionError.class).hasMessageContaining("'Client Attestation Trust Anchor'");

		env.putString("config", "vci.client_attestation_trust_anchor", "-----BEGIN CERTIFICATE-----");
		assertThatCode(() -> cond(new EnsureClientAttestationTrustAnchorConfigured()).execute(env)).doesNotThrowAnyException();

		env.putString("config", "vci.client_attestation_trust_anchor", "");
		env.putString("config", LoteSlot.WALLET.configKey, "https://lote.example.com/wallet.jwt");
		assertThatCode(() -> cond(new EnsureClientAttestationTrustAnchorConfigured()).execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void registerCredentialTrustAnchorRecordsThatATrustSourceIsConfigured() {
		cond(new RegisterCredentialTrustAnchor()).execute(env);
		assertThat(env.getString(RegisterCredentialTrustAnchor.ENV_TRUST_SOURCE_CONFIGURED)).isNull();

		env.putString("config", LoteSlot.PID.configKey, "https://lote.example.com/pid.jwt");
		cond(new RegisterCredentialTrustAnchor()).execute(env);
		assertThat(env.getString(RegisterCredentialTrustAnchor.ENV_TRUST_SOURCE_CONFIGURED)).isEqualTo("true");
		assertThat(env.getString("credential_trust_anchor_pem")).isNull();

		env.putString("config", "credential.trust_anchor_pem", "-----BEGIN CERTIFICATE-----");
		cond(new RegisterCredentialTrustAnchor()).execute(env);
		assertThat(env.getString("credential_trust_anchor_pem")).isEqualTo("-----BEGIN CERTIFICATE-----");
	}
}
