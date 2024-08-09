package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class FAPIValidateRequestObjectSigningAlg_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPIValidateRequestObjectSigningAlg cond;

	private JsonObject header;


	@BeforeEach
	public void setUp() throws Exception {

		cond = new FAPIValidateRequestObjectSigningAlg();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		header = JsonParser.parseString("{"
			+ "\"iss\":\"test-client-id-346334adgdsfgdfg3425\""
			+ "}").getAsJsonObject();
		header.addProperty("alg", "PS256");

	}

	private void addRequestObject(Environment env, JsonObject header) {
		JsonObject requestObject = new JsonObject();
		requestObject.getAsJsonObject().add("header", header);
		env.putObject("authorization_request_object", requestObject);
	}


	@Test
	public void testEvaluate_noError() {

		addRequestObject(env, header);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("authorization_request_object", "header.alg");
	}


	@Test
	public void testEvaluate_invalidAlg() {
		assertThrows(ConditionError.class, () -> {

			header.remove("exp");
			header.addProperty("alg", "RS256");

			addRequestObject(env, header);

			cond.execute(env);

		});

	}

}
