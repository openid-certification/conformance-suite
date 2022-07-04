package net.openid.conformance.apis.creditCard.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditCard.v2.CreditCardAccountsTransactionBillResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditCard/creditCardV2/cardTransactionsV2/cardTransactionsResponse.json")
public class CreditCardAccountsTransactionBillResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {

		run(new CreditCardAccountsTransactionBillResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardTransactionsV2/cardTransactionsResponse_with_missed_identificationNumber_field.json")
	public void validateStructureWithMissingField() {

		ConditionError error = runAndFail(new CreditCardAccountsTransactionBillResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("identificationNumber", new CreditCardAccountsTransactionBillResponseValidatorV2().getApiName())));
	}


	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardTransactionsV2/cardTransactionsResponseWrongMaxLength.json")
	public void validateStructureWrongMaxLength() {

		ConditionError error = runAndFail(new CreditCardAccountsTransactionBillResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("transactionalAdditionalInfo", new CreditCardAccountsTransactionBillResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardTransactionsV2/cardTransactionsResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {

		ConditionError error = runAndFail(new CreditCardAccountsTransactionBillResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("billId", new CreditCardAccountsTransactionBillResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardTransactionsV2/cardTransactionsResponseMissingPrevLink.json")
	public void validateStructureMissingPrevLink() {

		run(new CreditCardAccountsTransactionBillResponseValidatorV2());
	}

}
