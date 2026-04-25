package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
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
public class AustraliaConnectIdWarnIfRequestObjectBindingMessageIsNotAscii_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AustraliaConnectIdWarnIfRequestObjectBindingMessageIsNotAscii cond;

	@BeforeEach
	public void setUp() {
		cond = new AustraliaConnectIdWarnIfRequestObjectBindingMessageIsNotAscii();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_ascii() {
		JsonObject requestObject = new JsonObject();
		JsonObject claims = new JsonObject();
		claims.addProperty("binding_message", "1234");
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_nonAscii() {
		JsonObject requestObject = new JsonObject();
		JsonObject claims = new JsonObject();
		claims.addProperty("binding_message", "Héllo");
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
