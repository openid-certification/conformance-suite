package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EnsureClientAttestationIssuerConfigured_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private EnsureClientAttestationIssuerConfigured cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureClientAttestationIssuerConfigured();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		env.putObject("config", new JsonObject());
	}

	@Test
	public void missingIssuerFailsNamingTheField() {
		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Client Attestation Issuer'");
	}

	@Test
	public void aClearedFieldCountsAsMissing() {
		env.putString("config", "client_attestation.issuer", "");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Client Attestation Issuer'");
	}

	@Test
	public void aConfiguredIssuerPasses() {
		env.putString("config", "client_attestation.issuer", "https://wallet-provider.example.com");

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void theLegacyKeyIsReadAsAFallback() {
		env.putString("config", "vci.client_attestation_issuer", "https://wallet-provider.example.com");

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}
}
