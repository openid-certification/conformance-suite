package net.openid.conformance.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

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
	 * Test method for {@link CreateCreateAccountRequestRequest#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate() {

		cond.execute(env);

		JsonElement permissions = env.getElementFromObject("account_requests_endpoint_request", "Data.Permissions");
		assertThat(permissions).isNotNull();
		assertThat(permissions.isJsonArray()).isTrue();
		assertThat(permissions.getAsJsonArray().contains(new JsonPrimitive("ReadAccountsBasic"))).isTrue();

	}

}
