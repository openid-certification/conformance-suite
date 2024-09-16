package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckTokenEndpointRetryAfterHeaders_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckTokenEndpointRetryAfterHeaders cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckTokenEndpointRetryAfterHeaders();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_isEmpty() {
		assertThrows(ConditionError.class, () -> {

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_HasRetryAfter() {
		JsonObject o = new JsonObject();
		o.addProperty(HttpHeaders.RETRY_AFTER, 300);
		env.putObject("token_endpoint_response_headers", o);

		cond.execute(env);
	}

}
