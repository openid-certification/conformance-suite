package io.fintechlabs.testframework.example;

import io.fintechlabs.testframework.condition.client.RejectAuthCodeInUrlQuery;
import io.fintechlabs.testframework.condition.client.RejectErrorInUrlQuery;
import io.fintechlabs.testframework.fapi.AbstractRedirectServerTestModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddFormBasedClientSecretAuthenticationParameters;
import io.fintechlabs.testframework.condition.client.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CheckForAccessTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForRefreshTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForScopesInTokenResponse;
import io.fintechlabs.testframework.condition.client.CheckIfAuthorizationEndpointError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CheckMatchingStateParameter;
import io.fintechlabs.testframework.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import io.fintechlabs.testframework.condition.client.CreateRandomNonceValue;
import io.fintechlabs.testframework.condition.client.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.client.CreateRedirectUri;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.client.EnsureMinimumAccessTokenEntropy;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.FetchServerKeys;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.ValidateSHash;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.common.DisallowInsecureCipher;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureMinimumClientSecretEntropy;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;
import io.fintechlabs.testframework.condition.common.SetTLSTestHostFromConfig;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "sample-test",
	displayName = "Sample AS Test",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.client_secret",
		"client.scope",
		"tls.testHost",
		"tls.testPort",
		"mtls.cert",
		"mtls.key",
		"mtls.ca",

	},
	summary = "This is a test module summary, which describes the test module in greater detail, possibly giving the user more instructions about how to interact with this test module."
)
public class SampleTestModule extends AbstractRedirectServerTestModule {

	public static Logger logger = LoggerFactory.getLogger(SampleTestModule.class);

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#configure(com.google.gson.JsonObject)
	 */
	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		callAndStopOnFailure(SetTLSTestHostFromConfig.class);
		callAndStopOnFailure(EnsureTLS12.class, "FAPI-R-7.1-1");
		callAndContinueOnFailure(DisallowTLS10.class, "FAPI-R-7.1-1");
		callAndContinueOnFailure(DisallowTLS11.class, "FAPI-R-7.1-1");
		callAndContinueOnFailure(DisallowInsecureCipher.class, "FAPI-RW-8.5-1");

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Get the server's configuration
		callAndContinueOnFailure(GetDynamicServerConfiguration.class);

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		// fetch or load the server's keys as needed
		callAndStopOnFailure(FetchServerKeys.class);

		// Set up the client configuration
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		callAndContinueOnFailure(EnsureMinimumClientSecretEntropy.class, ConditionResult.FAILURE, "RFC6819-5.1.4.2-2", "RFC6749-10.10");

		//require(ExtractJWKsFromStaticClientConfiguration.class);

		//require(GenerateJWKsFromClientSecret.class);

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

		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class);

		performRedirect();
	}

	@Override
	protected void processCallback() {
		callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndContinueOnFailure(RejectErrorInUrlQuery.class, ConditionResult.FAILURE, "OAuth2-RT-5");

		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

		handleAuthorizationResult();

	}

	private void handleAuthorizationResult() {

		callAndStopOnFailure(CheckMatchingStateParameter.class);

		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		callAndStopOnFailure(AddFormBasedClientSecretAuthenticationParameters.class);
		//require(CreateClientAuthenticationAssertionClaims.class);

		//require(SignClientAuthenticationAssertion.class);

		//require(AddClientAssertionToTokenEndpointRequest.class);

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class, "FAPI-R-5.2.2-14");

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndStopOnFailure(CheckForScopesInTokenResponse.class, "FAPI-R-5.2.2-15");

		callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "FAPI-R-5.2.2-24");

		callAndStopOnFailure(ValidateIdToken.class, "FAPI-R-5.2.2-24");

		callAndStopOnFailure(ValidateIdTokenSignature.class, "FAPI-R-5.2.2-24");

		callAndContinueOnFailure(ValidateSHash.class, "FAPI-RW-5.2.2-4");

		callAndContinueOnFailure(CheckForRefreshTokenValue.class);

		callAndStopOnFailure(EnsureMinimumAccessTokenEntropy.class, "FAPI-R-5.2.2-16");


		fireTestFinished();
	}

}
