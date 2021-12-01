package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerTokenAndCustomHeaders;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.EnsureConsentResponseCodeWas201;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.PaymentsProxyCheckForAcceptedStatus;
import net.openid.conformance.sequence.ConditionSequence;

public abstract class AbstractOBBrasilQrCodePaymentFunctionalTestModule extends AbstractDictVerifiedPaymentTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		eventLog.startBlock("Setting date to today");
		env.putBoolean("consent_rejected", false);
		env.putString("proceed_with_test", "proceed");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
		callAndStopOnFailure(RemoveTransactionIdentification.class);
		configureDictInfo();
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		eventLog.log(getName(), "Payments scope present - protected resource assumed to be a payments endpoint");
		ConditionSequence steps = new OpenBankingBrazilPreAuthorizationErrorAgnosticSteps(addTokenEndpointClientAuthentication)
			.replace(OptionallyAllow201Or422.class, condition(EnsureConsentResponseCodeWas201.class));
		return steps;
	}

	@Override
	protected void requestProtectedResource() {
		if(!validationStarted) {
			validationStarted = true;
			ConditionSequence pixSequence = new CallPixPaymentsEndpointSequence();
			postProcessResourceSequence(pixSequence);
			errorMessageCondition().ifPresent(c -> {
				pixSequence.insertAfter(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, condition(c));
			});
			call(pixSequence);
			eventLog.startBlock(currentClientString() + "Validate response");
			validateResponse();
			eventLog.endBlock();
		}
	}

	protected void postProcessResourceSequence(ConditionSequence pixSequence) {
		// NOOP
	}

	@Override
	protected ConditionSequence statusValidationSequence() {
		return sequenceOf(
			condition(PaymentsProxyCheckForAcceptedStatus.class),
			condition(PaymentsProxyCheckForInvalidStatus.class));
	}
}
