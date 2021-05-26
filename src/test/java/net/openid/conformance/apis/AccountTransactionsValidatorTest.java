package net.openid.conformance.apis;

import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.AccountTransactionsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;


@UseResurce("jsonResponses/accountTransactionsResponse.json")
public class AccountTransactionsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {

		// Here we simply create an instance of our Condition class
		AccountTransactionsValidator condition = new AccountTransactionsValidator();

		// This evaluates the condition, passing in the loaded JSON document as
		// if it were an HTTP response
		run(condition);

	}

	@Test
	@UseResurce("jsonResponses/accountTransactionsResponse_missing_consents.json")
	public void validateStructureWithMissingField() {

		// Here we simply create an instance of our Condition class
		AccountTransactionsValidator condition = new AccountTransactionsValidator();

		// This evaluates the condition, passing in the loaded JSON document as
		// if it were an HTTP response
		// In this instance, we expect a failure, and thus, examine it
		ConditionError error = runAndFail(condition);

		// We make sure it is the error we're expecting
		assertThat(error.getMessage(), containsString("AccountTransactionsValidator: Unable to find path $.data[0].accountID"));

	}
}
