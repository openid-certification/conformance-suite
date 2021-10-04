package net.openid.conformance.apis.creditCard;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditCard.CreditCardAccountsTransactionBillResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditCard/invoiceCardTransaction/invoiceCardTransactionsResponse.json")
public class CreditCardAccountsTransactionBillResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		CreditCardAccountsTransactionBillResponseValidator condition = new CreditCardAccountsTransactionBillResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditCard/invoiceCardTransaction/invoiceCardTransactionsResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		CreditCardAccountsTransactionBillResponseValidator condition = new CreditCardAccountsTransactionBillResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("lineName")));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/invoiceCardTransaction/invoiceCardTransactionsResponseWrongMaxLength.json")
	public void validateStructureWrongMaxLength() {
		CreditCardAccountsTransactionBillResponseValidator condition = new CreditCardAccountsTransactionBillResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaxLengthMessage("payeeMCC")));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/invoiceCardTransaction/invoiceCardTransactionsResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		CreditCardAccountsTransactionBillResponseValidator condition = new CreditCardAccountsTransactionBillResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("billId")));
	}

}
