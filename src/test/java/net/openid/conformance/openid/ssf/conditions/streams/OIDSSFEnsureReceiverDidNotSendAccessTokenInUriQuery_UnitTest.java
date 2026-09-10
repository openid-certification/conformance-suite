package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureReceiverDidNotSendAccessTokenInUriQuery_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureReceiverDidNotSendAccessTokenInUriQuery createCondition() {
		OIDSSFEnsureReceiverDidNotSendAccessTokenInUriQuery condition = new OIDSSFEnsureReceiverDidNotSendAccessTokenInUriQuery();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepare(Boolean inQuery) {
		JsonObject authResult = new JsonObject();
		if (inQuery != null) {
			authResult.addProperty("access_token_in_query", inQuery);
		}
		JsonObject ssf = new JsonObject();
		ssf.add("auth_result", authResult);
		env.putObject("ssf", ssf);
	}

	@Test
	void failsWhenTokenWasSentInQuery() {
		prepare(true);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void passesWhenFlagIsAbsent() {
		prepare(null);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void passesWhenFlagIsFalse() {
		prepare(false);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}
}
