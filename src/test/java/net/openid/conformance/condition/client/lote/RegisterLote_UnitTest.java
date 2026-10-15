package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RegisterLote_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private RegisterLote cond;

	@BeforeEach
	public void setUp() {
		cond = new RegisterLote();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		env.putString(RegisterLote.SLOT, LoteSlot.PID.name());
		env.putObject("config", new JsonObject());
	}

	@Test
	public void registersTheConfiguredUrlForTheSlot() {
		env.putString("config", LoteSlot.PID.configKey, " https://lote.example.com/pid.jwt ");

		cond.execute(env);

		assertThat(env.getString(AbstractLoteCondition.LOTE, "url")).isEqualTo("https://lote.example.com/pid.jwt");
		assertThat(env.getString(AbstractLoteCondition.LOTE, "slot")).isEqualTo("PID");
		assertThat(env.getString(AbstractLoteCondition.LOTE, "expected_type")).isEqualTo(LoteSlot.PID.expectedLoteType);
	}

	@Test
	public void mdlSlotExpectsTheCommissionsListType() {
		env.putString(RegisterLote.SLOT, LoteSlot.MDL.name());
		env.putString("config", LoteSlot.MDL.configKey, "https://lote.example.com/mdl.jwt");

		cond.execute(env);

		assertThat(env.getString(AbstractLoteCondition.LOTE, "expected_type"))
			.isEqualTo("http://trust.ec.europa.eu/lists/mDL/mDLProvidersListType");
	}

	@Test
	public void registersNothingWhenTheFieldIsAbsentOrBlank() {
		cond.execute(env);
		assertThat(env.getObject(AbstractLoteCondition.LOTE)).isNull();

		env.putString("config", LoteSlot.PID.configKey, "");
		cond.execute(env);
		assertThat(env.getObject(AbstractLoteCondition.LOTE)).isNull();
	}

	@Test
	public void rejectsAPlainHttpUrl() {
		env.putString("config", LoteSlot.PID.configKey, "http://lote.example.com/pid.jwt");

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'PID providers LoTE URL' field in the 'Credential Issuer' section")
			.hasMessageContaining("https://");
	}

	@Test
	public void worksThroughTheKeyMappingTheSequenceUses() {
		env.mapKey(AbstractLoteCondition.LOTE, LoteSlot.WRPAC.envKey);
		env.putString(RegisterLote.SLOT, LoteSlot.WRPAC.name());
		env.putString("config", LoteSlot.WRPAC.configKey, "https://lote.example.com/wrpac.jwt");

		cond.execute(env);
		env.unmapKey(AbstractLoteCondition.LOTE);

		assertThat(env.getObject(AbstractLoteCondition.LOTE)).isNull();
		assertThat(env.getString(LoteSlot.WRPAC.envKey, "url")).isEqualTo("https://lote.example.com/wrpac.jwt");
	}
}
