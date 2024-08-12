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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureMinimumAuthorizationCodeLength_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureMinimumAuthorizationCodeLength cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureMinimumAuthorizationCodeLength();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_entropyGood() {
		JsonObject o = new JsonObject();
		o.addProperty("code", "BjnQbTsQiUYitDvHY9Ork6twLS2Ps_7ZZGlNfyx2FRo");
		env.putObject("authorization_endpoint_response", o);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_entropyBad() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			o.addProperty("code", "BjnQbTsQiUYi");
			env.putObject("authorization_endpoint_response", o);

			cond.execute(env);
		});
	}

}
