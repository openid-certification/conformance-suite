package net.openid.conformance.fapirwid2;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddClientAssertionToTokenEndpointRequest;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClient;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.SignClientAuthenticationAssertion;
import net.openid.conformance.condition.client.UpdateClientAuthenticationAssertionClaimsWithISSAud;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-client-assertion-with-iss-aud-succeeds",
	displayName = "FAPI-RW-ID2: ensure client_assertion with AS issuer ID succeeds at the token endpoint",
	summary = "This test passes a client assertion where 'aud' is the Authorization Server's Issuer ID instead of the token endpoint. Per RFC7523-3 and OpenID Connect Core 1.0 - 9, the AS must verify that it is the intended audience, but only recommended a value. The AS should accept the AS Issuer ID as valid, but as a recommended value, the AS may reject it with a valid error response and the test will end with a WARNING, which will not affect certification.",
	profile = "FAPI-RW-ID2",
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
public class FAPIRWID2EnsureClientAssertionWithIssAudSucceeds extends AbstractFAPIRWID2PerformTokenEndpoint {
	@Override
	protected void addClientAuthenticationToTokenEndpointRequest() {
		callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);
		callAndStopOnFailure(UpdateClientAuthenticationAssertionClaimsWithISSAud.class);
		callAndStopOnFailure(SignClientAuthenticationAssertion.class);
		callAndStopOnFailure(AddClientAssertionToTokenEndpointRequest.class);
	}

	@Override
	protected void processTokenEndpointResponse() {
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
			super.processTokenEndpointResponse();
		}
		fireTestFinished();
	}
}
