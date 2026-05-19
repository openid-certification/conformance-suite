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
public class CheckForUnexpectedParametersInVpAuthorizationEndpointHttpRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckForUnexpectedParametersInVpAuthorizationEndpointHttpRequest cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckForUnexpectedParametersInVpAuthorizationEndpointHttpRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void putHttpParams(String json) {
		JsonObject params = JsonParser.parseString(json).getAsJsonObject();
		env.putObject("authorization_endpoint_http_request_params", params);
	}

	@Test
	public void testEvaluate_requestUriWithExtraResponseTypeRejected() {
		putHttpParams("{\"client_id\":\"https://verifier.example.com\",\"response_type\":\"vp_token\",\"request_uri\":\"https://verifier.example.com/request\",\"request_uri_method\":\"post\"}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_urlQueryParametersIgnored() {
		putHttpParams("{\"client_id\":\"https://verifier.example.com\",\"response_type\":\"vp_token\",\"response_mode\":\"direct_post\",\"response_uri\":\"https://wallet.example.com/response\",\"dcql_query\":{},\"nonce\":\"abc\",\"state\":\"xyz\"}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_walletNonceRejected() {
		putHttpParams("{\"client_id\":\"https://verifier.example.com\",\"response_type\":\"vp_token\",\"request_uri\":\"https://verifier.example.com/request\",\"wallet_nonce\":\"abc\"}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_requestUriWithOnlyClientIdAndMethodAccepted() {
		putHttpParams("{\"client_id\":\"https://verifier.example.com\",\"request_uri\":\"https://verifier.example.com/request\",\"request_uri_method\":\"post\"}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_requestWithOnlyClientIdAccepted() {
		putHttpParams("{\"client_id\":\"https://verifier.example.com\",\"request\":\"eyJhbGciOiJub25lIn0.eyJjbGllbnRfaWQiOiJodHRwczovL3ZlcmlmaWVyLmV4YW1wbGUuY29tIn0.\"}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_requestWithExtraParameterRejected() {
		putHttpParams("{\"client_id\":\"https://verifier.example.com\",\"request\":\"eyJhbGciOiJub25lIn0.eyJjbGllbnRfaWQiOiJodHRwczovL3ZlcmlmaWVyLmV4YW1wbGUuY29tIn0.\",\"response_type\":\"vp_token\"}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
