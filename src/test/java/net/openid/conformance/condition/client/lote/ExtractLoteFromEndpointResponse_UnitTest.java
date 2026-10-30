package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExtractLoteFromEndpointResponse_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private final Environment env = new Environment();
	private ExtractLoteFromEndpointResponse cond;

	@BeforeEach
	public void setUp() {
		cond = new ExtractLoteFromEndpointResponse();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		JsonObject lote = new JsonObject();
		lote.addProperty("slot", LoteSlot.PID.name());
		lote.addProperty("url", "https://lote.example.com/pid.jwt");
		env.putObject(AbstractLoteCondition.LOTE, lote);
	}

	private void respond(String body) {
		respond(200, body);
	}

	private void respond(int status, String body) {
		JsonObject response = new JsonObject();
		response.addProperty("status", status);
		response.addProperty("body", body);
		env.putObject(CallLoteEndpoint.RESPONSE, response);
	}

	@Test
	public void storesTheBodyTrimmedAsTheListsValue() {
		respond("eyJ.eyJ.sig\n");

		cond.execute(env);

		assertThat(env.getString(AbstractLoteCondition.LOTE, "value")).isEqualTo("eyJ.eyJ.sig");
	}

	@Test
	public void rejectsAnEmptyBody() {
		respond(" ");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("empty response body");
		assertThat(env.getString(AbstractLoteCondition.LOTE, "value")).isNull();
	}

	@Test
	public void storesNothingForAnErrorResponse() {
		respond(404, "{\"error\":\"not found\"}");

		cond.execute(env);

		assertThat(env.getString(AbstractLoteCondition.LOTE, "value")).isNull();
	}
}
