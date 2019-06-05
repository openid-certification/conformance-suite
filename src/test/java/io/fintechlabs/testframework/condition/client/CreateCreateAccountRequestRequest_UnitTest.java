package io.fintechlabs.testframework.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CreateCreateAccountRequestRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateCreateAccountRequestRequest cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CreateCreateAccountRequestRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CreateCreateAccountRequestRequest#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate() {

		cond.evaluate(env);

		JsonElement permissions = env.getElementFromObject("account_requests_endpoint_request", "Data.Permissions");
		assertThat(permissions).isNotNull();
		assertThat(permissions.isJsonArray()).isTrue();
		assertThat(permissions.getAsJsonArray().contains(new JsonPrimitive("ReadAccountsBasic"))).isTrue();

	}

}
