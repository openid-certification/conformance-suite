package net.openid.conformance.apis.account;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.account.AccountListValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/account/accountListResponse.json")
public class AccountListValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {

		// Here we simply create an instance of our Condition class
		AccountListValidator condition = new AccountListValidator();

		// This evaluates the condition, passing in the loaded JSON document as
		// if it were an HTTP response
		run(condition);

	}

	@Test
	@UseResurce("jsonResponses/account/accountListResponse_missing_consents.json")
	public void validateStructureWithMissingField() {

		// Here we simply create an instance of our Condition class
		AccountListValidator condition = new AccountListValidator();

		// This evaluates the condition, passing in the loaded JSON document as
		// if it were an HTTP response
		// In this instance, we expect a failure, and thus, examine it
		ConditionError error = runAndFail(condition);

		// We make sure it is the error we're expecting
		assertThat(error.getMessage(), containsString("AccountListValidator: Unable to find path $.data[0].accountId"));

	}

}
