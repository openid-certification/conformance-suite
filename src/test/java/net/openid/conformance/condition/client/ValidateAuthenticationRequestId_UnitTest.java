package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
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
public class ValidateAuthenticationRequestId_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateAuthenticationRequestId cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateAuthenticationRequestId();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_isEmpty() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			env.putObject("backchannel_authentication_endpoint_response", o);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_isBlank() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			o.addProperty("auth_req_id", "");
			env.putObject("backchannel_authentication_endpoint_response", o);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_isGood() {
		JsonObject o = new JsonObject();
		o.addProperty("auth_req_id", "NlPTzeKKU8CNJompx_lj77ABC5cEXPPV3BernTiVU10");
		env.putObject("backchannel_authentication_endpoint_response", o);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_isBad() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			o.addProperty("auth_req_id", "~~f-vRyWiNZbuHssshH8WY");
			env.putObject("backchannel_authentication_endpoint_response", o);

			cond.execute(env);
		});
	}

}
