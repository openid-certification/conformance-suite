package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
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
public class CheckNoRedirectUriInVpAuthorizationRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckNoRedirectUriInVpAuthorizationRequest cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckNoRedirectUriInVpAuthorizationRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noRedirectUri() {
		JsonObject params = new JsonObject();
		params.addProperty("response_type", "vp_token");
		params.addProperty("response_uri", "https://example.com/response");
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, params);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_hasRedirectUri() {
		assertThrows(ConditionError.class, () -> {
			JsonObject params = new JsonObject();
			params.addProperty("response_type", "vp_token");
			params.addProperty("redirect_uri", "https://example.com/callback");
			env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, params);

			cond.execute(env);
		});
	}
}
