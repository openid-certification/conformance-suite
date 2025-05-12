package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.ServerAllowedExpiredAuthorizationCode;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.WaitFor62Seconds;
import net.openid.conformance.testmodule.PublishTestModule;

import org.apache.hc.core5.http.HttpStatus;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-ensure-token-endpoint-fails-with-expired-auth-code",
	displayName = "FAPI2-Security-Profile-ID2: try to use authorization code after its lifetime has expired",
	summary = "This test tries using an authorization code, for the first time, after its lifetime of 60 seconds has expired, This must fail with the AS returning an invalid_grant error.",
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

public class FAPI2SPID2AttemptToUseExpiredAuthCode extends AbstractFAPI2SPID2PerformTokenEndpoint {

	@Override
	protected void performPostAuthorizationFlow() {
		// Wait 60s + 2s clock skew before proceeding to the token endpoint.
		callAndStopOnFailure(WaitFor62Seconds.class);

		super.performPostAuthorizationFlow();
	}

	@Override
	protected void exchangeAuthorizationCode() {
		callSenderConstrainedTokenEndpoint();

		eventLog.startBlock(currentClientString() + "Verify token endpoint response");
		processTokenEndpointResponse();
		eventLog.endBlock();
	}

	@Override
	protected void processTokenEndpointResponse() {
		Integer httpStatus = env.getInteger("token_endpoint_response_http_status");
		if (httpStatus == HttpStatus.SC_OK) {
			callAndContinueOnFailure(ServerAllowedExpiredAuthorizationCode.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.3.1.1-11");
		} else {
			callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
			callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
			callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		}
		fireTestFinished();
	}
}
