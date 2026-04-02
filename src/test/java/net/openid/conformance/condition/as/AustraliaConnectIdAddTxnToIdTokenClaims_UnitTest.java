package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class AustraliaConnectIdAddTxnToIdTokenClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AustraliaConnectIdAddTxnToIdTokenClaims cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AustraliaConnectIdAddTxnToIdTokenClaims();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putObject("id_token_claims", new JsonObject());
	}

	@Test
	public void testEvaluate_cibaFlow() {
		env.putObject("backchannel_request_object", new JsonObject());
		env.putString("auth_req_id", "test-auth-req-id");

		cond.execute(env);

		assertEquals("test-auth-req-id", env.getString("id_token_claims", "txn"));
	}

	@Test
	public void testEvaluate_defaultFlow() {
		cond.execute(env);

		String txn = env.getString("id_token_claims", "txn");
		assertTrue(txn != null && !txn.isEmpty());
		// OIDCCLoadUserInfo generates a UUID-like txn by default
	}

	@Test
	public void testEvaluate_alreadyPresent() {
		JsonObject claims = env.getObject("id_token_claims");
		claims.addProperty("txn", "existing-txn");

		cond.execute(env);

		assertEquals("existing-txn", env.getString("id_token_claims", "txn"));
	}
}
