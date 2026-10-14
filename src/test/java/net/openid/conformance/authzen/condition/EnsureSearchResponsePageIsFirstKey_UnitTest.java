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

@ExtendWith(MockitoExtension.class)
class EnsureSearchResponsePageIsFirstKey_UnitTest {

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
	public void pageFirst_succeeds() {
		env.putObject("authzen_search_endpoint_response", json("{ \"page\": { \"next_token\": \"\" }, \"results\": [] }"));
		run(new EnsureSearchResponsePageIsFirstKey());
	}

	@Test
	public void noPage_succeeds() {
		env.putObject("authzen_search_endpoint_response", json("{ \"results\": [] }"));
		run(new EnsureSearchResponsePageIsFirstKey());
	}

	@Test
	public void pageAfterResults_fails() {
		env.putObject("authzen_search_endpoint_response", json("{ \"results\": [], \"page\": { \"next_token\": \"\" } }"));
		fails(new EnsureSearchResponsePageIsFirstKey());
	}
}
