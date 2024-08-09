package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
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
public class CheckBackchannelAuthenticationEndpointHttpStatus400_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckBackchannelAuthenticationEndpointHttpStatus400 cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckBackchannelAuthenticationEndpointHttpStatus400();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		env.putInteger("backchannel_authentication_endpoint_response_http_status", 400);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_withError400() {
		assertThrows(ConditionError.class, () -> {
			env.putInteger("backchannel_authentication_endpoint_response_http_status", 200);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_wrongParameters() {
		assertThrows(ConditionError.class, () -> {
			env.putInteger("authentication_endpoint_response_http_status", 400);

			cond.execute(env);
		});
	}

}
