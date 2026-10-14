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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RegisterClientRequestObjectTrustAnchor_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private RegisterClientRequestObjectTrustAnchor cond;

	@BeforeEach
	public void setUp() {
		cond = new RegisterClientRequestObjectTrustAnchor();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		env.putObject("config", new JsonObject());
	}

	@Test
	public void aPemAndAWrpacListTogetherAreFine() {
		env.putString("config", "client.request_object_trust_anchor_pem", "-----BEGIN CERTIFICATE-----");
		env.putString("config", LoteSlot.WRPAC.configKey, "https://lote.example.com/wrpac.jwt");

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
		assertThat(env.getString("client_request_object_trust_anchor_pem")).isEqualTo("-----BEGIN CERTIFICATE-----");
	}

	@Test
	public void aRicalAndAWrpacListTogetherAreRejected() {
		env.putString("config", "client.rical_url", "https://rical.example.com/r.rical");
		env.putString("config", LoteSlot.WRPAC.configKey, "https://lote.example.com/wrpac.jwt");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("'WRPAC providers LoTE URL'");
	}

	@Test
	public void aRicalAndAPemTogetherAreStillRejected() {
		env.putString("config", "client.rical_url", "https://rical.example.com/r.rical");
		env.putString("config", "client.request_object_trust_anchor_pem", "-----BEGIN CERTIFICATE-----");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Request Object Trust Anchor'");
	}

	@Test
	public void nothingConfiguredRegistersNothing() {
		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
		assertThat(env.getString("client_request_object_trust_anchor_pem")).isNull();
	}
}
