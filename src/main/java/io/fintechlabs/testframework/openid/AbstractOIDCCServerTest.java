package io.fintechlabs.testframework.openid;

import java.util.List;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddBasicAuthClientSecretAuthenticationParameters;
import io.fintechlabs.testframework.condition.client.AddFormBasedClientSecretAuthenticationParameters;
import io.fintechlabs.testframework.condition.client.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CallProtectedResourceWithBearerToken;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CheckForAccessTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForRefreshTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForSubjectInIdToken;
import io.fintechlabs.testframework.condition.client.CheckIfAuthorizationEndpointError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CheckMatchingCallbackParameters;
import io.fintechlabs.testframework.condition.client.CheckMatchingStateParameter;
import io.fintechlabs.testframework.condition.client.ConfigurationRequestsTestIsSkipped;
import io.fintechlabs.testframework.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import io.fintechlabs.testframework.condition.client.CreateRandomNonceValue;
import io.fintechlabs.testframework.condition.client.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.client.CreateRedirectUri;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractCHash;
import io.fintechlabs.testframework.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractJWKsFromStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractMTLSCertificatesFromConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractTLSTestValuesFromResourceConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractTLSTestValuesFromServerConfiguration;
import io.fintechlabs.testframework.condition.client.FetchServerKeys;
import io.fintechlabs.testframework.condition.client.GenerateJWKsFromClientSecret;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.client.GetResourceEndpointConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.RejectAuthCodeInUrlQuery;
import io.fintechlabs.testframework.condition.client.RejectErrorInUrlQuery;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeFromConfig;
import io.fintechlabs.testframework.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import io.fintechlabs.testframework.condition.client.ValidateCHash;
import io.fintechlabs.testframework.condition.client.ValidateClientJWKsPrivatePart;
import io.fintechlabs.testframework.condition.client.ValidateExpiresIn;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenACRClaimAgainstRequest;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenNonce;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignatureUsingKid;
import io.fintechlabs.testframework.condition.client.ValidateMTLSCertificatesAsX509;
import io.fintechlabs.testframework.condition.client.ValidateMTLSCertificatesHeader;
import io.fintechlabs.testframework.condition.client.ValidateServerJWKs;
import io.fintechlabs.testframework.condition.common.CheckForKeyIdInServerJWKs;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.fapi.AbstractFAPIRWID2ServerTestModule;
import io.fintechlabs.testframework.fapi.AbstractRedirectServerTestModule;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;
import io.fintechlabs.testframework.sequence.ConditionSequence;
import io.fintechlabs.testframework.sequence.client.AddMTLSClientAuthenticationToTokenEndpointRequest;
import io.fintechlabs.testframework.sequence.client.AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest;
import io.fintechlabs.testframework.testmodule.TestFailureException;

public abstract class AbstractOIDCCServerTest extends AbstractRedirectServerTestModule {

	// Variants
	public static final String variant_none = "none";
	public static final String variant_client_secret_basic = "client_secret_basic";
	public static final String variant_client_secret_post = "client_secret_post";
	public static final String variant_client_secret_jwt = "client_secret_jwt";
	public static final String variant_private_key_jwt = "private_key_jwt";
	public static final String variant_mtls = "mtls";

	public static final List<ResponseType> SUPPORTED_RESPONSE_TYPES = List.of(
			ResponseType.CODE,
			ResponseType.ID_TOKEN,
			ResponseType.ID_TOKEN_TOKEN,
			ResponseType.CODE_ID_TOKEN,
			ResponseType.CODE_TOKEN,
			ResponseType.CODE_ID_TOKEN_TOKEN);

	protected ResponseType responseType;

	protected Class<? extends ConditionSequence> profileClientValidation;
	protected Class<? extends ConditionSequence> addTokenEndpointClientAuthentication;

