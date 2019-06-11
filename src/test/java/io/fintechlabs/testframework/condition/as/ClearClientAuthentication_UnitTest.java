package io.fintechlabs.testframework.condition.as;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.assertNull;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ClearClientAuthentication_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ClearClientAuthentication cond;

	private JsonObject clientAuthentication;

	@Before
	public void setUp() throws Exception {

		cond = new ClearClientAuthentication();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		clientAuthentication = new JsonParser().parse("{\"client_id\": \"client\", \"client_auth\": \"client_auth\", \"method\": \"client:auth:method\"}").getAsJsonObject();

	}

	@Test
	public void test_good() {

		env.putString("client_authentication_success", "client:auth:method");
		env.putObject("client_authentication", clientAuthentication);
		cond.evaluate(env);

		verify(env, atLeastOnce()).removeNativeValue("client_authentication_success");
		verify(env, atLeastOnce()).removeObject("client_authentication");

		assertNull(env.getString("client_authentication_success"));
		assertNull(env.getObject("client_authentication"));
	}

	@Test
	public void test_onlySuccess() {

		env.putString("client_authentication_success", "client:auth:method");
		cond.evaluate(env);

		verify(env, atLeastOnce()).removeNativeValue("client_authentication_success");
		verify(env, atLeastOnce()).removeObject("client_authentication");

		assertNull(env.getString("client_authentication_success"));
		assertNull(env.getObject("client_authentication"));
	}

	@Test
	public void test_onlyAuth() {

		env.putObject("client_authentication", clientAuthentication);
		cond.evaluate(env);

		verify(env, atLeastOnce()).removeNativeValue("client_authentication_success");
		verify(env, atLeastOnce()).removeObject("client_authentication");

		assertNull(env.getString("client_authentication_success"));
		assertNull(env.getObject("client_authentication"));
	}

	@Test
	public void test_empty() {

		cond.evaluate(env);

		verify(env, atLeastOnce()).removeNativeValue("client_authentication_success");
		verify(env, atLeastOnce()).removeObject("client_authentication");

		assertNull(env.getString("client_authentication_success"));
		assertNull(env.getObject("client_authentication"));
	}

}
