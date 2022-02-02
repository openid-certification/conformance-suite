package net.openid.conformance.apis.creditCard;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
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
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("identificationNumber", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardTransactions/cardTransactionsResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		CreditCardAccountsTransactionResponseValidator condition = new CreditCardAccountsTransactionResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("lineName", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardTransactions/cardTransactionsResponseWrongMaxLength.json")
	public void validateStructureWrongMaxLength() {
		CreditCardAccountsTransactionResponseValidator condition = new CreditCardAccountsTransactionResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("payeeMCC", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardTransactions/cardTransactionsResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		CreditCardAccountsTransactionResponseValidator condition = new CreditCardAccountsTransactionResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("billId", condition.getApiName())));
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
			containsString(ErrorMessagesUtils.createArrayIsLessThanMaxItemsMessage(
				"$.data", condition.getApiName())));
	}

	// @Test
	// @UseResurce("jsonResponses/creditCard/cardTransactions/cardTransactionsResponseBadLinks.json")
	// public void validateStructureBadLinks() {
	// 	CreditCardAccountsTransactionResponseValidator condition = new CreditCardAccountsTransactionResponseValidator();
	// 	ConditionError error = runAndFail(condition);
	// 	assertThat(error.getMessage(),
	// 		containsString(condition.createFieldValueNotMatchPatternMessage(
	// 			"$.links.self")));
	// }

	@Test
	@UseResurce("jsonResponses/creditCard/cardTransactions/cardTransactionsResponseMissingPrevLink.json")
	public void validateStructureMissingPrevLink() {
		CreditCardAccountsTransactionResponseValidator condition = new CreditCardAccountsTransactionResponseValidator();
		run(condition);
	}

}
