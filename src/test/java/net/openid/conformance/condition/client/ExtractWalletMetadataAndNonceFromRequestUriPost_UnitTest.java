package net.openid.conformance.condition.client;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ExtractWalletMetadataAndNonceFromRequestUriPost_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExtractWalletMetadataAndNonceFromRequestUriPost cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ExtractWalletMetadataAndNonceFromRequestUriPost();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void putFormParams(String json) {
		JsonObject incoming = new JsonObject();
		incoming.add("body_form_params", JsonParser.parseString(json).getAsJsonObject());
		env.putObject("incoming_request", incoming);
	}

	@Test
	public void testEvaluate_emptyFormParams() {
		putFormParams("{}");
		cond.execute(env);
		assertNull(env.getString("received_wallet_nonce"));
		assertNull(env.getObject("received_wallet_metadata"));
	}

	@Test
	public void testEvaluate_noBody() {
		env.putObject("incoming_request", new JsonObject());
		cond.execute(env);
		assertNull(env.getString("received_wallet_nonce"));
		assertNull(env.getObject("received_wallet_metadata"));
	}

	@Test
	public void testEvaluate_bodyNotParsedAsFormParams() {
		JsonObject incoming = new JsonObject();
		incoming.addProperty("body", "{\"wallet_nonce\": \"abc\"}");
		env.putObject("incoming_request", incoming);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_walletNonce() {
		putFormParams("{\"wallet_nonce\": \"abc\"}");
		cond.execute(env);
		assertEquals("abc", env.getString("received_wallet_nonce"));
		assertNull(env.getObject("received_wallet_metadata"));
	}

	@Test
	public void testEvaluate_walletMetadata() {
		putFormParams("{\"wallet_metadata\": \"{\\\"vp_formats_supported\\\": {}}\"}");
		cond.execute(env);
		assertEquals(JsonParser.parseString("{\"vp_formats_supported\": {}}"), env.getObject("received_wallet_metadata"));
		assertNull(env.getString("received_wallet_nonce"));
	}

	@Test
	public void testEvaluate_walletMetadataNotJson() {
		putFormParams("{\"wallet_metadata\": \"{not json\"}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_walletMetadataNotJsonObject() {
		putFormParams("{\"wallet_metadata\": \"[]\"}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
