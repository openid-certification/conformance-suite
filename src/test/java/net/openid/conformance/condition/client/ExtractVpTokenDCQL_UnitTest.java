package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ExtractVpTokenDCQL_UnitTest {

	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ExtractVpTokenDCQL cond;


	@BeforeEach
	public void setUp() throws Exception {

		cond = new ExtractVpTokenDCQL();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_valuePresent() {
		String json = """
			{
			  "my_credential": "eyJhbGci...QMA"
			}
			""";
		env.putObjectFromJsonString("authorization_endpoint_response", "vp_token", json);

		cond.execute(env);

		assertEquals("my_credential", env.getString("credential_id"));
		assertEquals("eyJhbGci...QMA", env.getString("credential"));

	}

	@Test
	public void testEvaluate_twoCreds() {
		assertThrows(ConditionError.class, () -> {
			String json = """
			{
			  "my_credential": "eyJhbGci...QMA",
			  "my_credential2": "eyJhbGci...QMA"
			}
			""";
			env.putObjectFromJsonString("authorization_endpoint_response", "vp_token", json);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_noCreds() {
		assertThrows(ConditionError.class, () -> {
			String json = """
			{
			}
			""";
			env.putObjectFromJsonString("authorization_endpoint_response", "vp_token", json);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_notAnObject() {
		assertThrows(ConditionError.class, () -> {

			env.putString("authorization_endpoint_response", "vp_token", "credential");

			cond.execute(env);
		});
	}

}
