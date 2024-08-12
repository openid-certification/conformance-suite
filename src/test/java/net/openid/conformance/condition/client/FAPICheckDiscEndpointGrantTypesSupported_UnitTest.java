package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FAPICheckDiscEndpointGrantTypesSupported_UnitTest {

	@Test
	public void client_credentials_and_refresh_token_condition_passes_when_exactly_those_two_are_present() {
		FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken cond
			= getClientCredentialsAndRefreshTokenCondition();

		JsonArray grantTypesSupported = new JsonArray();
		grantTypesSupported.add("client_credentials");
		grantTypesSupported.add("refresh_token");

		Environment env = mock(Environment.class);
		when(env.getElementFromObject("server", "grant_types_supported")).thenReturn(grantTypesSupported);

		cond.evaluate(env);
	}

	@Test
	public void client_credentials_and_refresh_token_condition_passes_when_both_are_present() {
		FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken cond
			= getClientCredentialsAndRefreshTokenCondition();

		JsonArray grantTypesSupported = new JsonArray();
		grantTypesSupported.add("authorization_code");
		grantTypesSupported.add("client_credentials");
		grantTypesSupported.add("refresh_token");

		Environment env = mock(Environment.class);
		when(env.getElementFromObject("server", "grant_types_supported")).thenReturn(grantTypesSupported);

		cond.evaluate(env);
	}

	@Test
	public void client_credentials_and_refresh_token_condition_does_not_pass_when_client_credentials_is_missing() {
		FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken cond
			= getClientCredentialsAndRefreshTokenCondition();

		JsonArray grantTypesSupported = new JsonArray();
		grantTypesSupported.add("refresh_token");

		Environment env = mock(Environment.class);
		when(env.getElementFromObject("server", "grant_types_supported")).thenReturn(grantTypesSupported);

		assertThrows(ConditionError.class, () -> cond.evaluate(env));
	}

	@Test
	public void client_credentials_and_refresh_token_condition_does_not_pass_when_refresh_token_is_missing() {
		FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken cond
			= getClientCredentialsAndRefreshTokenCondition();

		JsonArray grantTypesSupported = new JsonArray();
		grantTypesSupported.add("authorization_code");
		grantTypesSupported.add("client_credentials");

		Environment env = mock(Environment.class);
		when(env.getElementFromObject("server", "grant_types_supported")).thenReturn(grantTypesSupported);

		assertThrows(ConditionError.class, () -> cond.evaluate(env));
	}

	@Test
	public void authorization_code_condition_passes_when_it_is_present() {
		FAPICheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode cond
			= getAuthorizationCodeCondition();

		JsonArray grantTypesSupported = new JsonArray();
		grantTypesSupported.add("client_credentials");
		grantTypesSupported.add("authorization_code");

		Environment env = mock(Environment.class);
		when(env.getElementFromObject("server", "grant_types_supported")).thenReturn(grantTypesSupported);

		cond.evaluate(env);
	}

	@Test
	public void authorization_code_condition_fails_when_it_is_missing() {
		FAPICheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode cond
			= getAuthorizationCodeCondition();

		JsonArray grantTypesSupported = new JsonArray();
		grantTypesSupported.add("client_credentials");
		grantTypesSupported.add("refresh_token");

		Environment env = mock(Environment.class);
		when(env.getElementFromObject("server", "grant_types_supported")).thenReturn(grantTypesSupported);

		assertThrows(ConditionError.class, () -> cond.evaluate(env));
	}

	@Test
	public void ciba_condition_passes_when_it_is_present() {
		FAPICheckDiscEndpointGrantTypesSupportedContainsCiba cond = getCibaCondition();

		JsonArray grantTypesSupported = new JsonArray();
		grantTypesSupported.add("urn:openid:params:grant-type:ciba");
		grantTypesSupported.add("authorization_code");

		Environment env = mock(Environment.class);
		when(env.getElementFromObject("server", "grant_types_supported")).thenReturn(grantTypesSupported);

		cond.evaluate(env);
	}

	@Test
	public void ciba_condition_fails_when_it_is_missing() {
		FAPICheckDiscEndpointGrantTypesSupportedContainsCiba cond = getCibaCondition();

		JsonArray grantTypesSupported = new JsonArray();
		grantTypesSupported.add("client_credentials");
		grantTypesSupported.add("refresh_token");
		grantTypesSupported.add("authorization_code");

		Environment env = mock(Environment.class);
		when(env.getElementFromObject("server", "grant_types_supported")).thenReturn(grantTypesSupported);

		assertThrows(ConditionError.class, () -> cond.evaluate(env));
	}

	FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken getClientCredentialsAndRefreshTokenCondition() {
		FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken cond
			= new FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken();
		TestInstanceEventLog eventLog = mock(TestInstanceEventLog.class);
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return cond;
	}

	FAPICheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode getAuthorizationCodeCondition() {
		FAPICheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode cond
			= new FAPICheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode();
		TestInstanceEventLog eventLog = mock(TestInstanceEventLog.class);
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return cond;
	}

	FAPICheckDiscEndpointGrantTypesSupportedContainsCiba getCibaCondition() {
		FAPICheckDiscEndpointGrantTypesSupportedContainsCiba cond = new FAPICheckDiscEndpointGrantTypesSupportedContainsCiba();
		TestInstanceEventLog eventLog = mock(TestInstanceEventLog.class);
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return cond;
	}

}
