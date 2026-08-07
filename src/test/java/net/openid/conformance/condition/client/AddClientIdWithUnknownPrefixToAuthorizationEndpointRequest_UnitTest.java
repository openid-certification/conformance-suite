package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class AddClientIdWithUnknownPrefixToAuthorizationEndpointRequest_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AddClientIdWithUnknownPrefixToAuthorizationEndpointRequest cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AddClientIdWithUnknownPrefixToAuthorizationEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_addsUnknownPrefixClientId() {
		env.putObject("authorization_endpoint_request", new JsonObject());

		cond.execute(env);

		String clientId = OIDFJSON.getString(env.getObject("authorization_endpoint_request").get("client_id"));
		assertEquals("invalid_scheme:example", clientId);
	}
}
