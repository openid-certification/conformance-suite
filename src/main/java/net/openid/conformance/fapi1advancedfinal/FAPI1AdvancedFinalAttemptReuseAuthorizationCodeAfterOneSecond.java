package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.ServerAllowedReusingAuthorizationCode;
import net.openid.conformance.condition.client.WaitForOneSecond;
import net.openid.conformance.testmodule.PublishTestModule;
import org.apache.hc.core5.http.HttpStatus;

@PublishTestModule(
	testName = "fapi1-advanced-final-attempt-reuse-authorisation-code-after-one-second",
	displayName = "FAPI1-Advanced-Final: try to reuse authorization code after one second",
	summary = "This test tries reusing an authorization code after one second, as the authorization code has already been used this must fail with the AS returning an invalid_grant error.\n\nAny issued access token 'should' also be revoked as per RFC6749 section 4.1.2 (although this is only recommended behaviour and this warning won't prevent you certifying) - see https://bitbucket.org/openid/fapi/issues/397/query-over-certification-test-for-access",
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
			callAndContinueOnFailure(ServerAllowedReusingAuthorizationCode.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-5.2.2-13");
		} else {
			super.verifyError();
		}
	}
}
