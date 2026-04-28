package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureWalletNonceClaimMatchesPostedValue_UnitTest {

	private static final String NONCE = "abc123";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureWalletNonceClaimMatchesPostedValue cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureWalletNonceClaimMatchesPostedValue();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putString("wallet_nonce", NONCE);
	}

	private void putRequestObjectWithClaims(String claimsJson) {
		JsonObject reqObj = new JsonObject();
		reqObj.add("claims", JsonParser.parseString(claimsJson).getAsJsonObject());
		env.putObject("authorization_request_object", reqObj);
	}

	@Test
	public void testEvaluate_matches() {
		putRequestObjectWithClaims("{\"wallet_nonce\": \"" + NONCE + "\"}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingClaim() {
		putRequestObjectWithClaims("{\"client_id\": \"x\"}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_mismatchedValue() {
		putRequestObjectWithClaims("{\"wallet_nonce\": \"different-value\"}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_claimNotAString() {
		putRequestObjectWithClaims("{\"wallet_nonce\": 12345}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
