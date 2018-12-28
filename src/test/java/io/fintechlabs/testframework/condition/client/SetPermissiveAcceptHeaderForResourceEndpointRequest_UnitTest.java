package io.fintechlabs.testframework.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class SetPermissiveAcceptHeaderForResourceEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private SetPermissiveAcceptHeaderForResourceEndpointRequest cond;

	private final String expectedHeader = "application/json, application/*+json, */*";

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new SetPermissiveAcceptHeaderForResourceEndpointRequest("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.SetPermissiveAcceptHeaderForResourceEndpointRequest#evaluate(io.fintechlabs.testframework.testmodule.Environment).
	 */
	@Test
	public void testEvaluate_noHeaders() {

		cond.evaluate(env);

		assertThat(env.getString("resource_endpoint_request_headers", "Accept")).isEqualTo(expectedHeader);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.SetPermissiveAcceptHeaderForResourceEndpointRequest#evaluate(io.fintechlabs.testframework.testmodule.Environment).
	 */
	@Test
	public void testEvaluate_replace() {

		JsonObject headers = new JsonObject();
		headers.addProperty("Accept", "something else");
		env.putObject("resource_endpoint_request_headers", headers);

		cond.evaluate(env);

		assertThat(env.getString("resource_endpoint_request_headers", "Accept")).isEqualTo(expectedHeader);
	}

}
