package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class CheckDiscEndpointAllEndpointsAreHttps_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckDiscEndpointAllEndpointsAreHttps cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckDiscEndpointAllEndpointsAreHttps();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		JsonObject server = JsonParser.parseString("{"
			+ "\"flibble_endpoint\": \"https://www.example.com/endpoint\""
			+ "}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noErrorOtherTypes () {
		JsonObject server = JsonParser.parseString("{"
			+ "\"flibble_endpoint\": \"https://www.example.com/endpoint\","
			+ "\"flibble\" : true,"
			+ "\"flibble2\" : 0.9,"
			+ "\"flibble3\" : { \"a\": \"b\"},"
			+ "\"flibble4\" : [ \"a\"]"
			+ "}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_notHttps() {
		assertThrows(ConditionError.class, () -> {
			JsonObject server = JsonParser.parseString("{"
				+ "\"flibble_endpoint\": \"http://www.example.com/endpoint\""
				+ "}")
				.getAsJsonObject();
			env.putObject("server", server);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_secondEndpointNotHttps() {
		// Regression: every *_endpoint must be validated, not just the first one.
		assertThrows(ConditionError.class, () -> {
			JsonObject server = JsonParser.parseString("{"
				+ "\"aaa_endpoint\": \"https://www.example.com/endpoint\","
				+ "\"zzz_endpoint\": \"http://www.example.com/endpoint\""
				+ "}")
				.getAsJsonObject();
			env.putObject("server", server);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_uppercaseHttpsAccepted() {
		JsonObject server = JsonParser.parseString("{"
			+ "\"flibble_endpoint\": \"HTTPS://www.example.com/endpoint\""
			+ "}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_nullEndpointRejected() {
		// An optional endpoint explicitly set to null is non-conformant (it should be omitted or a
		// valid URL string), so it must be flagged rather than skipped, with a message that names
		// the offending field.
		JsonObject server = JsonParser.parseString("{"
			+ "\"token_endpoint\": \"https://www.example.com/token\","
			+ "\"end_session_endpoint\": null"
			+ "}")
			.getAsJsonObject();
		env.putObject("server", server);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("end_session_endpoint"),
			"failure message should name the offending field, was: " + e.getMessage());
	}

	@Test
	public void testEvaluate_notUrl() {
		assertThrows(ConditionError.class, () -> {
			JsonObject server = JsonParser.parseString("{"
				+ "\"flibble_endpoint\": \"flibble\""
				+ "}")
				.getAsJsonObject();
			env.putObject("server", server);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_errorArray() {
		assertThrows(ConditionError.class, () -> {
			JsonObject server = JsonParser.parseString("{"
				+ "\"flibble_endpoint\": ["
				+ "\"https://www.example.com/endpoint\""
				+ "]}")
				.getAsJsonObject();
			env.putObject("server", server);
			cond.execute(env);
		});
	}

}
