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
public class AustraliaConnectIdEnsureRequestObjectSigningAlgIsPS256_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AustraliaConnectIdEnsureRequestObjectSigningAlgIsPS256 cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AustraliaConnectIdEnsureRequestObjectSigningAlgIsPS256();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_valid() {
		JsonObject requestObject = new JsonObject();
		JsonObject header = new JsonObject();
		header.addProperty("alg", "PS256");
		requestObject.add("header", header);
		env.putObject("authorization_request_object", requestObject);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_invalidAlg() {
		JsonObject requestObject = new JsonObject();
		JsonObject header = new JsonObject();
		header.addProperty("alg", "RS256");
		requestObject.add("header", header);
		env.putObject("authorization_request_object", requestObject);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingAlg() {
		JsonObject requestObject = new JsonObject();
		requestObject.add("header", new JsonObject());
		env.putObject("authorization_request_object", requestObject);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}
}
