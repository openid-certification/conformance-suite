package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.PaymentsProxyCheckForAcceptedStatus;
import net.openid.conformance.sequence.ConditionSequence;

public abstract class AbstractOBBrasilQrCodePaymentFunctionalTestModule extends AbstractDictVerifiedPaymentTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
		callAndStopOnFailure(RemoveTransactionIdentification.class);
	}

	@Override
	protected ConditionSequence statusValidationSequence() {
		return sequenceOf(
			condition(PaymentsProxyCheckForAcceptedStatus.class),
			condition(PaymentsProxyCheckForInvalidStatus.class));
	}
}
