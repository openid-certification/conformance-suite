package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckNotificationCallbackOnlyAuthReqId_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckNotificationCallbackOnlyAuthReqId cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckNotificationCallbackOnlyAuthReqId();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_caseNull() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("notification_callback", new JsonObject());

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseEmpty() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			o.add("body_json", new JsonObject());
			env.putObject("notification_callback", o);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseGood() {
		JsonObject o = new JsonObject();
		o.add("body_json", JsonParser.parseString("{\"auth_req_id\": \"1c266114-a1be-4252-8ad1-04986c5b9ac1\"}").getAsJsonObject());
		env.putObject("notification_callback", o);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseMultipleValues() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			o.add("body_json", JsonParser.parseString("{\"auth_req_id\": \"1c266114-a1be-4252-8ad1-04986c5b9ac1\",\"expires_in\": 3600}").getAsJsonObject());
			env.putObject("notification_callback", o);

			cond.execute(env);
		});
	}
}
