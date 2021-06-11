package net.openid.conformance.apis.account;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.account.AccountIdentificationResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/account/accountIdentificationResponse.json")
public class AccountIdentificationResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		AccountIdentificationResponseValidator condition = new AccountIdentificationResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/account/accountIdentificationResponseWithError.json")
	public void validateStructureWithMissingField() {
		AccountIdentificationResponseValidator condition = new AccountIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(condition.createElementNotFoundMessage("currency")));
	}
}
