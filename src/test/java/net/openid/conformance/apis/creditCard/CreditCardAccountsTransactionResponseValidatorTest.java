package net.openid.conformance.apis.creditCard;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditCard.CreditCardAccountsTransactionResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditCard/cardTransactionsResponse.json")
public class CreditCardAccountsTransactionResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		CreditCardAccountsTransactionResponseValidator condition = new CreditCardAccountsTransactionResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardTransactionsResponse_with_missed_identificationNumber_field.json")
	public void validateStructureWithMissingField() {
		CreditCardAccountsTransactionResponseValidator condition = new CreditCardAccountsTransactionResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("$.data[0].identificationNumber")));
	}
}
