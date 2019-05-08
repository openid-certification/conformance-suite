package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.as.CheckAuthReqIdInCallback;
import io.fintechlabs.testframework.condition.as.CheckNotificationCallbackOnlyAuthReqId;
import io.fintechlabs.testframework.condition.as.VerifyBearerTokenHeaderCallback;
import io.fintechlabs.testframework.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequestResponse;
import io.fintechlabs.testframework.condition.client.CreateRandomClientNotificationToken;
import io.fintechlabs.testframework.condition.common.EnsureIncomingTls12;
import io.fintechlabs.testframework.condition.common.EnsureIncomingTlsSecureCipher;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-ping-ensure-request-object-signed-by-other-client-fails-with-mtls",
	displayName = "FAPI-CIBA: Ping mode ensure request_object signed by other client fails (MTLS client authentication)",
	summary = "This test should end with the backchannel authorisation server returning an error message that the request is invalid.",
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
public class FAPICIBAPingEnsureRequestObjectSignedByOtherClientFailsWithMTLS extends AbstractFAPICIBAEnsureRequestObjectSignedByOtherClientFailsWithMTLS {
	@Override
	protected void processNotificationCallback(JsonObject requestParts) {

		String envKey = "notification_callback";

		eventLog.startBlock(currentClientString() + "Verify notification callback");

		env.putObject(envKey, requestParts);

		env.mapKey("client_request", envKey);

		callAndContinueOnFailure(EnsureIncomingTls12.class, "FAPI-R-7.1-1");
		callAndContinueOnFailure(EnsureIncomingTlsSecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-R-7.1-1");

		env.unmapKey("client_request");

		callAndStopOnFailure(VerifyBearerTokenHeaderCallback.class, "CIBA-10.2");

		callAndStopOnFailure(CheckAuthReqIdInCallback.class, Condition.ConditionResult.FAILURE, "CIBA-10.2");

		callAndStopOnFailure(CheckNotificationCallbackOnlyAuthReqId.class, "CIBA-10.2");
		eventLog.endBlock();

		eventLog.startBlock(currentClientString() + "Calling token endpoint after ping notification");
		callTokenEndpointForCibaGrant();
		eventLog.endBlock();
		handleSuccessfulTokenEndpointResponse();
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		callAndStopOnFailure(CreateRandomClientNotificationToken.class, "CIBA-7.1");

		callAndStopOnFailure(AddClientNotificationTokenToAuthorizationEndpointRequestResponse.class, "CIBA-7.1");
	}
}
