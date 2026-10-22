package net.openid.conformance.condition.as;

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
public class EnsureClientIdIsNotEmptyOnTokenRequestIfPresent_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureClientIdIsNotEmptyOnTokenRequestIfPresent cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureClientIdIsNotEmptyOnTokenRequestIfPresent();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putRequestWithClientId(String clientId) {
		JsonObject form = new JsonObject();
		form.addProperty("request", "eyJ...");
		if (clientId != null) {
			form.addProperty("client_id", clientId);
		}
		JsonObject request = new JsonObject();
		request.add("body_form_params", form);
		env.putObject("token_endpoint_request", request);
	}

	@Test
	public void testEvaluate_clientIdPresent() {
		putRequestWithClientId("client-a");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_clientIdAbsent() {
		putRequestWithClientId(null);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_clientIdEmpty() {
		putRequestWithClientId("");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
