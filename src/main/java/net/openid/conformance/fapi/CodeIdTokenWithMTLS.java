package net.openid.conformance.fapi;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddClientIdToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddCodeChallengeToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddCodeVerifierToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddMTLSEndpointAliasesToEnvironment;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerTokenAndCustomHeaders;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForDateHeaderInResourceResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CheckForRefreshTokenValue;
import net.openid.conformance.condition.client.CheckForScopesInTokenResponse;
import net.openid.conformance.condition.client.CheckForSubjectInIdToken;
import net.openid.conformance.condition.client.CheckIfAuthorizationEndpointError;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckMatchingStateParameter;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
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
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsMTLS;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractAtHash;
import net.openid.conformance.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractCHash;
import net.openid.conformance.condition.client.ExtractIdTokenFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractMTLSCertificates2FromConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.ExtractSHash;
import net.openid.conformance.condition.client.FAPIGenerateResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.GetStaticClient2Configuration;
import net.openid.conformance.condition.client.RejectAuthCodeInUrlQuery;
import net.openid.conformance.condition.client.RejectErrorInUrlQuery;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import net.openid.conformance.condition.client.ValidateAtHash;
import net.openid.conformance.condition.client.ValidateCHash;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateIdToken;
import net.openid.conformance.condition.client.ValidateIdTokenSignature;
import net.openid.conformance.condition.client.ValidateIdTokenSignatureUsingKid;
import net.openid.conformance.condition.client.ValidateMTLSCertificates2Header;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesAsX509;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.condition.client.ValidateSHash;
import net.openid.conformance.condition.common.DisallowInsecureCipher;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12WithFAPICiphers;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FapiRClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

@VariantNotApplicable(parameter = FapiRClientAuthType.class, values = {
	"none", "client_secret_jwt", "private_key_jwt"
})
@PublishTestModule(
	testName = "fapi-r-code-id-token-with-mtls",
	displayName = "FAPI-R: code id_token (MTLS authentication)",
	profile = "FAPI-R",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"resource.resourceUrl"
	}
)
public class CodeIdTokenWithMTLS extends AbstractFapiRServerTestModule {

	@Override
	protected void setupClient() {
		callAndContinueOnFailure(EnsureServerConfigurationSupportsMTLS.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-6");

		callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, ConditionResult.WARNING);
		callAndStopOnFailure(ExtractMTLSCertificatesFromConfiguration.class);

		// get the second client and second MTLS cert set for mixup tests
		callAndStopOnFailure(GetStaticClient2Configuration.class);
		callAndContinueOnFailure(ValidateMTLSCertificates2Header.class, ConditionResult.WARNING);
		callAndStopOnFailure(ExtractMTLSCertificates2FromConfiguration.class);

		// Validate the MTLS keys
		callAndContinueOnFailure(ValidateMTLSCertificatesAsX509.class, ConditionResult.FAILURE);

		// validate the secondary MTLS keys
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");
		callAndContinueOnFailure(ValidateMTLSCertificatesAsX509.class, ConditionResult.FAILURE);
		env.unmapKey("mutual_tls_authentication");
	}

	@Override
	protected void supportMTLSEndpointAliases() {
		callAndContinueOnFailure(AddMTLSEndpointAliasesToEnvironment.class, Condition.ConditionResult.INFO, "MTLS-5");
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		eventLog.startBlock("Authorization endpoint TLS test");
		env.mapKey("tls", "authorization_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12WithFAPICiphers.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");

		eventLog.startBlock("Token Endpoint TLS test");
		env.mapKey("tls", "token_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12WithFAPICiphers.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");

		eventLog.startBlock("Userinfo Endpoint TLS test");
		env.mapKey("tls", "userinfo_endpoint_tls");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, EnsureTLS12WithFAPICiphers.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");

		eventLog.startBlock("Registration Endpoint TLS test");
		env.mapKey("tls", "registration_endpoint_tls");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, EnsureTLS12WithFAPICiphers.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");

		eventLog.startBlock("Resource Endpoint TLS test");
		env.mapKey("tls", "resource_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12WithFAPICiphers.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");

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
		callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndContinueOnFailure(RejectErrorInUrlQuery.class, ConditionResult.FAILURE, "OAuth2-RT-5");

		// code id_token, so response should be in the hash
		env.mapKey("authorization_endpoint_response", "callback_params");

		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

		if (env.isKeyMapped("client")) {
			// we're doing the second client
			handleSecondClientAuthorizationResult();
		} else {
			// we're doing the first client
			handleAuthorizationResult();
		}

	}

	private void handleAuthorizationResult() {


		callAndStopOnFailure(CheckMatchingStateParameter.class);

		// check the ID token from the hybrid response

		callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class, "FAPI-RW-5.2.2-3");

		// save the id_token returned from the authorisation endpoint
		env.putObject("authorization_endpoint_id_token", env.getObject("id_token"));

		callAndStopOnFailure(ValidateIdToken.class, "FAPI-RW-5.2.2-3");

