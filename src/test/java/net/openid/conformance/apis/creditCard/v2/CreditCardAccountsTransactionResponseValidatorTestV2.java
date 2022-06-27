package net.openid.conformance.apis.creditCard.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditCard.v2.CreditCardAccountsTransactionResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditCard/creditCardV2/invoiceCardTransactionV2/invoiceCardTransactionsResponse.json")
public class CreditCardAccountsTransactionResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {

		run(new CreditCardAccountsTransactionResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/invoiceCardTransactionV2/invoiceCardTransactionsResponse_with_missed_identificationNumber_field.json")
	public void validateStructureWithMissingField() {

		ConditionError error = runAndFail(new CreditCardAccountsTransactionResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("identificationNumber", new CreditCardAccountsTransactionResponseValidatorV2().getApiName())));
	}


	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/invoiceCardTransactionV2/invoiceCardTransactionsResponseWrongMaxLength.json")
	public void validateStructureWrongMaxLength() {

		ConditionError error = runAndFail(new CreditCardAccountsTransactionResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("transactionalAdditionalInfo", new CreditCardAccountsTransactionResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/invoiceCardTransactionV2/invoiceCardTransactionsResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {

		ConditionError error = runAndFail(new CreditCardAccountsTransactionResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("billId", new CreditCardAccountsTransactionResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/invoiceCardTransactionV2/invoiceCardTransactionsResponseMissingPrevLink.json")
	public void validateStructureMissingPrevLink() {

		run(new CreditCardAccountsTransactionResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardTransactionsV2/cardTransactionsResponseNullPayeeMCC.json")
	public void payeeMCCIsNullable() {
		ConditionError error = runAndFail(new CreditCardAccountsTransactionResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementCantBeNullMessage("payeeMCC",
			new CreditCardAccountsTransactionResponseValidatorV2().getApiName())));

	}

}
