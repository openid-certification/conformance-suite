package net.openid.conformance.fapi;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddClientIdToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddCodeChallengeToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddCodeVerifierToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerTokenAndCustomHeaders;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForDateHeaderInResourceResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CheckForRefreshTokenValue;
import net.openid.conformance.condition.client.CheckForScopesInTokenResponse;
import net.openid.conformance.condition.client.CheckForSubjectInIdToken;
import net.openid.conformance.condition.client.CheckIfAuthorizationEndpointError;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckMatchingStateParameter;
import net.openid.conformance.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import net.openid.conformance.condition.client.CreateRandomCodeVerifier;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.condition.client.CreateS256CodeChallenge;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import net.openid.conformance.condition.client.DisallowAccessTokenInQuery;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureMinimumAccessTokenEntropy;
import net.openid.conformance.condition.client.EnsureMinimumAccessTokenLength;
import net.openid.conformance.condition.client.EnsureResourceResponseReturnedJsonContentType;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractSHash;
import net.openid.conformance.condition.client.FAPIGenerateResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.RejectAuthCodeInUrlQuery;
import net.openid.conformance.condition.client.RejectErrorInUrlQuery;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import net.openid.conformance.condition.client.ValidateIdToken;
import net.openid.conformance.condition.client.ValidateIdTokenSignature;
import net.openid.conformance.condition.client.ValidateIdTokenSignatureUsingKid;
import net.openid.conformance.condition.client.ValidateSHash;
import net.openid.conformance.condition.common.DisallowInsecureCipher;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12WithFAPICiphers;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FapiRClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

@VariantNotApplicable(parameter = FapiRClientAuthType.class, values = {
	"client_secret_jwt", "private_key_jwt", "mtls"
})
@PublishTestModule(
	testName = "fapi-r-code-id-token-with-pkce",
	displayName = "FAPI-R: code id_token (Public Client with PKCE/S256)",
	profile = "FAPI-R",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"resource.resourceUrl",
		"resource.institution_id"
	}
)
public class CodeIdTokenWithPKCE extends AbstractFapiRServerTestModule {

	@Override
	protected void setupClient() {
	}

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		eventLog.startBlock("Authorization endpoint TLS test");
		env.mapKey("tls", "authorization_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");

		eventLog.startBlock("Token Endpoint TLS test");
		env.mapKey("tls", "token_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");

		eventLog.startBlock("Userinfo Endpoint TLS test");
		env.mapKey("tls", "userinfo_endpoint_tls");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");

		eventLog.startBlock("Registration Endpoint TLS test");
		env.mapKey("tls", "registration_endpoint_tls");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");

		eventLog.startBlock("Resource Endpoint TLS test");
		env.mapKey("tls", "resource_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");

		eventLog.endBlock();
		env.unmapKey("tls");

		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

		call(condition(CreateRandomCodeVerifier.class).requirement("RFC7636-4.1"));
		call(exec().exposeEnvironmentString("code_verifier"));
		call(condition(CreateS256CodeChallenge.class));
		call(exec()
			.exposeEnvironmentString("code_challenge")
			.exposeEnvironmentString("code_challenge_method"));
		call(condition(AddCodeChallengeToAuthorizationEndpointRequest.class)
			.requirement("FAPI-R-5.2.2-7"));

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);

		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class);

		performRedirect();
	}

	@Override
	protected void processCallback() {
		callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndContinueOnFailure(RejectErrorInUrlQuery.class, Condition.ConditionResult.FAILURE, "OAuth2-RT-5");

		env.mapKey("authorization_endpoint_response", "callback_params");

		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

		handleAuthorizationResult();

	}

	private void handleAuthorizationResult() {

		callAndStopOnFailure(CheckMatchingStateParameter.class);

		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);

		callAndStopOnFailure(AddCodeVerifierToTokenEndpointRequest.class);

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class, "FAPI-R-5.2.2-14");

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndStopOnFailure(CheckForScopesInTokenResponse.class, "FAPI-R-5.2.2-15");

		callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "FAPI-R-5.2.2-24");

		callAndStopOnFailure(ValidateIdToken.class, "FAPI-R-5.2.2-24");

		callAndContinueOnFailure(ValidateIdTokenSignature.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-24");

		// This condition is a warning because we're not yet 100% sure of the code
		callAndContinueOnFailure(ValidateIdTokenSignatureUsingKid.class, Condition.ConditionResult.WARNING, "FAPI-R-5.2.2-24");

		callAndStopOnFailure(CheckForSubjectInIdToken.class, "FAPI-R-5.2.2-24");

		callAndContinueOnFailure(ExtractSHash.class, Condition.ConditionResult.INFO, "FAPI-RW-5.2.2-4");

		skipIfMissing(new String[]{"s_hash"}, null, Condition.ConditionResult.INFO,
			ValidateSHash.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

		callAndContinueOnFailure(CheckForRefreshTokenValue.class);

		callAndContinueOnFailure(EnsureMinimumAccessTokenLength.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-16");

		callAndContinueOnFailure(EnsureMinimumAccessTokenEntropy.class, "FAPI-R-5.2.2-16");

		// verify the access token against a protected resource

		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
		exposeEnvString("fapi_interaction_id");

		callAndStopOnFailure(FAPIGenerateResourceEndpointRequestHeaders.class);

		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class);

		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, "FAPI-R-6.2.1-1", "FAPI-R-6.2.1-3");

		callAndStopOnFailure(CheckForDateHeaderInResourceResponse.class, "FAPI-R-6.2.1-11");

		callAndStopOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, "FAPI-R-6.2.1-11");

		callAndContinueOnFailure(EnsureMatchingFAPIInteractionId.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");

		callAndStopOnFailure(EnsureResourceResponseReturnedJsonContentType.class, "FAPI-R-6.2.1-9", "FAPI-R-6.2.1-10");

		callAndStopOnFailure(DisallowAccessTokenInQuery.class, "FAPI-R-6.2.1-4");

		fireTestFinished();
	}

}
