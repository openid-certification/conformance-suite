package net.openid.conformance.apis.paymentInitiation;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
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
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("rel", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/paymentInitiation/consent/paymentInitiationConsentResponse(WrongEnum).json")
	public void validateStructureWithWrongEnum() {
		PaymentInitiationConsentValidator condition = new PaymentInitiationConsentValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("type", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/paymentInitiation/consent/paymentInitiationConsentResponse(WithMissingField).json")
	public void validateStructureWithMissField() {
		PaymentInitiationConsentValidator condition = new PaymentInitiationConsentValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("cpfCnpj", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/paymentInitiation/consent/paymentInitiationConsentResponse(WrongRegexp).json")
	public void validateStructureWithWrongRegexp() {
		PaymentInitiationConsentValidator condition = new PaymentInitiationConsentValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("creationDateTime", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/paymentInitiation/consent/paymentInitiationConsentResponse(WrongExpirationTimeTooOld).json")
	public void validateStructureWithExpirationOld() {
		PaymentInitiationConsentValidator condition = new PaymentInitiationConsentValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldIsntInSecondsRange("expirationDateTime", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/paymentInitiation/consent/paymentInitiationConsentResponse(WrongExpirationTimeTooYoung).json")
	public void validateStructureWithExpirationYoung() {
		PaymentInitiationConsentValidator condition = new PaymentInitiationConsentValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldIsntInSecondsRange("expirationDateTime", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/paymentInitiation/consent/paymentInitiationConsentResponseNoDetails.json")
	public void validateStructureWithMissingDetails() {
		PaymentInitiationConsentValidator condition = new PaymentInitiationConsentValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("details", condition.getApiName())));
	}

}
