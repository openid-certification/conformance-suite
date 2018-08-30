package io.fintechlabs.testframework.condition.as;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.assertEquals;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ExtractFapiDateHeader_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ExtractFapiDateHeader cond;

	private String date = "Tue, 11 Sep 2012 19:43:31 GMT"; // example from FAPI spec
	private String badDate = "Xen, 48 Boo 20XX 49-83-20 YYZ";

	private JsonObject goodRequest;
	private JsonObject badRequest;
	private JsonObject missingHeader;
	private JsonObject noHeaders;

	@Before
	public void setUp() throws Exception {

		cond = new ExtractFapiDateHeader("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodRequest = new JsonParser().parse("{\"headers\":{"
			+ "\"x-fapi-auth-date\": \"" + date + "\""
			+ "}}").getAsJsonObject();
		badRequest = new JsonParser().parse("{\"headers\":{"
			+ "\"x-fapi-auth-date\": \"" + badDate + "\""
			+ "}}").getAsJsonObject();
		missingHeader = new JsonParser().parse("{\"headers\":{}}").getAsJsonObject();
		noHeaders = new JsonParser().parse("{}").getAsJsonObject();

	}

	@Test
	public void test_good() {

		env.putObject("incoming_request", goodRequest);
		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("incoming_request", "headers.x-fapi-auth-date");
		assertEquals(date, env.getString("fapi_auth_date"));
	}

	@Test(expected = ConditionError.class)
	public void test_bad() {
		env.putObject("incoming_request", badRequest);
		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void test_missing() {
		env.putObject("incoming_request", missingHeader);
		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void test_noHeader() {
		env.putObject("incoming_request", noHeaders);
		cond.evaluate(env);
	}

}
