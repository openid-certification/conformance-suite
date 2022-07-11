package net.openid.conformance.apis.account.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.testmodules.account.AccountBalancesResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/account/accountV2/balancesV2/accountBalancesResponse.json")
public class AccountBalancesResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new AccountBalancesResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/account/accountV2/balancesV2/accountBalancesResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new AccountBalancesResponseValidatorV2());
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("blockedAmount", new AccountBalancesResponseValidatorV2().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/account/accountV2/balancesV2/errors/accountBalancesResponseWrongPattern.json")
	public void validateStructureWithWrongRegexp() {
		ConditionError error = runAndFail(new AccountBalancesResponseValidatorV2());
		String expected = ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("amount", new AccountBalancesResponseValidatorV2().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}
}
