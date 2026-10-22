package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EnsureClientRequestObjectTrustAnchorConfigured_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private EnsureClientRequestObjectTrustAnchorConfigured cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureClientRequestObjectTrustAnchorConfigured();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		env.putObject("config", new JsonObject());
	}

	@Test
	public void nothingConfiguredFailsNamingEveryField() {
		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("'RICAL' or 'RICAL URL'")
			.hasMessageContaining("'Request Object Trust Anchor'")
			.hasMessageContaining("'" + LoteSlot.WRPAC.fieldLabel + "'");
	}

	@Test
	public void aRicalIsATrustSource() {
		env.putString("config", "client.rical_url", "https://rical.example.com/r.rical");

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void aWrpacListIsATrustSource() {
		env.putString("config", LoteSlot.WRPAC.configKey, "https://lote.example.com/wrpac.jwt");

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void aPemIsATrustSource() {
		env.putString("config", "client.request_object_trust_anchor_pem", "-----BEGIN CERTIFICATE-----");

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}
}
