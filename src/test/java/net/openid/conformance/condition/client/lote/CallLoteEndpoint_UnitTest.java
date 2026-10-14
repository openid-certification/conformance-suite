package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonObject;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit5.HoverflyExtension;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.badRequest;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(HoverflyExtension.class)
public class CallLoteEndpoint_UnitTest {

	private static final String LIST = LoteTestFixtures.INSTANCE.emptyPidList(
		LoteTestFixtures.INSTANCE.generateSigner("EU", "LOTE User", null));

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private CallLoteEndpoint cond;
	private Environment env;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
			service("lote.example.com")
				.get("/pid.jwt")
				.willReturn(success()
					.header("Content-Type", "application/jwt")
					.body(LIST + "\n")),
			service("missing.example.com")
				.get("/pid.jwt")
				.willReturn(badRequest())));
		hoverfly.resetJournal();

		cond = new CallLoteEndpoint();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private void register(String url) {
		JsonObject lote = new JsonObject();
		lote.addProperty("slot", LoteSlot.PID.name());
		lote.addProperty("url", url);
		env.putObject(AbstractLoteCondition.LOTE, lote);
	}

	@Test
	public void fetchesTheListAndStoresTheResponse() {
		register("https://lote.example.com/pid.jwt");

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();

		assertThat(env.getString(CallLoteEndpoint.RESPONSE, "body")).isEqualTo(LIST + "\n");
		assertThat(env.getString(CallLoteEndpoint.RESPONSE, "headers.content-type")).isEqualTo("application/jwt");
		assertThat(env.getString(AbstractLoteCondition.LOTE, "value")).isNull();
	}

	@Test
	public void recordsAnHttpErrorResponseForTheStatusCheck() {
		register("https://missing.example.com/pid.jwt");

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();

		assertThat(env.getInteger(CallLoteEndpoint.RESPONSE, "status")).isEqualTo(400);
	}
}
