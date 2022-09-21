package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddAudAsPaymentInitiationUriToRequestObject;
import net.openid.conformance.condition.client.AddAudToRequestObject;
import net.openid.conformance.condition.client.AddBasicAuthClientSecretAuthenticationParameters;
import net.openid.conformance.condition.client.AddExpToRequestObject;
import net.openid.conformance.condition.client.AddFormBasedClientIdAuthenticationParameters;
import net.openid.conformance.condition.client.AddFormBasedClientSecretAuthenticationParameters;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddIssAsCertificateOuToRequestObject;
import net.openid.conformance.condition.client.AddIssAsDidJwkForProofKeyToRequestObject;
import net.openid.conformance.condition.client.AddJtiAsUuidToRequestObject;
import net.openid.conformance.condition.client.AddJtiToRequestObject;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromAuthorizationEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForRefreshTokenValue;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForPreAuthorizedCodeGrant;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureMinimumAuthorizationCodeEntropy;
import net.openid.conformance.condition.client.EnsureMinimumAuthorizationCodeLength;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsClientAuthNone;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsClientSecretBasic;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsClientSecretPost;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsMTLS;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsPrivateKeyJwt;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificates2FromConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.ExtractPreAuthorizedCodeFromIssuanceInitiationRequest;
import net.openid.conformance.condition.client.GenerateJWKsFromClientSecret;
import net.openid.conformance.condition.client.GenerateProofKey;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetStaticServerConfiguration;
import net.openid.conformance.condition.client.OID4VCCreateCredentialRequest;
import net.openid.conformance.condition.client.RejectAuthCodeInAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.SetApplicationJsonContentTypeHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseModeToFormPost;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeFromEnvironment;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToCredentialEndpoint;
import net.openid.conformance.condition.client.SetResourceMethodToPost;
import net.openid.conformance.condition.client.SetScopeInClientConfigurationToOpenId;
import net.openid.conformance.condition.client.SignProof;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClient;
import net.openid.conformance.condition.client.ValidateClientJWKsPrivatePart;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromAuthorizationEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromAuthorizationEndpointResponseError;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.condition.client.ValidateIssInAuthorizationResponse;
import net.openid.conformance.condition.client.ValidateIssuerInIssuanceInitiationRequest;
import net.openid.conformance.condition.client.ValidateMTLSCertificates2Header;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesAsX509;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.sequence.client.OIDCCCreateDynamicClientRegistrationRequest;
import net.openid.conformance.testmodule.AbstractRedirectServerTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

import java.util.function.Supplier;

@PublishTestModule(
	testName = "oid4vci-happy-flow",
	displayName = "OID4VCI: Happy flow",
	summary = "Tests primarily 'happy' flow for pre-authorized code flow",
	profile = "OID4VCI"
)
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "static", configurationFields = {
	"server.issuer",
	"server.jwks_uri",
	"server.authorization_endpoint",
	"server.token_endpoint"
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "discovery", configurationFields = {
	"server.discoveryUrl"
})
@VariantParameters({
	ServerMetadata.class
})
public class OID4VCIHappyFlow extends AbstractRedirectServerTestModule {

	@Override
	public final void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putString("external_url_override", externalUrlOverride);
		env.putObject("config", config);

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		switch (getVariant(ServerMetadata.class)) {
			case DISCOVERY:
				callAndStopOnFailure(GetDynamicServerConfiguration.class);
				break;
			case STATIC:
				callAndStopOnFailure(GetStaticServerConfiguration.class);
				break;
		}

		// Perform any custom configuration
		onConfigure(config, baseUrl);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void onConfigure(JsonObject config, String baseUrl) {

		// No custom configuration
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		performAuthorizationFlow();
	}

	protected void performAuthorizationFlow() {
		env.putString("redirect_to_authorization_endpoint", "https://idp.research.identiproof.io");
		performRedirect();
		eventLog.endBlock();
	}

