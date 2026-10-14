package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CaptureAuthzenResponseBodyForIdempotencyCheck_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static JsonObject json(String s) {
		return JsonParser.parseString(s).getAsJsonObject();
	}

	private void run(AbstractCondition cond) {
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		cond.execute(env);
	}

	private void fails(AbstractCondition cond) {
		assertThrows(ConditionError.class, () -> run(cond));
	}

	@Test
	public void storesTheFirstBodyOnly() {
		env.putObject("authzen_api_endpoint_response", json("{ \"body_json\": { \"results\": [ { \"name\": \"read\" } ] } }"));
		run(new CaptureAuthzenResponseBodyForIdempotencyCheck());
		env.putObject("authzen_api_endpoint_response", json("{ \"body_json\": { \"results\": [] } }"));
		run(new CaptureAuthzenResponseBodyForIdempotencyCheck());
		assertTrue(env.getString("authzen_idempotency_first_response_body").contains("read"));
	}

	@Test
	public void missingBody_fails() {
		env.putObject("authzen_api_endpoint_response", json("{ \"status\": 200 }"));
		fails(new CaptureAuthzenResponseBodyForIdempotencyCheck());
	}

	@Test
	public void nonObjectBody_fails() {
		env.putObject("authzen_api_endpoint_response", json("{ \"body_json\": [ 1, 2 ] }"));
		fails(new CaptureAuthzenResponseBodyForIdempotencyCheck());
	}
}
