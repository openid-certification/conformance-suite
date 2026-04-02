package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckNoPresentationDefinitionInVpAuthorizationRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckNoPresentationDefinitionInVpAuthorizationRequest cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckNoPresentationDefinitionInVpAuthorizationRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noPresentationDefinition() {
		JsonObject params = new JsonObject();
		params.addProperty("response_type", "vp_token");
		params.addProperty("nonce", "abc123");
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, params);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_hasPresentationDefinition() {
		assertThrows(ConditionError.class, () -> {
			JsonObject params = new JsonObject();
			params.addProperty("response_type", "vp_token");
			params.add("presentation_definition", new JsonObject());
			env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, params);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_hasPresentationDefinitionUri() {
		assertThrows(ConditionError.class, () -> {
			JsonObject params = new JsonObject();
			params.addProperty("response_type", "vp_token");
			params.addProperty("presentation_definition_uri", "https://example.com/pd");
			env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, params);

			cond.execute(env);
		});
	}
}
