package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddIatNbfExpOver60SecondsInTheFutureToClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaimsWithIssAudience;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400or401;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionWithIssAudAndAddToPAREndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-par-ensure-jwt-client-assertions-nbf-over-60-seconds-in-the-future-fails",
	displayName = "FAPI2-Security-Profile-ID2: ensure jwt client assertions with nbf over 60 seconds in the future fails at the par endopint",
	summary = "This test checks the clock skew handling of the PAR endpoint as per https://openid.bitbucket.io/fapi/fapi-2_0-security-profile.html#section-5.3.2.1-2.14. The test makes a PAR request with a client assertion with iat, nbf and exp set > 60s into the future. We expect the request to fail and a server response with status code 400 or 401 with error codes invalid_request or invalid_client.",
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
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"mtls"
})
public class FAPI2SPID2PAREnsureJWTClientAssertionWithIatNbfOver60SecondsInTheFutureFails extends AbstractFAPI2SPID2ServerTestModule {
	@Override
	protected void addClientAuthenticationToPAREndpointRequest() {
		if (getVariant(FAPI2ID2OPProfile.class) == FAPI2ID2OPProfile.CBUAE){
			call(new CreateJWTClientAuthenticationAssertionWithIssAudAndAddToPAREndpointRequest().insertAfter(
					CreateClientAuthenticationAssertionClaimsWithIssAudience.class,
					condition(AddIatNbfExpOver60SecondsInTheFutureToClientAuthenticationAssertionClaims.class).requirements("PAR-2", "RFC7519-4.1.5", "RFC7519-4.1.6")));
		} else {
			call(new CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest().insertAfter(
					CreateClientAuthenticationAssertionClaims.class,
					condition(AddIatNbfExpOver60SecondsInTheFutureToClientAuthenticationAssertionClaims.class).requirements("PAR-2", "RFC7519-4.1.5", "RFC7519-4.1.6")));
		}
	}

	@Override
	protected void processParResponse() {

		env.mapKey("endpoint_response", CallPAREndpoint.RESPONSE_KEY);
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "PAR-2.3", "RFC6749-4.1.2.1", "RFC6749-5.2");
		callAndContinueOnFailure(CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest.class, Condition.ConditionResult.FAILURE, "PAR-2.3", "RFC6749-4.1.2.1", "RFC6749-5.2");
		env.unmapKey("endpoint_response");

		fireTestFinished();
	}
}
