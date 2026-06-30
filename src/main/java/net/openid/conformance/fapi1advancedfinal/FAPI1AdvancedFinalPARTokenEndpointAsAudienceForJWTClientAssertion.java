package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.client.AddTokenEndpointAsAudToClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.UpdateClientAuthenticationAssertionClaimsWithISSAud;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-par-token-endpoint-url-as-audience-for-client-JWT-assertion",
	displayName = "PAR: try to use token endpoint URL as audience for Client JWT Assertion",
	summary = "This test uses the token endpoint URL as the audience for the Client JWT Assertion sent to the PAR endpoint. Per RFC 9126 section 2 the authorization server MUST accept its issuer identifier, token endpoint URL, or pushed authorization request endpoint URL as values that identify it as an intended audience, so this test must succeed.",
	profile = "FAPI1-Advanced-Final",
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
@VariantNotApplicable(parameter = FAPIAuthRequestMethod.class, values = {
	"by_value"
})
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"mtls"
})
public class FAPI1AdvancedFinalPARTokenEndpointAsAudienceForJWTClientAssertion extends AbstractFAPI1AdvancedFinalServerTestModule {

	@Override
	protected void addClientAuthenticationToPAREndpointRequest() {
		mapClientAuthKeys("pushed_authorization_request_form_parameters", "pushed_authorization_request_endpoint_request_headers");
		call(new CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest().replace(
			UpdateClientAuthenticationAssertionClaimsWithISSAud.class,
			condition(AddTokenEndpointAsAudToClientAuthenticationAssertionClaims.class).requirement("PAR-2")));
		unmapClientAuthKeys();
	}
}
