package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ExtractVP1FinalVpTokenDCQL_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExtractVP1FinalVpTokenDCQL cond;


	@BeforeEach
	public void setUp() throws Exception {

		cond = new ExtractVP1FinalVpTokenDCQL();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_valuePresent() {
		String json = """
			{
			  "my_credential": [ "eyJhbGci...QMA" ]
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

	/**
	 * A wallet that returns the credential as a bare string instead of wrapping it in a JSON
	 * array is non-conformant, but the condition must still expose the extracted credential so
	 * the caller can keep validating it against the DCQL query.
	 */
	@Test
	public void testEvaluate_credentialIsBareString() {
		String json = """
			{
			  "my_credential": "eyJhbGci...QMA"
			}
			""";
		env.putObjectFromJsonString("authorization_endpoint_response", "vp_token", json);

		assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("my_credential", env.getString("credential_id"));
		assertEquals("eyJhbGci...QMA", env.getString("credential"));
	}

	@Test
	public void testEvaluate_credentialIsEmptyArray() {
		String json = """
			{
			  "my_credential": []
			}
			""";
		env.putObjectFromJsonString("authorization_endpoint_response", "vp_token", json);

		assertThrows(ConditionError.class, () -> cond.execute(env));

		assertNull(env.getString("credential"));
	}

	@Test
	public void testEvaluate_credentialIsWrongType() {
		String json = """
			{
			  "my_credential": 42
			}
			""";
		env.putObjectFromJsonString("authorization_endpoint_response", "vp_token", json);

		assertThrows(ConditionError.class, () -> cond.execute(env));

		assertNull(env.getString("credential"));
	}

}
