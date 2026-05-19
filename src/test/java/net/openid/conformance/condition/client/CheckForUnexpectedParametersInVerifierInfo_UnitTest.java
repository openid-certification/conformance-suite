package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.AbstractVciUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class CheckForUnexpectedParametersInVerifierInfo_UnitTest extends AbstractVciUnitTest {

	private CheckForUnexpectedParametersInVerifierInfo cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckForUnexpectedParametersInVerifierInfo();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	public void testEvaluate_noUnknownProperties() {
		putVerifierInfo("""
			[ { "format": "registration_certificate_jwt", "data": "x" } ]
			""");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_unknownPropertyOnEntry() {
		putVerifierInfo("""
			[ { "format": "x", "data": "y", "extension_field": "z" } ]
			""");
		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertUnknownPropertyAtPath(data, "$.verifier_info[0].extension_field");
	}

	private void putVerifierInfo(String json) {
		JsonElement verifierInfo = JsonParser.parseString(json);
		JsonObject wrapper = new JsonObject();
		wrapper.add(ExtractVerifierInfoFromClientConfiguration.WRAPPER_PROPERTY, verifierInfo);
		env.putObject(ExtractVerifierInfoFromClientConfiguration.ENV_KEY, wrapper);
	}
}
