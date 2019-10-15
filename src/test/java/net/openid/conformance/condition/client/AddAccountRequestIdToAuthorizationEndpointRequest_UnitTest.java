package net.openid.conformance.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class AddAccountRequestIdToAuthorizationEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddAccountRequestIdToAuthorizationEndpointRequest cond;

	private String requestId;

	@Before
	public void setUp() throws Exception {
		cond = new AddAccountRequestIdToAuthorizationEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		requestId = "88379"; // OpenBanking example
	}

	@Test
	public void testEvaluate() {

		env.putObject("authorization_endpoint_request", new JsonObject());
		env.putString("account_request_id", requestId);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("account_request_id");

		assertThat(env.getString("authorization_endpoint_request", "claims.id_token.openbanking_intent_id.value")).isEqualTo(requestId);
		assertThat(env.getBoolean("authorization_endpoint_request", "claims.id_token.openbanking_intent_id.essential")).isTrue();

	}

}
