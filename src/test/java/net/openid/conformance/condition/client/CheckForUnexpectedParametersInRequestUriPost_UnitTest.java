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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckForUnexpectedParametersInRequestUriPost_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckForUnexpectedParametersInRequestUriPost cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckForUnexpectedParametersInRequestUriPost();
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
	}

	@Test
	public void testEvaluate_walletNonceOnly() {
		putFormParams("{\"wallet_nonce\": \"abc\"}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_walletMetadataOnly() {
		putFormParams("{\"wallet_metadata\": \"{}\"}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_bothExpectedParameters() {
		putFormParams("{\"wallet_nonce\": \"abc\", \"wallet_metadata\": \"{}\"}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_unknownParameter() {
		putFormParams("{\"wallet_nonce\": \"abc\", \"surprise\": \"value\"}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
