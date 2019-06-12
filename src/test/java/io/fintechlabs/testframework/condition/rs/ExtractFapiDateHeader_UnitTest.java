package io.fintechlabs.testframework.condition.rs;

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

import static org.junit.Assert.assertEquals;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.rs.ExtractFapiDateHeader;
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
	private String badDate = "Tue, 31 Sep 2012 19:43:31 GMT";
	private String oddDate = "Xen, 48 Boo 20XX 49-83-20 YYZ";
	private String oldFormat = "Sunday, 06-Nov-94 08:49:37 GMT";
	private String asciiFormat = "Sun Nov  6 08:49:37 1994";
	private String badFormat = "Xenubar, 438   23Boo 20XX1 211 49-83-20 YYZ zsdff.bob";

	private JsonObject goodRequest;
	private JsonObject badRequest;
	private JsonObject oddRequest;
	private JsonObject oldRequest;
	private JsonObject asciiRequest;
	private JsonObject badFormatRequest;
	private JsonObject missingHeader;
	private JsonObject noHeaders;

	@Before
	public void setUp() throws Exception {

		cond = new ExtractFapiDateHeader();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodRequest = new JsonParser().parse("{\"headers\":{"
			+ "\"x-fapi-auth-date\": \"" + date + "\""
			+ "}}").getAsJsonObject();
		badRequest = new JsonParser().parse("{\"headers\":{"
			+ "\"x-fapi-auth-date\": \"" + badDate + "\""
			+ "}}").getAsJsonObject();
		oddRequest = new JsonParser().parse("{\"headers\":{"
			+ "\"x-fapi-auth-date\": \"" + oddDate + "\""
			+ "}}").getAsJsonObject();
		oldRequest = new JsonParser().parse("{\"headers\":{"
			+ "\"x-fapi-auth-date\": \"" + oldFormat + "\""
			+ "}}").getAsJsonObject();
		asciiRequest = new JsonParser().parse("{\"headers\":{"
			+ "\"x-fapi-auth-date\": \"" + asciiFormat + "\""
			+ "}}").getAsJsonObject();
		badFormatRequest = new JsonParser().parse("{\"headers\":{"
			+ "\"x-fapi-auth-date\": \"" + badFormat + "\""
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
	public void test_odd() {
		env.putObject("incoming_request", oddRequest);
		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void test_old() {
		env.putObject("incoming_request", oldRequest);
		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void test_ascii() {
		env.putObject("incoming_request", asciiRequest);
		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void test_badFormat() {
		env.putObject("incoming_request", badFormatRequest);
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
