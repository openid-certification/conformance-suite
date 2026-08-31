package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static net.openid.conformance.condition.client.ExtractGrantedScopeFromTokenEndpointResponse.GRANTED_SCOPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ExtractGrantedScopeFromTokenEndpointResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private <T extends AbstractCondition> T cond(T condition) {
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		return condition;
	}

	private static JsonObject json(String s) {
		return JsonParser.parseString(s).getAsJsonObject();
	}

	@Test
	public void recordsTheScopeTheServerGrantedUnderTheClientObject() {
		env.putObject("client", json("{\"client_id\":\"client1\"}"));
		env.putObject("token_endpoint_response", json("{\"scope\":\"openid accounts\"}"));

		cond(new ExtractGrantedScopeFromTokenEndpointResponse()).execute(env);

		assertEquals("openid accounts", env.getString("client", GRANTED_SCOPE));
	}

	@Test
	public void clearsAnyPreviouslyRecordedScopeWhenTheResponseHasNone() {
		// otherwise a stale value would be used for a later request by the same client
		env.putObject("client", json("{\"client_id\":\"client1\",\"" + GRANTED_SCOPE + "\":\"openid accounts\"}"));
		env.putObject("token_endpoint_response", json("{\"access_token\":\"abc\"}"));

		cond(new ExtractGrantedScopeFromTokenEndpointResponse()).execute(env);

		assertNull(env.getString("client", GRANTED_SCOPE));
	}

	@Test
	public void isScopedToTheClientItWasRecordedFor() {
		// the multiple-client tests map the client object per client, so client2 does not see client1's scope
		env.putObject("client", json("{\"client_id\":\"client1\"}"));
		env.putObject("token_endpoint_response", json("{\"scope\":\"openid accounts\"}"));
		cond(new ExtractGrantedScopeFromTokenEndpointResponse()).execute(env);

		env.putObject("client", json("{\"client_id\":\"client2\",\"scope\":\"openid payments\"}"));
		env.putObject("token_endpoint_request_form_parameters", json("{}"));

		cond(new AddScopeToTokenEndpointRequest()).execute(env);

		assertEquals("openid payments",
			env.getString("token_endpoint_request_form_parameters", "scope"));
	}

	@Test
	public void addScopeToTokenEndpointRequest_prefersTheGrantedScope() {
		env.putObject("token_endpoint_request_form_parameters", json("{}"));
		env.putObject("client", json("{\"scope\":\"openid accounts payments\",\"" + GRANTED_SCOPE + "\":\"openid accounts\"}"));

		cond(new AddScopeToTokenEndpointRequest()).execute(env);

		assertEquals("openid accounts",
			env.getString("token_endpoint_request_form_parameters", "scope"));
	}

	@Test
	public void addScopeToTokenEndpointRequest_fallsBackToTheConfiguredScope() {
		env.putObject("token_endpoint_request_form_parameters", json("{}"));
		env.putObject("client", json("{\"scope\":\"openid accounts payments\"}"));

		cond(new AddScopeToTokenEndpointRequest()).execute(env);

		assertEquals("openid accounts payments",
			env.getString("token_endpoint_request_form_parameters", "scope"));
	}

	@Test
	public void addScopeToTokenEndpointRequest_failsWhenNeitherScopeIsAvailable() {
		env.putObject("token_endpoint_request_form_parameters", json("{}"));
		env.putObject("client", json("{}"));

		assertThrows(ConditionError.class,
			() -> cond(new AddScopeToTokenEndpointRequest()).execute(env));
	}
}