		callAndContinueOnFailure(ValidateIdTokenSignature.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-3");

		// This condition is a warning because we're not yet 100% sure of the code
		callAndContinueOnFailure(ValidateIdTokenSignatureUsingKid.class, ConditionResult.WARNING, "FAPI-RW-5.2.2-3");

		callAndStopOnFailure(CheckForSubjectInIdToken.class, "FAPI-R-5.2.2-24");

		callAndContinueOnFailure(ExtractSHash.class, ConditionResult.INFO, "FAPI-RW-5.2.2-4");

		skipIfMissing(new String[]{"s_hash"}, null, ConditionResult.INFO,
			ValidateSHash.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

		callAndContinueOnFailure(ExtractCHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		skipIfMissing(new String[]{"c_hash"}, null, ConditionResult.INFO,
			ValidateCHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		// call the token endpoint and complete the flow

		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);

		call(condition(AddCodeVerifierToTokenEndpointRequest.class));

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class, "FAPI-R-5.2.2-14");

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndStopOnFailure(CheckForScopesInTokenResponse.class, "FAPI-R-5.2.2-15");

		callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "FAPI-R-5.2.2-24");

		callAndStopOnFailure(ValidateIdToken.class, "FAPI-R-5.2.2-24");

		callAndContinueOnFailure(ValidateIdTokenSignature.class, ConditionResult.FAILURE,"FAPI-R-5.2.2-24");

		// This condition is a warning because we're not yet 100% sure of the code
		callAndContinueOnFailure(ValidateIdTokenSignatureUsingKid.class, ConditionResult.WARNING, "FAPI-R-5.2.2-24");

		callAndStopOnFailure(CheckForSubjectInIdToken.class, "FAPI-R-5.2.2-24");

		callAndContinueOnFailure(ExtractSHash.class, ConditionResult.INFO, "FAPI-RW-5.2.2-4");

		skipIfMissing(new String[]{"s_hash"}, null, ConditionResult.INFO,
			ValidateSHash.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

		eventLog.startBlock("Verify at_hash in the authorization endpoint id_token");

		env.mapKey("id_token","authorization_endpoint_id_token");

		callAndContinueOnFailure(ExtractAtHash.class, ConditionResult.INFO, "OIDCC-3.3.2.11");

		skipIfMissing(new String[] { "at_hash" }, null, ConditionResult.INFO,
			ValidateAtHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		env.unmapKey("id_token");

		eventLog.endBlock();

		callAndContinueOnFailure(CheckForRefreshTokenValue.class);

		callAndContinueOnFailure(EnsureMinimumAccessTokenLength.class, ConditionResult.FAILURE, "FAPI-R-5.2.2-16");

		callAndContinueOnFailure(EnsureMinimumAccessTokenEntropy.class, ConditionResult.FAILURE, "FAPI-R-5.2.2-16");

		// verify the access token against a protected resource

		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
		exposeEnvString("fapi_interaction_id");

		callAndStopOnFailure(FAPIGenerateResourceEndpointRequestHeaders.class);

		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "FAPI-R-6.2.2-6");

		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, "FAPI-R-6.2.1-1", "FAPI-R-6.2.1-3");

		callAndStopOnFailure(CheckForDateHeaderInResourceResponse.class, "FAPI-R-6.2.1-11");

		callAndStopOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, "FAPI-R-6.2.1-11");

		callAndContinueOnFailure(EnsureMatchingFAPIInteractionId.class, ConditionResult.FAILURE, "FAPI-R-6.2.1-11");

		callAndStopOnFailure(EnsureResourceResponseReturnedJsonContentType.class, "FAPI-R-6.2.1-9", "FAPI-R-6.2.1-10");

		callAndStopOnFailure(DisallowAccessTokenInQuery.class, "FAPI-R-6.2.1-4");

		// get token for second client
		eventLog.startBlock("Second client");
		env.mapKey("client", "client2");
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");

		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);

		call(condition(CreateRandomCodeVerifier.class).requirement("RFC7636-4.1"));
		call(exec().exposeEnvironmentString("code_verifier"));
		call(condition(CreateS256CodeChallenge.class));
		call(exec()
			.exposeEnvironmentString("code_challenge")
			.exposeEnvironmentString("code_challenge_method"));
		call(condition(AddCodeChallengeToAuthorizationEndpointRequest.class)
			.requirement("FAPI-R-5.2.2-7"));

		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class);

		performRedirect();

	}

	private void handleSecondClientAuthorizationResult() {

		// we skip the validation steps for the second client and as long as it's not an error we use the results for negative testing

		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		call(condition(AddCodeVerifierToTokenEndpointRequest.class));

		eventLog.startBlock("Attempt to use authorization code obtained by client 2 with the client_id and client certificate for client 1");
		env.unmapKey("client");
		env.unmapKey("mutual_tls_authentication");

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);

		env.mapKey("client", "client2");

		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);

		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");

		callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");

		// put everything back where we found it

		env.unmapKey("client");
		env.unmapKey("mutual_tls_authentication");
		eventLog.endBlock();

		fireTestFinished();

	}

}
