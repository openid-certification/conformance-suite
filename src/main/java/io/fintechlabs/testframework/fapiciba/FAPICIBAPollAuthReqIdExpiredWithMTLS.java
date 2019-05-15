package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.client.AddRequestedExpToAuthorizationEndpointRequestResponse;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatusNot200;
import io.fintechlabs.testframework.condition.client.SleepUntilAuthReqExpires;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-poll-auth-req-id-expired-with-mtls",
	displayName = "FAPI-CIBA: Poll mode (MTLS client authentication)",
	summary = "This test should end with the token endpoint server showing an error message that the auth_req_id token expired.",
	profile = "FAPI-CIBA",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
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
public class FAPICIBAPollAuthReqIdExpiredWithMTLS extends AbstractFAPICIBAWithMTLS {

	@Override
	protected void callAutomatedEndpoint() {
		// Override behavior. Don't need to call automated endpoint. User doesn't try to authenticate
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		setStatus(Status.WAITING);
		callAndStopOnFailure(SleepUntilAuthReqExpires.class);
		setStatus(Status.RUNNING);

		eventLog.startBlock(currentClientString() + "Calling token endpoint expecting a token expired error");
		callTokenEndpointForCibaGrant();
		eventLog.endBlock();

		callAndStopOnFailure(CheckTokenEndpointHttpStatusNot200.class);

		verifyTokenEndpointResponseIsTokenExpired();
		fireTestFinished();
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		fireTestFailure();
		throw new ConditionError(getId(), "Notification endpoint was called during a poll test");
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		env.getObject("client").addProperty("requested_expiry", 30);
		callAndStopOnFailure(AddRequestedExpToAuthorizationEndpointRequestResponse.class, "CIBA-11");
	}
}
