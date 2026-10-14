package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ExtractAuthzenApiEndpointDecisionResponse_UnitTest {

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
	public void objectBody_isExtracted() {
		env.putObject("authzen_api_endpoint_response", json("{ \"body_json\": { \"decision\": true } }"));
		run(new ExtractAuthzenApiEndpointDecisionResponse());
		assertTrue(OIDFJSON.getBoolean(env.getObject("authzen_api_endpoint_decision").get("decision")));
	}

	@Test
	public void missingBody_fails() {
		env.putObject("authzen_api_endpoint_response", json("{ \"status\": 200 }"));
		fails(new ExtractAuthzenApiEndpointDecisionResponse());
	}

	@Test
	public void arrayBody_fails() {
		env.putObject("authzen_api_endpoint_response", json("{ \"body_json\": [ { \"decision\": true } ] }"));
		fails(new ExtractAuthzenApiEndpointDecisionResponse());
	}
}
