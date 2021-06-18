package net.openid.conformance.apis.account;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.account.AccountLimitsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/account/limits/accountLimitsResponse.json")
public class AccountLimitsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		AccountLimitsValidator condition = new AccountLimitsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/account/limits/accountLimitsResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		AccountLimitsValidator condition = new AccountLimitsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("unarrangedOverdraftAmountCurrency")));
	}

	@Test
	@UseResurce("jsonResponses/account/limits/errors/accountLimitsResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		AccountLimitsValidator condition = new AccountLimitsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("overdraftUsedLimitCurrency")));
	}
}
