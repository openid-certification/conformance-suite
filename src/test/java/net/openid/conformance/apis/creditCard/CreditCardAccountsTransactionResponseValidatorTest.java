package net.openid.conformance.apis.creditCard;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditCard.CreditCardAccountsTransactionResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditCard/cardTransactions/cardTransactionsResponse.json")
public class CreditCardAccountsTransactionResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		CreditCardAccountsTransactionResponseValidator condition = new CreditCardAccountsTransactionResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardTransactions/cardTransactionsResponse_with_missed_identificationNumber_field.json")
	public void validateStructureWithMissingField() {
		CreditCardAccountsTransactionResponseValidator condition = new CreditCardAccountsTransactionResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("identificationNumber")));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardTransactions/cardTransactionsResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		CreditCardAccountsTransactionResponseValidator condition = new CreditCardAccountsTransactionResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("lineName")));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardTransactions/cardTransactionsResponseWrongMaxLength.json")
	public void validateStructureWrongMaxLength() {
		CreditCardAccountsTransactionResponseValidator condition = new CreditCardAccountsTransactionResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaxLengthMessage("payeeMCC")));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardTransactions/cardTransactionsResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		CreditCardAccountsTransactionResponseValidator condition = new CreditCardAccountsTransactionResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("billId")));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardTransactions/cardTransactionsResponseNullPayeeMCC.json")
	public void payeeMCCIsNullable() {
		CreditCardAccountsTransactionResponseValidator condition = new CreditCardAccountsTransactionResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardTransactions/cardTransactionsResponseEmpty.json")
	public void validateStructureEmpty() {
		CreditCardAccountsTransactionResponseValidator condition = new CreditCardAccountsTransactionResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(condition.createArrayIsLessThanMaxItemsMessage(
				"data")));
	}


}
