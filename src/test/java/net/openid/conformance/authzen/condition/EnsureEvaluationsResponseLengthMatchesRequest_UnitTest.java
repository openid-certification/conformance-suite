package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class EnsureEvaluationsResponseLengthMatchesRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureEvaluationsResponseLengthMatchesRequest cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureEvaluationsResponseLengthMatchesRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void put(String request, String response) {
		env.putObject("authzen_api_endpoint_request", JsonParser.parseString(request).getAsJsonObject());
		env.putObject("authzen_evaluations_endpoint_response", JsonParser.parseString(response).getAsJsonObject());
	}

	@Test
	public void sameLength_succeeds() {
		put("""
			{ "evaluations": [ {}, {} ] }""", """
			{ "evaluations": [ { "decision": true }, { "decision": false } ] }""");
		cond.execute(env);
	}

	@Test
	public void shorterResponse_fails() {
		put("""
			{ "evaluations": [ {}, {} ] }""", """
			{ "evaluations": [ { "decision": true } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void responseWithoutEvaluations_fails() {
		put("""
			{ "evaluations": [ {} ] }""", """
			{ "decision": true }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
