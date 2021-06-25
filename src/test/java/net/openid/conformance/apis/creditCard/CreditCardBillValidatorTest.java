package net.openid.conformance.apis.creditCard;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditCard.CreditCardBillValidator;
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
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("currency")));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardBill/cardBillResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		CreditCardBillValidator condition = new CreditCardBillValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("type")));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardBill/cardBillResponseWrongRegexp.json")
	public void validateStructureWrongPattern() {
		CreditCardBillValidator condition = new CreditCardBillValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("billTotalAmountCurrency")));
	}
}
