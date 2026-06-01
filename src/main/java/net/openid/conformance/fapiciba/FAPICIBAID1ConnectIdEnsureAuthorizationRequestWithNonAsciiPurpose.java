package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessage;
import net.openid.conformance.condition.client.SetRequestObjectBindingMessageToNonAsciiPurpose;
import net.openid.conformance.condition.client.WarnIfRequestObjectClaimsBindingMessageIsNotAscii;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-connectid-ensure-authorization-request-with-non-ascii-purpose",
	displayName = "ConnectID CIBA: Test with a non-ASCII binding_message purpose",
	summary = "This test sends a ConnectID CIBA backchannel authentication request with a non-ASCII binding_message purpose. The server may return invalid_binding_message; if it authenticates successfully, the test logs a warning because ConnectID purpose statements should be ASCII-only.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "openbanking_brazil"})
public class FAPICIBAID1ConnectIdEnsureAuthorizationRequestWithNonAsciiPurpose extends AbstractFAPICIBAID1 {

	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		callAndStopOnFailure(SetRequestObjectBindingMessageToNonAsciiPurpose.class, "CID-IDA-5.2-10");
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

			callAndContinueOnFailure(CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessage.class,
				Condition.ConditionResult.FAILURE, "CIBA-13", "CID-IDA-5.2-10");

			fireTestFinished();
		} else {
			callAndContinueOnFailure(WarnIfRequestObjectClaimsBindingMessageIsNotAscii.class,
				Condition.ConditionResult.WARNING, "CID-IDA-5.2-10");

			performValidateAuthorizationResponse();

			eventLog.endBlock();

			performPostAuthorizationResponse();
		}
	}
}
