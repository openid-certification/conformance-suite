package net.openid.conformance.apis;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.GetAccountResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;


@UseResurce("jsonResponses/accountConsentResponse.json")
public class GetAccountResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {

		// Here we simply create an instance of our Condition class
		GetAccountResponseValidator condition = new GetAccountResponseValidator();

		// This evaluates the condition, passing in the loaded JSON document as
		// if it were an HTTP response
		run(condition);

	}

	@Test
	@UseResurce("jsonResponses/accountConsentResponse_missing_consents.json")
	public void validateStructureWithMissingField() {

		// Here we simply create an instance of our Condition class
		GetAccountResponseValidator condition = new GetAccountResponseValidator();

		// This evaluates the condition, passing in the loaded JSON document as
		// if it were an HTTP response
		// In this instance, we expect a failure, and thus, examine it
		ConditionError error = runAndFail(condition);

		// We make sure it is the error we're expecting
		assertThat(error.getMessage(), containsString("AccountResponseValidator: Unable to find path $.data[0].accountID"));

	}
}
