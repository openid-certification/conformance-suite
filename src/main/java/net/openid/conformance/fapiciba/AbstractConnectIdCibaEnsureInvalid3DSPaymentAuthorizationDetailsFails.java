package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequestOrInvalidAuthorizationDetails;
import net.openid.conformance.condition.client.SetConnectIdCibaLoginHintToCardPrimaryAccountNumber;

public abstract class AbstractConnectIdCibaEnsureInvalid3DSPaymentAuthorizationDetailsFails
	extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	protected abstract Class<? extends Condition> getInvalidAuthorizationDetailsCondition();

	@Override
	protected void performProfileAuthorizationEndpointSetup() {
		callAndStopOnFailure(SetConnectIdCibaLoginHintToCardPrimaryAccountNumber.class,
			"CID-CIBA-4.1.3.1", "CID-CIBA-4.3-1");
		call(new ConnectIdAuCibaServerProfileBehavior.CommonAuthorizationEndpointSetupSteps());
	}

	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		callAndStopOnFailure(getInvalidAuthorizationDetailsCondition(), "CID-CIBA-4.1.3.1", "RFC9396-2");
	}

	@Override
	protected void checkErrorFromBackchannelAuthorizationRequestResponse() {
		callAndContinueOnFailure(CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequestOrInvalidAuthorizationDetails.class,
			Condition.ConditionResult.FAILURE, "CIBA-13", "RFC9396-5");
	}
}
