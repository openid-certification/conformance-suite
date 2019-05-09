package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequestResponse;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatusNot200;
import io.fintechlabs.testframework.condition.client.CreateRandomClientNotificationToken;
import io.fintechlabs.testframework.condition.client.WaitForSuccessfulCibaAuthentication;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestModule;

@PublishTestModule(
	testName = "fapi-ciba-ping-user-rejects-authentication-with-mtls",
	displayName = "FAPI-CIBA: user rejects authentication in Ping mode (MTLS client authentication)",
	summary = "This test requires the user to reject the authentication on their device, for example by pressing the 'cancel' button on the login screen. It verifies the error is correctly notified back to the relying party.",
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
public class FAPICIBAPingUserRejectsAuthenticationWithMTLS extends AbstractFAPICIBAUserRejectAuthentication {

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		// for Ping mode:
		callAndStopOnFailure(WaitForSuccessfulCibaAuthentication.class);

		setStatus(TestModule.Status.WAITING);
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {

		processPingNotificationCallback(requestParts);

		callAndStopOnFailure(CheckTokenEndpointHttpStatusNot200.class);

		env.putObject("callback_params", env.getObject("token_endpoint_response"));
		verifyTokenEndpointResponseIsAccessDenied();
		fireTestFinished();
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		callAndStopOnFailure(CreateRandomClientNotificationToken.class, "CIBA-7.1");

		callAndStopOnFailure(AddClientNotificationTokenToAuthorizationEndpointRequestResponse.class, "CIBA-7.1");
	}
}
