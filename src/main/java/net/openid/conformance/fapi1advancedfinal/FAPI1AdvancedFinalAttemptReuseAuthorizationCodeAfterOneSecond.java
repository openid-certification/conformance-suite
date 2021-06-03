package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.ServerAllowedReusingAuthorizationCode;
import net.openid.conformance.condition.client.WaitForOneSecond;
import net.openid.conformance.testmodule.PublishTestModule;
import org.apache.http.HttpStatus;

@PublishTestModule(
	testName = "fapi1-advanced-final-attempt-reuse-authorisation-code-after-one-second",
	displayName = "FAPI1-Advanced-Final: try to reuse authorization code after one second",
	summary = "This test tries reusing an authorization code after one second, as the authorization code has already been used this should fail with the AS returning an invalid_grant error. If the AS does not do this a warning (not a failure) is issued - a warning will not prevent certification.",
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
public class FAPI1AdvancedFinalAttemptReuseAuthorizationCodeAfterOneSecond extends AbstractFAPI1AdvancedFinalAttemptReuseAuthorizationCode {

	@Override
	protected void waitForAmountOfTime() {
		callAndStopOnFailure(WaitForOneSecond.class);
	}

	@Override
	protected void verifyError() {
		Integer httpStatus = env.getInteger("token_endpoint_response_http_status");
		if (httpStatus == HttpStatus.SC_OK) {
			callAndContinueOnFailure(ServerAllowedReusingAuthorizationCode.class, Condition.ConditionResult.FAILURE, "FAPI1-BASELINE-5.2.2-13");
		} else {
			super.verifyError();
		}
	}
}
