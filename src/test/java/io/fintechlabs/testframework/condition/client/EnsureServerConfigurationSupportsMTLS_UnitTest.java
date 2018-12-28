package io.fintechlabs.testframework.condition.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class EnsureServerConfigurationSupportsMTLS_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureServerConfigurationSupportsMTLS cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new EnsureServerConfigurationSupportsMTLS("UNIT-TEST", eventLog, ConditionResult.INFO);

		env.putObject("server", new JsonObject());
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsMTLS#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		JsonArray methods = new JsonArray();
		methods.add("client_secret_basic");
		methods.add("tls_client_auth");
		methods.add("pub_key_tls_client_auth");

		env.getObject("server").add("token_endpoint_auth_methods_supported", methods);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getElementFromObject("server", "token_endpoint_auth_methods_supported");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsMTLS#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_supportsPKIOnly() {

		JsonArray methods = new JsonArray();
		methods.add("tls_client_auth");

		env.getObject("server").add("token_endpoint_auth_methods_supported", methods);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsMTLS#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_supportsPublicKeyOnly() {

		JsonArray methods = new JsonArray();
		methods.add("pub_key_tls_client_auth");

		env.getObject("server").add("token_endpoint_auth_methods_supported", methods);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsMTLS#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_supportsBasicOnly() {

		JsonArray methods = new JsonArray();
		methods.add("client_secret_basic");

		env.getObject("server").add("token_endpoint_auth_methods_supported", methods);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsMTLS#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_defaultOnly() {

		cond.evaluate(env);
	}

}
