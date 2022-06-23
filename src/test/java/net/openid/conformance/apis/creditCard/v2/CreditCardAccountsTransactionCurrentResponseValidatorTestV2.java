package net.openid.conformance.apis.creditCard.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditCard.v2.CreditCardAccountsTransactionCurrentResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditCard/creditCardV2/cardTransactionsCurrentV2/cardTransactionsCurrentResponse.json")
public class CreditCardAccountsTransactionCurrentResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {

		run(new CreditCardAccountsTransactionCurrentResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardTransactionsCurrentV2/cardTransactionsCurrentResponse_with_missed_identificationNumber_field.json")
	public void validateStructureWithMissingField() {

		ConditionError error = runAndFail(new CreditCardAccountsTransactionCurrentResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("identificationNumber", new CreditCardAccountsTransactionCurrentResponseValidatorV2().getApiName())));
	}


	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardTransactionsCurrentV2/cardTransactionsCurrentResponseWrongMaxLength.json")
	public void validateStructureWrongMaxLength() {

		ConditionError error = runAndFail(new CreditCardAccountsTransactionCurrentResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("transactionalAdditionalInfo", new CreditCardAccountsTransactionCurrentResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardTransactionsCurrentV2/cardTransactionsCurrentResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {

		ConditionError error = runAndFail(new CreditCardAccountsTransactionCurrentResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("billId", new CreditCardAccountsTransactionCurrentResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardTransactionsCurrentV2/cardTransactionsCurrentResponseMissingPrevLink.json")
	public void validateStructureMissingPrevLink() {

		run(new CreditCardAccountsTransactionCurrentResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardTransactionsCurrentV2/cardTransactionsCurrentResponseNullPayeeMCC.json")
	public void payeeMCCIsNullable() {
		ConditionError error = runAndFail(new CreditCardAccountsTransactionCurrentResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementCantBeNullMessage("payeeMCC",
			new CreditCardAccountsTransactionCurrentResponseValidatorV2().getApiName())));

	}

}
