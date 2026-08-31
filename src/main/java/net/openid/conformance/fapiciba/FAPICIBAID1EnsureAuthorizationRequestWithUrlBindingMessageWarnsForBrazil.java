package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessageOrInvalidRequest;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestBindingMessageToUrl;
import net.openid.conformance.condition.client.WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-authorization-request-with-url-binding-message-warns-for-brazil",
	displayName = "FAPI-CIBA-ID1: Brazil CIBA request with URL binding_message should warn if accepted",
	summary = "This test sends a Brazil CIBA backchannel authentication request containing a URL in binding_message. The server may reject the request with invalid_binding_message or invalid_request; if it accepts the request, the test logs a warning because Open Finance Brasil CIBA says binding_message must not contain URLs.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "connectid_au"})
public class FAPICIBAID1EnsureAuthorizationRequestWithUrlBindingMessageWarnsForBrazil extends AbstractFAPICIBAID1 {

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		callAndStopOnFailure(SetAuthorizationEndpointRequestBindingMessageToUrl.class, "BrazilCIBA-6.2.5");
	}

	@Override
	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		eventLog.startBlock(currentClientString() + "Call backchannel authentication endpoint");

		createAuthorizationRequest();

		performAuthorizationRequest();

		JsonObject callbackParams = env.getObject("backchannel_authentication_endpoint_response");

		if (callbackParams != null && callbackParams.has("error")) {
			validateErrorFromBackchannelAuthorizationRequestResponse();

			callAndContinueOnFailure(CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessageOrInvalidRequest.class,
				Condition.ConditionResult.FAILURE, "BrazilCIBA-6.2.5", "CIBA-13");

			eventLog.endBlock();
			fireTestFinished();
		} else {
			performValidateAuthorizationResponse();

			callAndContinueOnFailure(WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl.class,
				Condition.ConditionResult.WARNING, "BrazilCIBA-6.2.5");

			eventLog.endBlock();

			performPostAuthorizationResponse();
		}
	}
}
