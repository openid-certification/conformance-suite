package net.openid.conformance.apis.creditCard.v1;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditCard.v1.CreditCardBillValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;


@UseResurce("jsonResponses/creditCard/cardBill/cardBillResponse.json")
public class CreditCardBillValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		CreditCardBillValidator condition = new CreditCardBillValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardBill/cardBillResponseWithError.json")
	public void validateStructureWithMissingField() {
		CreditCardBillValidator condition = new CreditCardBillValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("currency",
			condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardBill/cardBillResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		CreditCardBillValidator condition = new CreditCardBillValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("type",
			condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardBill/cardBillResponseWrongRegexp.json")
	public void validateStructureWrongPattern() {
		CreditCardBillValidator condition = new CreditCardBillValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("billTotalAmountCurrency",
			condition.getApiName())));
	}
}
