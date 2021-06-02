package net.openid.conformance.apis.account;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.account.AccountBalancesResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/account/accountBalancesResponse.json")
public class AccountBalancesResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		AccountBalancesResponseValidator condition = new AccountBalancesResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/account/accountBalancesResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		AccountBalancesResponseValidator condition = new AccountBalancesResponseValidator();
		ConditionError error = runAndFail(condition);
		String expected = "AccountBalancesResponseValidator: Unable to find path $.data.blockedAmount";
		assertThat(error.getMessage(), containsString(expected));
	}
}
