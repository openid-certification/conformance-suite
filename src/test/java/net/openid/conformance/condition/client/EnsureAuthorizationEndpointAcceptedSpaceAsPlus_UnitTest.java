package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class EnsureAuthorizationEndpointAcceptedSpaceAsPlus_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private Environment env;
	private EnsureAuthorizationEndpointAcceptedSpaceAsPlus cond;

	@BeforeEach
	public void setUp() {
		env = new Environment();
		cond = new EnsureAuthorizationEndpointAcceptedSpaceAsPlus();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.WARNING);
	}

	@Test
	public void successResponsePasses() {
		JsonObject response = new JsonObject();
		response.addProperty("code", "SplxlOBeZQQYbYS6WxSbIA");
		env.putObject("authorization_endpoint_response", response);
		cond.execute(env);
	}

	@Test
	public void errorResponseFails() {
		JsonObject response = new JsonObject();
		response.addProperty("error", "invalid_scope");
		env.putObject("authorization_endpoint_response", response);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
