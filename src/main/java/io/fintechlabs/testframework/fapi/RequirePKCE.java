package io.fintechlabs.testframework.fapi;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import io.fintechlabs.testframework.condition.client.CreateRandomNonceValue;
import io.fintechlabs.testframework.condition.client.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.client.CreateRedirectUri;
import io.fintechlabs.testframework.condition.client.EnsureEmptyCallbackUrlQuery;
import io.fintechlabs.testframework.condition.client.EnsureInvalidRequestError;
import io.fintechlabs.testframework.condition.client.ExpectPKCEError;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.RejectAuthCodeInUrlQuery;
import io.fintechlabs.testframework.condition.client.RejectErrorInUrlQuery;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-r-require-pkce",
	displayName = "FAPI-R: Require PKCE in authorization request (code id_token)",
	profile = "FAPI-R",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope"
	}
)
public class RequirePKCE extends AbstractRedirectServerTestModule {

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#configure(com.google.gson.JsonObject, io.fintechlabs.testframework.logging.EventLog, java.lang.String, io.fintechlabs.testframework.frontChannel.BrowserControl, java.lang.String)
	 */
	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		// Create a redirect URI (this will be removed from the actual request)
		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Make sure we're calling the right server configuration
		callAndStopOnFailure(GetDynamicServerConfiguration.class);

		// Make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		// Set up the client configuration
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		exposeEnvString("client_id");

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#start()
	 */
	@Override
	public void start() {
		setStatus(Status.RUNNING);

		// Create a valid authorization request
		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);

		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class);

		performRedirectAndWaitForErrorCallback();
	}

	@Override
	protected void processCallback() {
		callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndContinueOnFailure(RejectErrorInUrlQuery.class, ConditionResult.FAILURE, "OAuth2-RT-5");

		handleAuthorizationResult();
	}

	private void handleAuthorizationResult() {
		// code id_token, so response should be in the hash
		env.mapKey("authorization_endpoint_response", "callback_params");

		callAndContinueOnFailure(EnsureInvalidRequestError.class, ConditionResult.FAILURE, "OIDCC-3.3.2.6");

		callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndContinueOnFailure(RejectErrorInUrlQuery.class, ConditionResult.FAILURE, "OAuth2-RT-5");

		callAndContinueOnFailure(EnsureEmptyCallbackUrlQuery.class, ConditionResult.FAILURE, "OIDCC-3.3.2.6");

		fireTestFinished();
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectPKCEError.class, "FAPI-R-5.2.2-7");

		env.putString("error_callback_placeholder", env.getString("pkce_error"));
	}
}
