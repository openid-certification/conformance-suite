package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.client.AddArrayContainingIssuerAndAnotherValueAsAudToClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.UpdateClientAuthenticationAssertionClaimsWithISSAud;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantNotApplicable;


@PublishTestModule(
	testName = "fapi2-security-profile-id2-test-array-as-audience-for-client-JWT-assertion",
	displayName = "Try to use an array containing the issuer and another value as the audience for Client JWT Assertions at the PAR and token endpoints",
	summary = "This test sets the audience in client assertion claims at the PAR and token endpoints to be an array containing the authorization server issuer and another dummy value. The authorization server is expected to accept both requests as per discussion at https://gitlab.com/openid/conformance-suite/-/issues/1187 - in particular as per RFC7523 section 3 the aud value is only required to contain a value that identifies the authorization server as an intended audience.",
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
public class FAPI2SPID2PARArrayAsAudienceForJWTClientAssertion extends AbstractFAPI2SPID2ServerTestModule {
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
			condition(AddArrayContainingIssuerAndAnotherValueAsAudToClientAuthenticationAssertionClaims.class).requirements("PAR-2", "RFC7519-4.1.3")));
	}

	@Override
	protected void addClientAuthenticationToTokenEndpointRequest() {
		call(new CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest().insertAfter(
			CreateClientAuthenticationAssertionClaims.class,
			condition(AddArrayContainingIssuerAndAnotherValueAsAudToClientAuthenticationAssertionClaims.class).requirements("RFC7519-4.1.3")));
	}
}
