package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.client.AddIatNbf8SecondsInTheFutureToClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaims;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-par-ensure-jwt-client-assertions-nbf-8-seconds-in-the-future-is-accepted",
	displayName = "FAPI2-Security-Profile-ID2: ensure jwt client assertions with nbf 8 seconds in the future is accepted at the par endpoint",
	summary = "This test makes a PAR request with a client assertion 'nbf' of 8 seconds in the future. This must be accepted by the authentication server as per 'https://bitbucket.org/openid/fapi/pull-requests/497/diff'",
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
public class FAPI2SPID2PAREnsureJWTClientAssertionWithIatNbf8SecondsInTheFutureIsAccepted extends AbstractFAPI2SPID2ServerTestModule {
	@Override
	protected void addClientAuthenticationToPAREndpointRequest() {
		call(new CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest().insertAfter(
			CreateClientAuthenticationAssertionClaims.class,
			condition(AddIatNbf8SecondsInTheFutureToClientAuthenticationAssertionClaims.class).requirements("PAR-2", "RFC7519-4.1.5",  "RFC7519-4.1.6")));
	}
}
