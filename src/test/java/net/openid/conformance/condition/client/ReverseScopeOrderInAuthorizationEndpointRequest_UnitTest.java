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
public class ReverseScopeOrderInAuthorizationEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ReverseScopeOrderInAuthorizationEndpointRequest cond;

	private String requestId;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ReverseScopeOrderInAuthorizationEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate() {
		JsonObject authRequest = new JsonObject();
		authRequest.addProperty("scope", "openid accounts");
		env.putObject("authorization_endpoint_request", authRequest);

		cond.execute(env);

		assertThat(env.getString("authorization_endpoint_request", "scope")).isEqualTo("accounts openid");
	}

	@Test
	public void testEvaluateNoScope() {
		assertThrows(ConditionError.class, () -> {
			JsonObject authRequest = new JsonObject();
			env.putObject("authorization_endpoint_request", authRequest);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluateScopeEmpty() {
		assertThrows(ConditionError.class, () -> {
			JsonObject authRequest = new JsonObject();
			authRequest.addProperty("scope", "");
			env.putObject("authorization_endpoint_request", authRequest);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluateOnlyOneScope() {
		assertThrows(ConditionError.class, () -> {
			JsonObject authRequest = new JsonObject();
			authRequest.addProperty("scope", "openid");
			env.putObject("authorization_endpoint_request", authRequest);

			cond.execute(env);
		});
	}

}