	public static class ValidateClientForClientSecretJwt extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(GenerateJWKsFromClientSecret.class);
		}
	}

	public static class ValidateClientForMtls extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
			callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateMTLSCertificatesAsX509.class, Condition.ConditionResult.FAILURE);
			// FIXME: do we need to do second client mtls too?
		}
	}

	public static class ValidateClientForPrivateKeyJwt extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(ValidateClientJWKsPrivatePart .class, "RFC7517-1.1");
			callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration .class);
		}
	}

	public static class AddBasicAuthClientSecretAuthenticationToTokenRequest extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(AddBasicAuthClientSecretAuthenticationParameters.class);
		}
	}

	public static class AddFormBasedClientSecretAuthenticationToTokenRequest extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(AddFormBasedClientSecretAuthenticationParameters.class);
		}
	}

	protected void setupNone() {
		profileClientValidation = null;
		addTokenEndpointClientAuthentication = null;
	}

	protected void setupClientSecretBasic() {
		profileClientValidation = null;
		addTokenEndpointClientAuthentication = AddBasicAuthClientSecretAuthenticationToTokenRequest.class;
	}

	protected void setupClientSecretPost() {
		profileClientValidation = null;
		addTokenEndpointClientAuthentication = AddFormBasedClientSecretAuthenticationToTokenRequest.class;
	}

	protected void setupClientSecretJwt() {
		profileClientValidation = ValidateClientForClientSecretJwt.class;
		addTokenEndpointClientAuthentication = AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest.class;
	}

	protected void setupPrivateKeyJwt() {
		profileClientValidation = ValidateClientForPrivateKeyJwt.class;
		addTokenEndpointClientAuthentication = AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest.class;
	}

	protected void setupMtls() {
		profileClientValidation = ValidateClientForMtls.class;
		addTokenEndpointClientAuthentication = AddMTLSClientAuthenticationToTokenEndpointRequest.class;
	}

	@Override
	public final void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		Boolean skip = env.getBoolean("config", "skip_test");
		if (skip != null && skip) {
			// This is intended for use in our CI where we insist all tests run to completion
			// It would be used as a temporary measure in an 'override' where one of the environments we are testing
			// against is not able to run the test to completion due to an issue in that environments.
			callAndContinueOnFailure(ConfigurationRequestsTestIsSkipped.class, Condition.ConditionResult.FAILURE);
			fireTestFinished();
			return;
		}

		String responseTypeStr = env.getString("config", "response_type");
		if (responseTypeStr == null) {
			throw new TestFailureException(getId(), "No response_type was found in the config");
		}

		responseType = ResponseType.parse(responseTypeStr);

		if (!SUPPORTED_RESPONSE_TYPES.contains(responseType)) {
			throw new TestFailureException(getId(), "Not a supported response type: " + responseType);
		}

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Make sure we're calling the right server configuration
		callAndStopOnFailure(GetDynamicServerConfiguration.class);

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(ExtractTLSTestValuesFromServerConfiguration.class);

		callAndStopOnFailure(FetchServerKeys.class);
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
		callAndContinueOnFailure(CheckForKeyIdInServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");

		// Set up the client configuration
		configureClient();

		// Set up the resource endpoint configuration
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		callAndStopOnFailure(SetProtectedResourceUrlToSingleResourceEndpoint.class);

		callAndStopOnFailure(ExtractTLSTestValuesFromResourceConfiguration.class);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void configureClient() {
		// Set up the client configuration
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		exposeEnvString("client_id");

		validateClientConfiguration();
	}

	protected void validateClientConfiguration() {
		if (profileClientValidation != null) {
			call(sequence(profileClientValidation));
		}
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		performAuthorizationFlow();
	}

	protected void performAuthorizationFlow() {
		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
		createAuthorizationRequest();
		createAuthorizationRedirect();
		performRedirect();
		eventLog.endBlock();
	}

	protected void createAuthorizationRequest() {
		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeFromConfig.class);
	}

	protected void createAuthorizationRedirect() {
		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class);
	}

	protected void processCallback() {
		eventLog.startBlock(currentClientString() + "Verify authorization endpoint response");

		if (isCodeFlow()) {
			env.mapKey("authorization_endpoint_response", "callback_query_params");
		} else {
			env.mapKey("authorization_endpoint_response", "callback_params");

			callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");
			callAndContinueOnFailure(RejectErrorInUrlQuery.class, Condition.ConditionResult.FAILURE, "OAuth2-RT-5");
		}

		onAuthorizationCallbackResponse();
		eventLog.endBlock();
	}

	protected void onAuthorizationCallbackResponse() {
		callAndStopOnFailure(CheckMatchingCallbackParameters.class);
		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);
		callAndStopOnFailure(CheckMatchingStateParameter.class);
		if (responseType.includesCode()) {
			callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);
		}
		if (responseType.includesToken()) {
			callAndStopOnFailure(ExtractAccessTokenFromAuthorizationResponse.class);
		}
		handleSuccessfulAuthorizationEndpointResponse();
	}

	protected void handleSuccessfulAuthorizationEndpointResponse() {
		if (responseType.includesIdToken()) {
			callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class);

			// save the id_token returned from the authorisation endpoint
			env.putObject("authorization_endpoint_id_token", env.getObject("id_token"));

			performIdTokenValidation();
		}
		if (responseType.includesCode()) {
			performAuthorizationCodeValidation();
		}
		if (responseType.includesToken()) {
			requestProtectedResource();
		}
		performPostAuthorizationFlow();
	}

	protected void performIdTokenValidation() {
		callAndContinueOnFailure(ValidateIdToken.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateIdTokenNonce.class, ConditionResult.FAILURE, "OIDCC-2");
		callAndContinueOnFailure(ValidateIdTokenACRClaimAgainstRequest.class, Condition.ConditionResult.FAILURE, "OIDCC-5.5.1.1");
		callAndContinueOnFailure(ValidateIdTokenSignature.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateIdTokenSignatureUsingKid.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckForSubjectInIdToken.class, ConditionResult.FAILURE);
	}

	protected void performAuthorizationCodeValidation() {
		if (responseType.includesIdToken()) {
			callAndContinueOnFailure(ExtractCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

			skipIfMissing(new String[] { "c_hash" }, null, Condition.ConditionResult.INFO,
					ValidateCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");
		}
	}

	protected void performPostAuthorizationFlow() {
		if (responseType.includesCode()) {
			// call the token endpoint and complete the flow
			createAuthorizationCodeRequest();
			requestAuthorizationCode();
		}
		requestProtectedResource();
		onPostAuthorizationFlowComplete();
	}

	protected void createAuthorizationCodeRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);
		if (addTokenEndpointClientAuthentication != null) {
			call(sequence(addTokenEndpointClientAuthentication));
		}
	}

	protected void requestAuthorizationCode() {
		callAndStopOnFailure(CallTokenEndpoint.class);
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
		callAndStopOnFailure(CheckForAccessTokenValue.class);
		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class);
		skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
			ValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "RFC6749-5.1");

		callAndContinueOnFailure(CheckForRefreshTokenValue.class);

		if (responseType.includesIdToken()) {
			callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "OIDCC-3.3.2.5");
			performIdTokenValidation();

			callAndContinueOnFailure(CheckForSubjectInIdToken.class, ConditionResult.FAILURE);
		}
	}

	protected void requestProtectedResource() {
		eventLog.startBlock(currentClientString() + "Resource server endpoint tests");
		callAndStopOnFailure(CallProtectedResourceWithBearerToken.class);
		eventLog.endBlock();
	}

	protected void onPostAuthorizationFlowComplete() {
		fireTestFinished();
	}

	protected String currentClientString() {
		return "";
	}

	protected boolean isCodeFlow() {
		return responseType.equals(ResponseType.CODE);
	}

	protected boolean isHybridFlow() {
		return responseType.includesCode() && !isCodeFlow();
	}

	protected boolean isImplicitFlow() {
		return !responseType.includesCode();
	}
}
