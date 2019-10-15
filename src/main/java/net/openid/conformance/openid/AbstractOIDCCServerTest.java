package net.openid.conformance.openid;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddBasicAuthClientSecretAuthenticationParameters;
import net.openid.conformance.condition.client.AddFormBasedClientSecretAuthenticationParameters;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerToken;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForRefreshTokenValue;
import net.openid.conformance.condition.client.CheckForSubjectInIdToken;
import net.openid.conformance.condition.client.CheckIfAuthorizationEndpointError;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckMatchingCallbackParameters;
import net.openid.conformance.condition.client.CheckMatchingStateParameter;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import net.openid.conformance.condition.client.ExtractAccessTokenFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractCHash;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromResourceConfiguration;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromServerConfiguration;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.GenerateJWKsFromClientSecret;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetResourceEndpointConfiguration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.RejectAuthCodeInUrlQuery;
import net.openid.conformance.condition.client.RejectErrorInUrlQuery;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeFromEnvironment;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.condition.client.ValidateCHash;
import net.openid.conformance.condition.client.ValidateClientJWKsPrivatePart;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.condition.client.ValidateIdToken;
import net.openid.conformance.condition.client.ValidateIdTokenACRClaimAgainstRequest;
import net.openid.conformance.condition.client.ValidateIdTokenNonce;
import net.openid.conformance.condition.client.ValidateIdTokenSignature;
import net.openid.conformance.condition.client.ValidateIdTokenSignatureUsingKid;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesAsX509;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.common.CheckForKeyIdInServerJWKs;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import net.openid.conformance.fapi.AbstractRedirectServerTestModule;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToTokenEndpointRequest;
import net.openid.conformance.sequence.client.AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;
import net.openid.conformance.variant.VariantConfigurationFields;

@VariantParameters({
	ClientAuthType.class,
	ResponseType.class
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_basic", configurationFields = {
	"client.client_secret"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_post", configurationFields = {
	"client.client_secret"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_jwt", configurationFields = {
	"client.client_secret",
	"client.client_secret_jwt_alg"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "private_key_jwt", configurationFields = {
	"client.client_jwks"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "mtls", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca"
})
public abstract class AbstractOIDCCServerTest extends AbstractRedirectServerTestModule {

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

	@VariantSetup(parameter = ClientAuthType.class, value = "none")
	public void setupNone() {
		profileClientValidation = null;
		addTokenEndpointClientAuthentication = null;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_basic")
	public void setupClientSecretBasic() {
		profileClientValidation = null;
		addTokenEndpointClientAuthentication = AddBasicAuthClientSecretAuthenticationToTokenRequest.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_post")
	public void setupClientSecretPost() {
		profileClientValidation = null;
		addTokenEndpointClientAuthentication = AddFormBasedClientSecretAuthenticationToTokenRequest.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_jwt")
	public void setupClientSecretJwt() {
		profileClientValidation = ValidateClientForClientSecretJwt.class;
		addTokenEndpointClientAuthentication = AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		profileClientValidation = ValidateClientForPrivateKeyJwt.class;
		addTokenEndpointClientAuthentication = AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
	public void setupMtls() {
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

		responseType = getVariant(ResponseType.class);
		env.putString("response_type", responseType.toString());

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

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeFromEnvironment.class);
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
