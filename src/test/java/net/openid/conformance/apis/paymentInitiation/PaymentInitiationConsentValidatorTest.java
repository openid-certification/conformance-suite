package net.openid.conformance.apis.paymentInitiation;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentInitiationConsentValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class PaymentInitiationConsentValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/paymentInitiation/consent/paymentInitiationConsentResponseOK.json")
	public void validateStructure() {
		PaymentInitiationConsentValidator condition = new PaymentInitiationConsentValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/paymentInitiation/consent/paymentInitiationConsentResponse(WithoutOptional).json")
	public void validateStructureWithoutOptional() {
		PaymentInitiationConsentValidator condition = new PaymentInitiationConsentValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/paymentInitiation/consent/paymentInitiationConsentResponse(WrongBusinessEntity).json")
	public void validateStructureWrongBusinessEntity() {
		PaymentInitiationConsentValidator condition = new PaymentInitiationConsentValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("rel")));
	}

	@Test
	@UseResurce("jsonResponses/paymentInitiation/consent/paymentInitiationConsentResponse(WrongEnum).json")
	public void validateStructureWithWrongEnum() {
		PaymentInitiationConsentValidator condition = new PaymentInitiationConsentValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("type")));
	}

	@Test
	@UseResurce("jsonResponses/paymentInitiation/consent/paymentInitiationConsentResponse(WithMissingField).json")
	public void validateStructureWithMissField() {
		PaymentInitiationConsentValidator condition = new PaymentInitiationConsentValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("cpfCnpj")));
	}

	@Test
	@UseResurce("jsonResponses/paymentInitiation/consent/paymentInitiationConsentResponse(WrongRegexp).json")
	public void validateStructureWithWrongRegexp() {
		PaymentInitiationConsentValidator condition = new PaymentInitiationConsentValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("creationDateTime")));
	}
}
