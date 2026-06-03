package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.SetConnectIdCibaLoginHintToCardPrimaryAccountNumber;

public abstract class AbstractConnectIdCibaEnsureInvalid3DSPaymentAuthorizationDetailsFails
	extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	protected abstract Class<? extends Condition> getInvalidAuthorizationDetailsCondition();

	@Override
	protected void performProfileAuthorizationEndpointSetup() {
		super.performProfileAuthorizationEndpointSetup();
		callAndStopOnFailure(SetConnectIdCibaLoginHintToCardPrimaryAccountNumber.class,
			"CID-CIBA-4.1.3.1", "CID-CIBA-4.3-1");
	}

	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		callAndStopOnFailure(getInvalidAuthorizationDetailsCondition(), "CID-CIBA-4.1.3.1", "RAR-2");
	}
}
