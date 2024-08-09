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
public class ValidateAuthenticationRequestIdExpiresIn_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateAuthenticationRequestIdExpiresIn cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateAuthenticationRequestIdExpiresIn();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_isGood() {
		JsonObject o = new JsonObject();
		o.addProperty("expires_in", 600);
		env.putObject("backchannel_authentication_endpoint_response", o);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_isNotNumber() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			o.addProperty("expires_in", "600");
			env.putObject("backchannel_authentication_endpoint_response", o);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_lessthanZero() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			o.addProperty("expires_in", -2);
			env.putObject("backchannel_authentication_endpoint_response", o);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_morethanOneYear() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			o.addProperty("expires_in", 60000000);
			env.putObject("backchannel_authentication_endpoint_response", o);

			cond.execute(env);
		});
	}
}
