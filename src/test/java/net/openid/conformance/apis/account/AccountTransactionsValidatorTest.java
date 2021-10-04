package net.openid.conformance.apis.account;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.account.AccountTransactionsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;


@UseResurce("jsonResponses/account/accountTransactionsResponse.json")
public class AccountTransactionsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/account/transactions/accountTransactionsResponseOK.json")
	public void validateStructure() {
		AccountTransactionsValidator condition = new AccountTransactionsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/account/transactions/accountTransactionsResponseOK(missingNotMandatoryField).json")
	public void validateStructureWithMissingNotMandatoryField() {
		AccountTransactionsValidator condition = new AccountTransactionsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/account/transactions/errors/accountTransactionsResponseWithError(missingConsents).json")
	public void validateStructureWithMissingField() {
		AccountTransactionsValidator condition = new AccountTransactionsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage(
			"partieCheckDigit")));
	}

	@Test
	@UseResurce("jsonResponses/account/transactions/errors/accountTransactionsResponseWithError(PatternNotMatch).json")
	public void validateStructurePatternNotMatch() {
		AccountTransactionsValidator condition = new AccountTransactionsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(condition.createFieldValueNotMatchPatternMessage("transactionDate")));
	}

	@Test
	@UseResurce("jsonResponses/account/transactions/errors/accountTransactionsResponseWithError(ExcessMaxLength).json")
	public void validateStructureExcessMaxLength() {
		AccountTransactionsValidator condition = new AccountTransactionsValidator();
		ConditionError error = runAndFail(condition);
		// We make sure it is the error we're expecting
		assertThat(error.getMessage(),
			containsString(condition.createFieldValueIsMoreThanMaxLengthMessage(
				"partieCheckDigit")));
	}

	@Test
	@UseResurce("jsonResponses/account/transactions/errors/accountTransactionsResponseWithError(EnumNotMatch).json")
	public void validateStructureEnumNotMatch() {
		AccountTransactionsValidator condition = new AccountTransactionsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(condition.createFieldValueNotMatchEnumerationMessage(
				"creditDebitType")));
	}

	@Test
	@UseResurce("jsonResponses/account/transactions/errors/accountTransactionsResponseWithError(Empty).json")
	public void validateStructureWithEmptyList() {
		AccountTransactionsValidator condition = new AccountTransactionsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(condition.createArrayIsLessThanMaxItemsMessage(
				"data")));
	}
	// @Test
	// @UseResurce("jsonResponses/account/transactions/errors/accountTransactionsResponseWithError(BadLinks).json")
	// public void validateStructureBadLinks() {
	// 	AccountTransactionsValidator condition = new AccountTransactionsValidator();
	// 	ConditionError error = runAndFail(condition);
	// 	assertThat(error.getMessage(),
	// 		containsString(condition.createFieldValueNotMatchPatternMessage(
	// 			"$.links.self")));
	// }

}
