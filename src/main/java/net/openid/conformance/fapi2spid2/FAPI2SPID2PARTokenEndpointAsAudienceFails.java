package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddTokenEndpointAsAudToClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaimsWithIssAudience;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400or401;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionWithIssAudAndAddToPAREndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-par-test-token-endpoint-url-as-audience-fails",
	displayName = "FAPI2-Security-Profile-ID2: ensure jwt client assertions with token endpoint as audience fails at the par endpoint",
	summary = "This test checks if PAR endpoint does not accept a client assertion with token endpoint as audience. We expect the request to fail and a server response with status code 400 or 401 with error codes invalid_request or invalid_client.",
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
@VariantNotApplicable(parameter = FAPI2ID2OPProfile.class, values = {
		"plain_fapi",
		"openbanking_uk",
		"consumerdataright_au",
		"openbanking_brazil",
		"connectid_au"
})
public class FAPI2SPID2PARTokenEndpointAsAudienceFails extends AbstractFAPI2SPID2ServerTestModule {
	@Override
	protected void addClientAuthenticationToPAREndpointRequest() {
			call(new CreateJWTClientAuthenticationAssertionWithIssAudAndAddToPAREndpointRequest().insertAfter(
					CreateClientAuthenticationAssertionClaimsWithIssAudience.class,
					condition(AddTokenEndpointAsAudToClientAuthenticationAssertionClaims.class).requirements("PAR-2")));
	}

	@Override
	protected void processParResponse() {
		env.mapKey("endpoint_response", CallPAREndpoint.RESPONSE_KEY);
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "CBUAE-2.-Security-Profile");
		callAndContinueOnFailure(CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest.class, Condition.ConditionResult.FAILURE, "CBUAE");
		env.unmapKey("endpoint_response");

		fireTestFinished();
	}
}
