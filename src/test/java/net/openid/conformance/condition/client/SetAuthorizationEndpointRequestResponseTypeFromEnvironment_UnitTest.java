package net.openid.conformance.condition.client;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class SetAuthorizationEndpointRequestResponseTypeFromEnvironment_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private SetAuthorizationEndpointRequestResponseTypeFromEnvironment cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new SetAuthorizationEndpointRequestResponseTypeFromEnvironment();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_code() {

		env.putObject("authorization_endpoint_request", new JsonObject());
		env.putString("response_type", "code");

		cond.execute(env);

		assertThat(env.getString("authorization_endpoint_request", "response_type")).isEqualTo("code");
	}

	@Test
	public void testEvaluate_idToken() {

		env.putObject("authorization_endpoint_request", new JsonObject());
		env.putString("response_type", "id_token");

		cond.execute(env);

		assertThat(env.getString("authorization_endpoint_request", "response_type")).isEqualTo("id_token");
	}

	@Test
	public void testEvaluate_missing() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("authorization_endpoint_request", new JsonObject());

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_empty() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("authorization_endpoint_request", new JsonObject());
			env.putString("response_type", "");

			cond.execute(env);
		});
	}
}
