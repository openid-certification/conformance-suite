package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class RemoveAudFromRequestObject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private RemoveAudFromRequestObject cond;

	@Before
	public void setUp() throws Exception {
		cond = new RemoveAudFromRequestObject();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_notPresentAudValue() {

		JsonObject requestObjectClaims = new JsonObject();

		requestObjectClaims.addProperty("aud", "https://fapidev-as.authlete.net/");

		env.putObject("request_object_claims", requestObjectClaims);

		cond.execute(env);

		assertThat(env.getObject("request_object_claims").has("aud")).isFalse();

	}

}
