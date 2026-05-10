package net.openid.conformance.condition.common;

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
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class RARSupport_CheckForAuthorizationDetailsInTokenResponse_UnitTest {

	private RARSupport.CheckForAuthorizationDetailsInTokenResponse cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new RARSupport.CheckForAuthorizationDetailsInTokenResponse();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private void givenAuthorizationDetails(String authorizationDetailsJson) {
		JsonObject resp = new JsonObject();
		resp.add("authorization_details", JsonParser.parseString(authorizationDetailsJson));
		env.putObject("token_endpoint_response", resp);
	}

	@Test
	public void testEvaluate_validEntries_succeeds() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\"x\"}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_primitiveEntry_failsCleanly() {
		// Regression: previously called element.getAsJsonObject() in the args of the
		// thrown error, which threw IllegalStateException for non-object entries and
		// escaped as a generic framework exception instead of a conformance failure.
		givenAuthorizationDetails("[\"string_entry\"]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_objectEntryMissingType_failsCleanly() {
		givenAuthorizationDetails("[{\"credential_configuration_id\":\"x\"}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
