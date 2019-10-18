package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddAlgorithmAsRS256;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-request-object-signature-algorithm-is-RS256-fails",
	displayName = "FAPI-CIBA-ID1: Ensure request_object signature algorithm is RS256 fails",
	summary = "This test should end with the backchannel authorisation server returning an error message that the request is invalid.",
	profile = "FAPI-CIBA-ID1",
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
public class FAPICIBAID1EnsureRequestObjectSignatureAlgorithmIsRS256Fails extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorisationRequest {

	@Override
	protected void performAuthorizationRequest() {
		callAndContinueOnFailure(AddAlgorithmAsRS256.class, Condition.ConditionResult.FAILURE, "CIBA-7.2");

		super.performAuthorizationRequest();
	}
}
