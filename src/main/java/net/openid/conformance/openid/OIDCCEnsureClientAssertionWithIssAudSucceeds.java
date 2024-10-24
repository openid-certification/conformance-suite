package net.openid.conformance.openid;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClient;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForRefreshTokenValue;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromTokenResponse;
import net.openid.conformance.condition.client.UpdateClientAuthenticationAssertionClaimsWithISSAud;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.condition.client.ValidateIdTokenFromTokenResponseEncryption;
import net.openid.conformance.condition.client.VerifyIdTokenSubConsistentHybridFlow;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

// New test not present in python suite
@PublishTestModule(
	testName = "oidcc-ensure-client-assertion-with-iss-aud-succeeds",
	displayName = "OIDCC: client_assertion with AS issuer ID succeeds at the token endpoint",
	summary = "This test passes a client assertion where 'aud' is the Authorization Server's Issuer ID instead of the token endpoint. Per RFC7523-3 and Connect Core 1.0 - 3, the AS must verify that it is the intended audience, but only recommended a value. The AS should accept the AS Issuer ID as valid, but as a recommended value, the AS may reject it with a valid error response and the test will end with a WARNING, which will not affect certification.",
	profile = "OIDCC"
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = { "client_secret_basic", "client_secret_post", "mtls", "none" })
public class OIDCCEnsureClientAssertionWithIssAudSucceeds extends AbstractOIDCCServerTest {

	@Override
	protected void performPostAuthorizationFlow() {
		if (responseType.includesCode()) {
			// call the token endpoint and check response
			createAuthorizationCodeRequest();
			requestAuthorizationCode();
			// stop after checking token endpoint response
		}
		onPostAuthorizationFlowComplete();
	}

	@Override
	protected void createAuthorizationCodeRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);
		if (addTokenEndpointClientAuthentication != null) {
			call(sequenceOf(sequence(addTokenEndpointClientAuthentication)).insertAfter(CreateClientAuthenticationAssertionClaims.class, condition(UpdateClientAuthenticationAssertionClaimsWithISSAud.class)));
		}
	}

	@Override
	protected void requestAuthorizationCode() {
		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);

		/* If we get an error back from the token endpoint server:
		 * - It must be a 'invalid_client' error
		 */
		if(!Strings.isNullOrEmpty(env.getString("token_endpoint_response", "error"))) {
			callAndContinueOnFailure(CheckIfTokenEndpointResponseError.class, Condition.ConditionResult.WARNING);
			callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
			callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");
			callAndContinueOnFailure(CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClient.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		} else {
			callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
			callAndStopOnFailure(CheckForAccessTokenValue.class);
			callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

			callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, Condition.ConditionResult.INFO, "RFC6749-5.1"); // this is 'recommended' by the RFC, but we don't want to raise a warning on every test
			skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
				ValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "RFC6749-5.1");

			callAndContinueOnFailure(CheckForRefreshTokenValue.class, Condition.ConditionResult.INFO);

			skipIfMissing(new String[]{"client_jwks"}, null, Condition.ConditionResult.INFO,
				ValidateIdTokenFromTokenResponseEncryption.class, Condition.ConditionResult.WARNING, "OIDCC-10.2");
			callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "OIDCC-3.1.3.3", "OIDCC-3.3.3.3");

			// save the id_token returned from the token endpoint
			env.putObject("token_endpoint_id_token", env.getObject("id_token"));

			additionalTokenEndpointResponseValidation();

			if (responseType.includesIdToken()) {
				callAndContinueOnFailure(VerifyIdTokenSubConsistentHybridFlow.class, Condition.ConditionResult.FAILURE, "OIDCC-2");
			}
		}
	}

}
