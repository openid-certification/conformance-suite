package net.openid.conformance.condition.as;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
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

@ExtendWith(MockitoExtension.class)
public class CdrCheckForUnexpectedClaimsInClaimsParameter_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CdrCheckForUnexpectedClaimsInClaimsParameter cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CdrCheckForUnexpectedClaimsInClaimsParameter();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addClaimsParameter(String claimsJson) {
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY,
			JsonParser.parseString("{\"claims\":" + claimsJson + "}").getAsJsonObject());
	}

	@Test
	public void testEvaluate_cdrClaimsAccepted() {
		addClaimsParameter("{\"sharing_duration\":7776000,\"cdr_arrangement_id\":\"arr-1\",\"id_token\":{\"acr\":{\"essential\":true,\"value\":\"urn:cds.au:cdr:2\"}}}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_unknownClaimStillCaught() {
		assertThrows(ConditionError.class, () -> {
			addClaimsParameter("{\"sharing_duraton\":7776000,\"id_token\":{}}");
			cond.execute(env);
		});
	}

}
