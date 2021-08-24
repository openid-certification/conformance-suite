package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-wrong-auth-req-id-in-token-endpoint-request",
	displayName = "FAPI-CIBA-ID1: Ensure wrong auth_req_id in token endpoint request",
	summary = "This test uses an auth_req_id issued to client 1 at the token endpoint, but authenticates as client 2. The token endpoint server must return an 'invalid_grant' error.",
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
public class FAPICIBAID1EnsureWrongAuthenticationRequestIdInTokenEndpointRequest extends AbstractFAPICIBAID1 {

	@Override
	protected void configClient() {
		setupClient1();

		setupClient2();
	}

	@Override
	protected void performPostAuthorizationResponse() {

		eventLog.startBlock(currentClientString() + "Swapping to Client2, Jwks2, tls2 and calling token endpoint");
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");

		callTokenEndpointForCibaGrant();

		env.unmapKey("mutual_tls_authentication");
		env.mapKey("client_jwks", "client_jwks2");
		env.unmapKey("client");
		eventLog.endBlock();

		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndStopOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
		callAndStopOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");
		callAndStopOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2", "CIBA-11");
		callAndStopOnFailure(CheckTokenEndpointHttpStatus400.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");

		cleanupAfterBackchannelRequestShouldHaveFailed();
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		// we've already done the testing; we just approved the authentication so that we don't leave an
		// in-progress authentication lying around that would sometime later send an 'expired' ping
		fireTestFinished();
	}

	@Override
	public void cleanup() {
		unregisterClient1();

		unregisterClient2();
	}
}
