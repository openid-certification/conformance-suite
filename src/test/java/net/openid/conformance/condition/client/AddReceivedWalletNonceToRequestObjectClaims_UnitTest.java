package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class AddReceivedWalletNonceToRequestObjectClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AddReceivedWalletNonceToRequestObjectClaims cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AddReceivedWalletNonceToRequestObjectClaims();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_addsClaim() {
		JsonObject claims = JsonParser.parseString("{\"client_id\": \"https://verifier.example.com\"}").getAsJsonObject();
		env.putObject("request_object_claims", claims);
		env.putString("received_wallet_nonce", "abc123");

		cond.execute(env);

		JsonObject updated = env.getObject("request_object_claims");
		assertEquals("abc123", OIDFJSON.getString(updated.get("wallet_nonce")));
	}

	@Test
	public void testEvaluate_overwritesExistingValue() {
		JsonObject claims = JsonParser.parseString("{\"wallet_nonce\": \"old\"}").getAsJsonObject();
		env.putObject("request_object_claims", claims);
		env.putString("received_wallet_nonce", "new");

		cond.execute(env);

		JsonObject updated = env.getObject("request_object_claims");
		assertEquals("new", OIDFJSON.getString(updated.get("wallet_nonce")));
	}
}
