package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.client.AddTokenEndpointAsAudToClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.UpdateClientAuthenticationAssertionClaimsWithISSAud;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantNotApplicable;


@PublishTestModule(
	testName = "fapi2-security-profile-id2-par-test-token-endpoint-url-as-audience-for-client-JWT-assertion",
	displayName = "PAR : try to use token endpoint url as audience for Client JWT Assertion",
	summary = "This test tries to use the token endpoint url as audience for Client JWT Assertion, the authorization server is expected to accept it",
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
		"cbuae"
})
public class FAPI2SPID2PARTokenEndpointAsAudienceForJWTClientAssertion extends AbstractFAPI2SPID2ServerTestModule {
	/*
	PAR-2.0
	Note that there's some potential ambiguity around the appropriate audience value to use when
	JWT client assertion based authentication is employed. To address that ambiguity the issuer
	identifier URL of the AS according to [RFC8414] SHOULD be used as the value of the audience.
	In order to facilitate interoperability the AS MUST accept its issuer identifier,
	token endpoint URL, or pushed authorization request endpoint URL as values that identify
	it as an intended audience.
	*/
	@Override
	protected void addClientAuthenticationToPAREndpointRequest() {
		call(new CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest().replace(
				UpdateClientAuthenticationAssertionClaimsWithISSAud.class,
			condition(AddTokenEndpointAsAudToClientAuthenticationAssertionClaims.class).requirement("PAR-2")));
	}
}
