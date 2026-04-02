package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
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
public class AustraliaConnectIdValidateRequestObjectBindingMessage_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AustraliaConnectIdValidateRequestObjectBindingMessage cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AustraliaConnectIdValidateRequestObjectBindingMessage();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_valid() {
		JsonObject requestObject = new JsonObject();
		JsonObject claims = new JsonObject();
		claims.addProperty("binding_message", "1234");
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missing() {
		JsonObject requestObject = new JsonObject();
		requestObject.add("claims", new JsonObject());
		env.putObject("authorization_request_object", requestObject);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_tooShort() {
		JsonObject requestObject = new JsonObject();
		JsonObject claims = new JsonObject();
		claims.addProperty("binding_message", "12");
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_tooLong() {
		JsonObject requestObject = new JsonObject();
		JsonObject claims = new JsonObject();
		claims.addProperty("binding_message", "a".repeat(301));
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_notString() {
		JsonObject requestObject = new JsonObject();
		JsonObject claims = new JsonObject();
		claims.addProperty("binding_message", 1234);
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_nonAscii() {
		JsonObject requestObject = new JsonObject();
		JsonObject claims = new JsonObject();
		claims.addProperty("binding_message", "Héllo"); // non-ascii is allowed but logged
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);

		cond.execute(env);
	}
}
