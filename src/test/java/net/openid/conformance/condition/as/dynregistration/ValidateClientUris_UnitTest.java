package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValidateClientUris_UnitTest {

	@Test
	public void foo() {
		Environment env = mock(Environment.class);
		JsonObject client = new JsonObject();
		client.addProperty("client_uri", "https://www.geru.com.br/");
		when(env.getObject("client")).thenReturn(client);

		ValidateClientUris validator = new ValidateClientUris();
		TestInstanceEventLog log = mock(TestInstanceEventLog.class);
		validator.setProperties("UNIT-TEST", log, Condition.ConditionResult.INFO);

		validator.evaluate(env);
	}

}
