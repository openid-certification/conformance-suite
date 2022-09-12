package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.paymentInitiation.EnsureEndToEndIdIsEqual;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentFetchPixPaymentsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class PollForAcceptedPaymentSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		call(new ValidateSelfEndpoint()
			.insertAfter(
				EnsureResponseCodeWas200.class, sequenceOf(
					condition(EnsureResponseWasJwt.class),
					condition(PaymentFetchPixPaymentsValidator.class),
					condition(EnsureEndToEndIdIsEqual.class)
				)
			)
			.insertBefore(CallProtectedResource.class, sequenceOf(
				condition(AddJWTAcceptHeader.class)
			))
			.insertAfter(ValidateResponseMetaData.class, sequenceOf(
				condition(CheckPaymentAccepted.class)
			))
			.skip(SaveOldValues.class,
				"Not saving old values")
			.skip(LoadOldValues.class,
				"Not loading old values")
		);
	}
}