	protected void createAuthorizationRedirect() {
		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class);
	}

	@Override
	protected void processCallback() {
		eventLog.startBlock(currentClientString() + "Verify Issuance Initiation Request");

		env.mapKey("issuance_initiation_request", "callback_query_params");

		onAuthorizationCallbackResponse();
		eventLog.endBlock();
	}

	protected void onAuthorizationCallbackResponse() {
		callAndContinueOnFailure(ValidateIssuerInIssuanceInitiationRequest.class, Condition.ConditionResult.FAILURE, "OIDC4VCI-6.1");
		// FIXME other defined values not currently checked:
		// credential_type (care required: may appear more than once)
		// user_pin_required
		// op_state

		callAndStopOnFailure(ExtractPreAuthorizedCodeFromIssuanceInitiationRequest.class, "OIDC4VCI-6.1");

		// FIXME verify length/entropy of code?

		handleSuccessfulAuthorizationEndpointResponse();
	}

	protected void handleSuccessfulAuthorizationEndpointResponse() {
		performPostAuthorizationFlow();
	}

	protected void performPostAuthorizationFlow() {
		// call the token endpoint and complete the flow
		createPreAuthedTokenEndpointRequest();
		requestAccessToken();
		requestProtectedResource();

		onPostAuthorizationFlowComplete();
	}

	protected void createPreAuthedTokenEndpointRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForPreAuthorizedCodeGrant.class);
	}

	protected void requestAccessToken() {
		callAndStopOnFailure(CallTokenEndpoint.class);
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
		callAndStopOnFailure(CheckForAccessTokenValue.class);
		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, Condition.ConditionResult.INFO, "RFC6749-5.1"); // this is 'recommended' by the RFC, but we don't want to raise a warning on every test
		skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
			ValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "RFC6749-5.1");

		callAndContinueOnFailure(CheckForRefreshTokenValue.class);

		// issue warning if expires_in is missing (RFC6749 recommends it)
		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, Condition.ConditionResult.WARNING, "RFC6749-5.1");
	}

	protected void requestProtectedResource() {
		eventLog.startBlock(currentClientString() + "Call credential endpoint");
		callAndStopOnFailure(SetProtectedResourceUrlToCredentialEndpoint.class, "OID4VCI-9");
		callAndStopOnFailure(SetResourceMethodToPost.class);
		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);
		callAndStopOnFailure(SetApplicationJsonContentTypeHeaderForResourceEndpointRequest.class);

		callAndStopOnFailure(GenerateProofKey.class);

		env.putObject("proof_claims", new JsonObject());

		// we reuse the request object conditions to add various jwt claims; it would perhaps make sense to make
		// these more generic.
		call(exec().mapKey("request_object_claims", "proof_claims"));

		// one of kid, jwk, x5c

		// iss MUST be the client_id of the client making the credential request.

		callAndStopOnFailure(AddIatToRequestObject.class, "OID4VCI-9.2");
		callAndStopOnFailure(AddExpToRequestObject.class, "OID4VCI-9.2"); // not required by spec, but in identiproof.io examples
		callAndStopOnFailure(AddAudToRequestObject.class, "OID4VCI-9.2");
		callAndStopOnFailure(AddJtiToRequestObject.class, "OID4VCI-9.2"); // not required by spec, but in identiproof.io examples
		// nonce is required by spec but is not in the identiproof.io examples

		callAndStopOnFailure(AddIssAsDidJwkForProofKeyToRequestObject.class, "OID4VCI-9.2");

		call(exec().unmapKey("request_object_claims"));

		callAndStopOnFailure(SignProof.class);

		callAndStopOnFailure(OID4VCCreateCredentialRequest.class);
		callAndStopOnFailure(CallProtectedResource.class);
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));

		// FIXME: validate response & contained credentials

		eventLog.endBlock();
	}

	protected void onPostAuthorizationFlowComplete() {
		fireTestFinished();
	}

	protected String currentClientString() {
		return "";
	}
}
