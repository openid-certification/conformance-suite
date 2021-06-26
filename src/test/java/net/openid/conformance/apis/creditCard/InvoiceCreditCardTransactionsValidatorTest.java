package net.openid.conformance.apis.creditCard;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditCard.InvoiceCreditCardTransactionsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditCard/invoiceCardTransaction/invoiceCardTransactionsResponse.json")
public class InvoiceCreditCardTransactionsValidatorTest extends AbstractJsonResponseConditionUnitTest {
	@Test
	public void validateStructure() {
		InvoiceCreditCardTransactionsValidator condition = new InvoiceCreditCardTransactionsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditCard/invoiceCardTransaction/invoiceCardTransactionsResponseWithError.json")
	public void validateStructureWithMissingField() {
		InvoiceCreditCardTransactionsValidator condition = new InvoiceCreditCardTransactionsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("payeeMCC")));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/invoiceCardTransaction/invoiceCardTransactionsResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		InvoiceCreditCardTransactionsValidator condition = new InvoiceCreditCardTransactionsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("lineName")));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/invoiceCardTransaction/invoiceCardTransactionsResponseWrongMaxLength.json")
	public void validateStructureWrongMaxLength() {
		InvoiceCreditCardTransactionsValidator condition = new InvoiceCreditCardTransactionsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaxLengthMessage("payeeMCC")));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/invoiceCardTransaction/invoiceCardTransactionsResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		InvoiceCreditCardTransactionsValidator condition = new InvoiceCreditCardTransactionsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("billId")));
	}
}
