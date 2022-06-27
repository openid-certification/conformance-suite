package net.openid.conformance.apis.account.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.account.v2.AccountLimitsValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/account/accountV2/limitsV2/accountLimitsResponse.json")
public class AccountLimitsValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new AccountLimitsValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/account/accountV2/limitsV2/accountLimitsResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new AccountLimitsValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("unarrangedOverdraftAmount", new AccountLimitsValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/account/accountV2/limitsV2/errors/accountLimitsResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		ConditionError error = runAndFail(new AccountLimitsValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("amount", new AccountLimitsValidatorV2().getApiName())));
	}
}
