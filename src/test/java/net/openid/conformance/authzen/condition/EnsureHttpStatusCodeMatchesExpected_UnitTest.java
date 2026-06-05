package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
class EnsureHttpStatusCodeMatchesExpected_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureHttpStatusCodeMatchesExpected cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureHttpStatusCodeMatchesExpected();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putAcceptable(int actualStatus, int... codes) {
		JsonObject endpointResponse = new JsonObject();
		endpointResponse.addProperty("status", actualStatus);
		endpointResponse.addProperty("endpoint_name", "Authzen API");
		env.putObject("endpoint_response", endpointResponse);
		JsonArray arr = new JsonArray();
		for (int c : codes) {
			arr.add(c);
		}
		JsonObject wrapper = new JsonObject();
		wrapper.add("codes", arr);
		env.putObject("authzen_expected_http_status_codes", wrapper);
	}

	@Test
	public void singleAcceptableMatches_succeeds() {
		putAcceptable(400, 400);
		cond.execute(env);
	}

	@Test
	public void singleAcceptableMismatches_fails() {
		putAcceptable(404, 400);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void multipleAcceptable_actualIsInSet_succeeds() {
		putAcceptable(405, 400, 405);
		cond.execute(env);
	}

	@Test
	public void multipleAcceptable_actualNotInSet_fails() {
		putAcceptable(404, 400, 405);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void noExpectedCodesEnvKey_fails() {
		JsonObject endpointResponse = new JsonObject();
		endpointResponse.addProperty("status", 200);
		endpointResponse.addProperty("endpoint_name", "Authzen API");
		env.putObject("endpoint_response", endpointResponse);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void emptyCodesArray_fails() {
		JsonObject endpointResponse = new JsonObject();
		endpointResponse.addProperty("status", 200);
		endpointResponse.addProperty("endpoint_name", "Authzen API");
		env.putObject("endpoint_response", endpointResponse);
		JsonObject wrapper = new JsonObject();
		wrapper.add("codes", new JsonArray());
		env.putObject("authzen_expected_http_status_codes", wrapper);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
