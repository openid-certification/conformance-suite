package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VCIDetermineCredentialIssuerMetadataResponseFormat_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIDetermineCredentialIssuerMetadataResponseFormat cond;

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIDetermineCredentialIssuerMetadataResponseFormat();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env = new Environment();
	}

	private String run(String accept) {
		JsonObject headers = new JsonObject();
		if (accept != null) {
			headers.addProperty("accept", accept);
		}
		JsonObject request = new JsonObject();
		request.add("headers", headers);
		env.putObject("incoming_request", request);
		cond.execute(env);
		return env.getString(VCIDetermineCredentialIssuerMetadataResponseFormat.ENV_KEY);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"application/jwt",
		"APPLICATION/JWT",
		"application/json, application/jwt",
		"application/jwt;q=0.5, application/json",
		// semicolon-separated types, as sent by the EUDI reference wallet: the first type still counts
		"application/jwt; application/json",
	})
	public void testEvaluate_signed(String accept) {
		assertEquals("application/jwt", run(accept));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"application/json",
		"*/*",
		"application/*",
		"application/jwt;q=0, application/json",
		"application/json; application/jwt",
	})
	public void testEvaluate_unsigned(String accept) {
		assertEquals("application/json", run(accept));
	}

	@Test
	public void testEvaluate_absentAcceptHeader() {
		assertEquals("application/json", run(null));
	}
}
