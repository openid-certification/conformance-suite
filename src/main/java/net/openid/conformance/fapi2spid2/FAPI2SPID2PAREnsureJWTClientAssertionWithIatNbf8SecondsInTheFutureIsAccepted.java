package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddIatNbf8SecondsInTheFutureToClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CheckPAREndpointResponse201WithNoError;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaimsWithIssAudience;
import net.openid.conformance.condition.client.UpdateClientAuthenticationAssertionClaimsWithISSAud;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionWithIssAudAndAddToPAREndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpStatus;

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
		if (getVariant(FAPI2ID2OPProfile.class) == FAPI2ID2OPProfile.CBUAE){
			call(new CreateJWTClientAuthenticationAssertionWithIssAudAndAddToPAREndpointRequest().insertAfter(
					CreateClientAuthenticationAssertionClaimsWithIssAudience.class,
					condition(AddIatNbf8SecondsInTheFutureToClientAuthenticationAssertionClaims.class).requirements("PAR-2", "RFC7519-4.1.5", "RFC7519-4.1.6")));
		} else {
			call(new CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest().insertAfter(
					UpdateClientAuthenticationAssertionClaimsWithISSAud.class,
					condition(AddIatNbf8SecondsInTheFutureToClientAuthenticationAssertionClaims.class).requirements("PAR-2", "RFC7519-4.1.5", "RFC7519-4.1.6")));
		}
	}

	@Override
	protected void processParResponse() {

		// if response code is not 201 then skip test
		Integer status = env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status");
		if (status != HttpStatus.CREATED.value()) {
			callAndContinueOnFailure(CheckPAREndpointResponse201WithNoError.class, Condition.ConditionResult.WARNING,"PAR-2.2", "PAR-2.3");
			eventLog.log(executionManager.getTestId(), "PAR endpoint doesn't seem to support clock skew, finishing test prematurely.");
			fireTestFinished();
			return;
		}

		super.processParResponse();
	}
}
