package net.openid.conformance.apis.account.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.account.v2.AccountTransactionsValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;


public class AccountTransactionsValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/account/accountV2/transactionsV2/accountTransactionsResponseOK.json")
	public void validateStructure() {

		run(new AccountTransactionsValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/account/accountV2/transactionsV2/errors/accountTransactionsResponseWithError(missingConsents).json")
	public void validateStructureWithMissingField() {

		ConditionError error = runAndFail(new AccountTransactionsValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage(
			"completedAuthorisedPaymentType", new AccountTransactionsValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/account/accountV2/transactionsV2/errors/accountTransactionsResponseWithError(PatternNotMatch).json")
	public void validateStructurePatternNotMatch() {

		ConditionError error = runAndFail(new AccountTransactionsValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils
				.createFieldValueNotMatchPatternMessage("transactionDate", new AccountTransactionsValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/account/accountV2/transactionsV2/errors/accountTransactionsResponseWithError(ExcessMaxLength).json")
	public void validateStructureExcessMaxLength() {

		ConditionError error = runAndFail(new AccountTransactionsValidatorV2());
		// We make sure it is the error we're expecting
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage(
				"partieCheckDigit", new AccountTransactionsValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/account/accountV2/transactionsV2/errors/accountTransactionsResponseWithError(EnumNotMatch).json")
	public void validateStructureEnumNotMatch() {

		ConditionError error = runAndFail(new AccountTransactionsValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage(
				"creditDebitType", new AccountTransactionsValidatorV2().getApiName())));
	}


}