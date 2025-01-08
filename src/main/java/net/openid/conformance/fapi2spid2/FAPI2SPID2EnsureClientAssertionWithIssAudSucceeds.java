package net.openid.conformance.fapi2spid2;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClient;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionWithIssAudAndAddToTokenEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-ensure-client-assertion-with-token-endpoint-aud-succeeds",
	displayName = "FAPI2-Security-Profile-ID2: ensure client_assertion with AS token endpoint url as aud succeeds at the token endpoint",
	summary = "This test passes a client assertion to the token endpoint where 'aud' is the Authorization Server's token endpoint instead of the Issuer ID. As per FAPI2 'NOTE: In order to facilitate interoperability the authorization server should also accept its token endpoint URL or the URL of the endpoint at which the assertion was received in the aud claim received in client authentication assertions', but as a recommended ('should') value, the AS may reject it with a valid error response and the test will end with a WARNING, which will not affect certification.",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = { "mtls" })
public class FAPI2SPID2EnsureClientAssertionWithIssAudSucceeds extends AbstractFAPI2SPID2PerformTokenEndpoint {

	@Override
	protected void addClientAuthenticationToTokenEndpointRequest() {
		if(getVariant(FAPI2ID2OPProfile.class) != FAPI2ID2OPProfile.CBUAE) {
			call(new CreateJWTClientAuthenticationAssertionWithIssAudAndAddToTokenEndpointRequest());
		} else {
			call(new CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest());
		}
	}

	@Override
	protected void processTokenEndpointResponse() {
		/* If we get an error back from the token endpoint server:
		 * - It must be a 'invalid_client' error
		 */
		if(!Strings.isNullOrEmpty(env.getString("token_endpoint_response", "error"))) {
			callAndContinueOnFailure(CheckIfTokenEndpointResponseError.class, Condition.ConditionResult.WARNING);
			callAndContinueOnFailure(CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
			callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClient.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");
		} else {
			super.processTokenEndpointResponse();
		}
		fireTestFinished();
	}
}
