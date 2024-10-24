package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClient;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.EnsureErrorTokenEndpointSlowdownOrAuthorizationPending;
import net.openid.conformance.condition.client.UpdateClientAuthenticationAssertionClaimsWithISSAud;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-client-assertion-with-iss-aud-to-token-endpoint-succeeds",
	displayName = "FAPI-CIBA-ID1: Ensure that client_assertion with AS issuer ID succeeds at the token endpoint",
	summary = "This test passes a client assertion where 'aud' is the Authorization Server's Issuer ID instead of the token endpoint. Per RFC7523-3 and Connect Core 1.0 - 3, the AS must verify that it is the intended audience, but only recommended a value. The AS should accept the AS Issuer ID as valid, but as a recommended value, the AS may reject it with a valid error response and the test will end with a WARNING, which will not affect certification.",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = { "mtls" })
public class FAPICIBAID1EnsureClientAssertionWithIssAudToTokenEndpointSucceeds extends AbstractFAPICIBAID1 {


	@Override
	protected void addClientAuthenticationToTokenEndpointRequest() {
		call(sequenceOf(sequence(addTokenEndpointClientAuthentication)).insertAfter(CreateClientAuthenticationAssertionClaims.class, condition(UpdateClientAuthenticationAssertionClaimsWithISSAud.class)));
	}

	@Override
	protected void verifyTokenEndpointResponseIsPendingOrSlowDown() {
		eventLog.startBlock(currentClientString() + "Verify token endpoint response is pending or slow_down");

		callAndContinueOnFailure(CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2", "CIBA-13");
		validateErrorFromTokenEndpointResponse();

		// expecting a slow_down or authorization_pending if client assertion is not rejected
		callAndContinueOnFailure(EnsureErrorTokenEndpointSlowdownOrAuthorizationPending.class, Condition.ConditionResult.WARNING);

		eventLog.endBlock();
	}

	@Override
	protected void performPostAuthorizationResponse() {
		eventLog.startBlock(currentClientString() + "Call token endpoint expecting pending");
		callTokenEndpointForCibaGrant();
		verifyTokenEndpointResponseIsPendingOrSlowDown();
		eventLog.endBlock();
		/* If AS does not reject the client authentication assertion,
		 * - It must be a 'invalid_client' error
		 */

		String error = env.getString("token_endpoint_response", "error");
		if (!(error.equals("slow_down") || error.equals("authorization_pending"))) {
			eventLog.startBlock(currentClientString() + "Verify token endpoint response is invalid_client");
			// we already checked this, this just makes it obvious in the log file that we checked it
			callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClient.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2", "CIBA-13");
			eventLog.endBlock();
		}
		cleanupAfterBackchannelRequestShouldHaveFailed();
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		// we've already done the testing; we just approved the authentication so that we don't leave an
		// in-progress authentication lying around that would sometime later send an 'expired' ping
		fireTestFinished();
	}
}
