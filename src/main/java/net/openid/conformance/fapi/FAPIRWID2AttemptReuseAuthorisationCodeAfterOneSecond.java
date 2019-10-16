package net.openid.conformance.fapi;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.ServerAllowedReusingAuthorisationCode;
import net.openid.conformance.condition.client.WaitForOneSecond;
import net.openid.conformance.testmodule.PublishTestModule;
import org.apache.http.HttpStatus;

@PublishTestModule(
	testName = "fapi-rw-id2-attempt-reuse-authorisation-code-after-one-second",
	displayName = "FAPI-RW-ID2: try to reuse authorisation code after one second",
	summary = "This test tries reusing an authorization code after one second and expects AS return an error",
	profile = "FAPI-RW-ID2",
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
		"resource.resourceUrl",
		"resource.institution_id"
	}
)
public class FAPIRWID2AttemptReuseAuthorisationCodeAfterOneSecond extends AbstractFAPIRWID2AttemptReuseAuthorisationCode {

	@Override
	protected void waitForAmountOfTime() {
		callAndStopOnFailure(WaitForOneSecond.class);
	}

	@Override
	protected void verifyError() {
		Integer httpStatus = env.getInteger("token_endpoint_response_http_status");
		if (httpStatus == HttpStatus.SC_OK) {
			callAndContinueOnFailure(ServerAllowedReusingAuthorisationCode.class, Condition.ConditionResult.WARNING);
		} else {
			super.verifyError();
		}
	}
}
