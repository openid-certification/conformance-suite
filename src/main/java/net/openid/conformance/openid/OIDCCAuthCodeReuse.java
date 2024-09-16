package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.ServerAllowedReusingAuthorizationCode;
import net.openid.conformance.testmodule.PublishTestModule;
import org.apache.hc.core5.http.HttpStatus;

@PublishTestModule(
	testName = "oidcc-codereuse",
	displayName = "OIDCC: Authorization code reuse",
	summary = "This test tries using an authorization code for a second time, immediately after the first use. The server should return an invalid_grant error as the authorization code has already been used. If it doesn't, a warning is raised.",
	profile = "OIDCC"
)
// Equivalent of https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_OAuth_2nd
public class OIDCCAuthCodeReuse extends AbstractOIDCCAuthCodeReuse {

	@Override
	protected void checkResponse() {
		Integer httpStatus = env.getInteger("token_endpoint_response_http_status");
		if (httpStatus == HttpStatus.SC_OK) {
			callAndContinueOnFailure(ServerAllowedReusingAuthorizationCode.class, Condition.ConditionResult.WARNING);
		} else {
			super.checkResponse();
		}
	}

}
