package net.openid.conformance.apis.paymentInitiation;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentInitiationPixPaymentsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class PaymentInitiationPixPaymentsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/paymentInitiation/pixByPayments/paymentInitiationConsentResponsePixByPaymentIdOK.json")
	public void validateStructure() {
		PaymentInitiationPixPaymentsValidator condition = new PaymentInitiationPixPaymentsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/paymentInitiation/pixByPayments/paymentInitiationConsentResponsePixByPaymentId(WithoutOptional).json")
	public void validateStructureWithoutOptional() {
		PaymentInitiationPixPaymentsValidator condition = new PaymentInitiationPixPaymentsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/paymentInitiation/pixByPayments/paymentInitiationConsentResponsePixByPaymentId(WrongEnum).json")
	public void validateStructureWithWrongEnum() {
		PaymentInitiationPixPaymentsValidator condition = new PaymentInitiationPixPaymentsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("rejectionReason")));
	}

	@Test
	@UseResurce("jsonResponses/paymentInitiation/pixByPayments/paymentInitiationConsentResponsePixByPaymentId(MissField).json")
	public void validateStructureWithMissField() {
		PaymentInitiationPixPaymentsValidator condition = new PaymentInitiationPixPaymentsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("statusUpdateDateTime")));
	}

	@Test
	@UseResurce("jsonResponses/paymentInitiation/pixByPayments/paymentInitiationConsentResponsePixByPaymentId(WrongRegexp).json")
	public void validateStructureWithWrongRegexp() {
		PaymentInitiationPixPaymentsValidator condition = new PaymentInitiationPixPaymentsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("ispb")));
	}

}
