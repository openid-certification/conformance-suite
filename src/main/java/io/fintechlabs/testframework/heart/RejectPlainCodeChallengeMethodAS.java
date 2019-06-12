package io.fintechlabs.testframework.heart;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddCodeChallengeToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CheckHeartServerJwksFields;
import io.fintechlabs.testframework.condition.client.CheckRedirectUri;
import io.fintechlabs.testframework.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import io.fintechlabs.testframework.condition.client.CreatePlainCodeChallenge;
import io.fintechlabs.testframework.condition.client.CreateRandomCodeVerifier;
import io.fintechlabs.testframework.condition.client.CreateRandomNonceValue;
import io.fintechlabs.testframework.condition.client.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.client.CreateRedirectUri;
import io.fintechlabs.testframework.condition.client.EnsureAuthorizationEndpointError;
import io.fintechlabs.testframework.condition.client.ExpectRejectPlainCodeChallengeMethodErrorPage;
import io.fintechlabs.testframework.condition.client.FetchServerKeys;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.RejectAuthCodeInUrlQuery;
import io.fintechlabs.testframework.condition.client.RejectErrorInUrlQuery;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import io.fintechlabs.testframework.condition.common.CheckForKeyIdInServerJWKs;
import io.fintechlabs.testframework.condition.common.CheckHeartServerConfiguration;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;
import io.fintechlabs.testframework.condition.common.SetTLSTestHostFromConfig;
import io.fintechlabs.testframework.fapi.AbstractRedirectServerTestModule;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

/**
 * Using {@link NativeDelegatedClientAS} as a base, but with a 'plain' code challenge which should be rejected.
 */
@PublishTestModule(
	testName = "heart-reject-plain-code-challenge",
	displayName = "HEART AS: Reject plain code challenge method",
	profile = "HEART",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"tls.testHost",
		"tls.testPort"
	}
)
public class RejectPlainCodeChallengeMethodAS extends AbstractRedirectServerTestModule {

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#configure(com.google.gson.JsonObject)
	 */
	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		callAndStopOnFailure(SetTLSTestHostFromConfig.class);
		callAndStopOnFailure(EnsureTLS12.class, "HEART-OAuth2-6");
		callAndContinueOnFailure(DisallowTLS10.class, "HEART-OAuth2-6");
		callAndContinueOnFailure(DisallowTLS11.class, "HEART-OAuth2-6");

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Get the server's configuration
		callAndContinueOnFailure(GetDynamicServerConfiguration.class, "HEART-OAuth2-3.1.5");

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckHeartServerConfiguration.class, "HEART-OAuth2-3.1.5");

		// fetch or load the server's keys as needed
		callAndStopOnFailure(FetchServerKeys.class, "HEART-OAuth2-3.1.5");
		callAndStopOnFailure(CheckHeartServerJwksFields.class, "HEART-OAuth2-3.1.5");
		callAndStopOnFailure(CheckForKeyIdInServerJWKs.class, "OIDCC-10.1");

		// Set up the client configuration
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		callAndStopOnFailure(CheckRedirectUri.class);

		exposeEnvString("client_id");

		// Set up the resource endpoint configuration
		//callAndStopOnFailure(GetResourceEndpointConfiguration.class);

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#start()
	 */
	@Override
	public void start() {

		setStatus(Status.RUNNING);

		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);

		// set up PKCE
		callAndStopOnFailure(CreateRandomCodeVerifier.class, "HEART-OAuth2-2.1.2");
		callAndStopOnFailure(CreatePlainCodeChallenge.class);
		callAndStopOnFailure(AddCodeChallengeToAuthorizationEndpointRequest.class, "HEART-OAuth2-2.1.2");

		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class);

		performRedirectAndWaitForErrorCallback();
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRejectPlainCodeChallengeMethodErrorPage.class);

		env.putString("error_callback_placeholder", env.getString("plain_pkce_error"));
	}

	@Override
	protected void processCallback() {
		callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndContinueOnFailure(RejectErrorInUrlQuery.class, Condition.ConditionResult.FAILURE, "OAuth2-RT-5");

		handleAuthorizationResult();

	}

	private void handleAuthorizationResult() {
		callAndStopOnFailure(EnsureAuthorizationEndpointError.class);

		fireTestFinished();
	}

}
