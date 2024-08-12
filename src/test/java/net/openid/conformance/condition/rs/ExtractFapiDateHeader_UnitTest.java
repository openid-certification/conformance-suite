package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
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

	@BeforeEach
	public void setUp() throws Exception {

		cond = new ExtractFapiDateHeader();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodRequest = JsonParser.parseString("{\"headers\":{"
			+ "\"x-fapi-auth-date\": \"" + date + "\""
			+ "}}").getAsJsonObject();
		badRequest = JsonParser.parseString("{\"headers\":{"
			+ "\"x-fapi-auth-date\": \"" + badDate + "\""
			+ "}}").getAsJsonObject();
		oddRequest = JsonParser.parseString("{\"headers\":{"
			+ "\"x-fapi-auth-date\": \"" + oddDate + "\""
			+ "}}").getAsJsonObject();
		oldRequest = JsonParser.parseString("{\"headers\":{"
			+ "\"x-fapi-auth-date\": \"" + oldFormat + "\""
			+ "}}").getAsJsonObject();
		asciiRequest = JsonParser.parseString("{\"headers\":{"
			+ "\"x-fapi-auth-date\": \"" + asciiFormat + "\""
			+ "}}").getAsJsonObject();
		badFormatRequest = JsonParser.parseString("{\"headers\":{"
			+ "\"x-fapi-auth-date\": \"" + badFormat + "\""
			+ "}}").getAsJsonObject();
		missingHeader = JsonParser.parseString("{\"headers\":{}}").getAsJsonObject();
		noHeaders = JsonParser.parseString("{}").getAsJsonObject();

	}

	@Test
	public void test_good() {

		env.putObject("incoming_request", goodRequest);
		cond.execute(env);

		verify(env, atLeastOnce()).getString("incoming_request", "headers.x-fapi-auth-date");
		assertEquals(date, env.getString("fapi_auth_date"));
	}

	@Test
	public void test_bad() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("incoming_request", badRequest);
			cond.execute(env);
		});
	}

	@Test
	public void test_odd() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("incoming_request", oddRequest);
			cond.execute(env);
		});
	}

	@Test
	public void test_old() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("incoming_request", oldRequest);
			cond.execute(env);
		});
	}

	@Test
	public void test_ascii() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("incoming_request", asciiRequest);
			cond.execute(env);
		});
	}

	@Test
	public void test_badFormat() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("incoming_request", badFormatRequest);
			cond.execute(env);
		});
	}

	@Test
	public void test_missing() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("incoming_request", missingHeader);
			cond.execute(env);
		});
	}

	@Test
	public void test_noHeader() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("incoming_request", noHeaders);
			cond.execute(env);
		});
	}

}
