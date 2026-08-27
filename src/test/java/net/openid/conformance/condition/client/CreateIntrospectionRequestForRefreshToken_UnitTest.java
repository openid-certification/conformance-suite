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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class CreateIntrospectionRequestForRefreshToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreateIntrospectionRequestForRefreshToken cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CreateIntrospectionRequestForRefreshToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate() {
		env.putString("refresh_token", "example-refresh-token");

		cond.execute(env);

		JsonObject form = env.getObject("introspection_endpoint_request_form_parameters");
		assertNotNull(form);
		assertEquals("example-refresh-token", OIDFJSON.getString(form.get("token")));
		assertEquals("refresh_token", OIDFJSON.getString(form.get("token_type_hint")));
	}

}
