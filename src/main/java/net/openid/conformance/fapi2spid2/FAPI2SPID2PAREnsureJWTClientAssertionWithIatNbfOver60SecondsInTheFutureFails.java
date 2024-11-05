package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddIatNbfOver60SecondsInTheFutureToClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.CheckPAREndpointResponse401WithInvalidClientError;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-par-ensure-jwt-client-assertions-nbf-over-60-seconds-in-the-future-fails",
	displayName = "FAPI2-Security-Profile-ID2: ensure jwt client assertions with nbf over 60 seconds in the future fails at the par endopint",
	summary = "This test makes a PAR request with a client assertion 'nbf' of >60 seconds in the future. This must be rejected by the authentication server as per 'https://bitbucket.org/openid/fapi/pull-requests/497/diff'",
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
		call(new CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest().insertAfter(
			CreateClientAuthenticationAssertionClaims.class,
			condition(AddIatNbfOver60SecondsInTheFutureToClientAuthenticationAssertionClaims.class).requirements("PAR-2", "RFC7519-4.1.5", "RFC7519-4.1.6")));
	}

	@Override
	protected void processParResponse() {
		callAndContinueOnFailure(CheckPAREndpointResponse401WithInvalidClientError.class, Condition.ConditionResult.FAILURE, "PAR-2.3", "RFC6749-4.1.2.1");

		fireTestFinished();
	}
}
