package net.openid.conformance.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CreateTokenEndpointRequestForAuthorizationCodeGrant_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateTokenEndpointRequestForAuthorizationCodeGrant cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		cond = new CreateTokenEndpointRequestForAuthorizationCodeGrant();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link CreateTokenEndpointRequestForAuthorizationCodeGrant#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate() {

		env.putString("code", "SplxlOBeZQQYbYS6WxSbIA");
		env.putString("redirect_uri", "https://client.example.com/cb");

		cond.execute(env);

		verify(env, atLeastOnce()).getString("code");
		verify(env, atLeastOnce()).getString("redirect_uri");

		assertThat(env.getObject("token_endpoint_request_form_parameters")).isNotNull();
		assertThat(env.getString("token_endpoint_request_form_parameters", "grant_type")).isEqualTo("authorization_code");
		assertThat(env.getString("token_endpoint_request_form_parameters", "code")).isEqualTo("SplxlOBeZQQYbYS6WxSbIA");
		assertThat(env.getString("token_endpoint_request_form_parameters", "redirect_uri")).isEqualTo("https://client.example.com/cb");
	}
}
