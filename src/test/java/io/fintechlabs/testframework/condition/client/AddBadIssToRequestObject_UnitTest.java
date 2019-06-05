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
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AddBadIssToRequestObject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddBadIssToRequestObject cond;

	private String clientId = "21541757519";

	@Before
	public void setUp() throws Exception {
		cond = new AddBadIssToRequestObject();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		JsonObject client = new JsonObject();

		client.addProperty("client_id", clientId);

		env.putObject("client", client);
	}

	@Test
	public void testEvaluate_presentIssValueAndNotEqualClientId() {

		JsonObject requestObjectClaims = new JsonObject();

		env.putObject("request_object_claims", requestObjectClaims);

		cond.evaluate(env);

		assertThat(env.getObject("request_object_claims").has("iss")).isTrue();

		assertThat(env.getString("request_object_claims", "iss")).isNotEqualTo(clientId);

	}

}
