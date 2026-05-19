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
public class EnsureNoWalletNonceInRequestObject_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureNoWalletNonceInRequestObject cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureNoWalletNonceInRequestObject();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void putRequestObjectWithClaims(String claimsJson) {
		JsonObject reqObj = new JsonObject();
		reqObj.add("claims", JsonParser.parseString(claimsJson).getAsJsonObject());
		env.putObject("authorization_request_object", reqObj);
	}

	@Test
	public void testEvaluate_absent() {
		putRequestObjectWithClaims("{\"client_id\": \"x\"}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_emptyClaims() {
		putRequestObjectWithClaims("{}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_present() {
		putRequestObjectWithClaims("{\"wallet_nonce\": \"abc\"}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
