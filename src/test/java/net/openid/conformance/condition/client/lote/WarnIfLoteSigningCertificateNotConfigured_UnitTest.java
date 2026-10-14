package net.openid.conformance.condition.client.lote;

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

public class WarnIfLoteSigningCertificateNotConfigured_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private WarnIfLoteSigningCertificateNotConfigured cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnIfLoteSigningCertificateNotConfigured();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.WARNING);
		env.putObject("config", new JsonObject());
	}

	@Test
	public void warnsWhenNothingIsConfigured() {
		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("'LoTE signing certificate'");
	}

	@Test
	public void warnsWhenTheFieldIsBlank() {
		env.putString("config", LoteSlot.SIGNING_CERTIFICATE_CONFIG_KEY, "  ");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
	}

	@Test
	public void passesWhenACertificateIsConfigured() {
		env.putString("config", LoteSlot.SIGNING_CERTIFICATE_CONFIG_KEY, "-----BEGIN CERTIFICATE-----\nMIIB\n-----END CERTIFICATE-----");

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}
}
