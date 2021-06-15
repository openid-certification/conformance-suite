package net.openid.conformance.apis.creditCard;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditCard.InvoiceCreditCardTransactionsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditCard/invoiceCardTransactionsResponse.json")
public class InvoiceCreditCardTransactionsValidatorTest extends AbstractJsonResponseConditionUnitTest {
	@Test
	public void validateStructure() {
		InvoiceCreditCardTransactionsValidator condition = new InvoiceCreditCardTransactionsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditCard/invoiceCardTransactionsResponseWithError.json")
	public void validateStructureWithMissingField() {
		InvoiceCreditCardTransactionsValidator condition = new InvoiceCreditCardTransactionsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("$.data[0].payeeMCC")));
	}
}
