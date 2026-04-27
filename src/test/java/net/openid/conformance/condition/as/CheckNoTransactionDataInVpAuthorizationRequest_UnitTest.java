package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class CheckNoTransactionDataInVpAuthorizationRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckNoTransactionDataInVpAuthorizationRequest cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckNoTransactionDataInVpAuthorizationRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	@Test
	public void testEvaluate_passesWhenAbsent() {
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, new JsonObject());

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void testEvaluate_failsWhenPresent() {
		JsonObject params = JsonParser.parseString(
			"{\"transaction_data\": [\"eyJ0eXBlIjogImV4YW1wbGUifQ\"]}").getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, params);

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("does not support transaction_data");
	}
}
