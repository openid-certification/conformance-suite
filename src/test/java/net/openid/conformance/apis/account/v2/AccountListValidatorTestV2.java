package net.openid.conformance.apis.account.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.account.v2.AccountListValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/account/accountV2/listV2/accountListResponse.json")
public class AccountListValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {
	@Test
	public void validateStructure() {
		run(new AccountListValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/account/accountV2/listV2/accountListResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new AccountListValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("accountId", new AccountListValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/account/accountV2/listV2/accountListResponse_empty.json")
	public void validateStructureWrongRegexp() {
		ConditionError error = runAndFail(new AccountListValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("number", new AccountListValidatorV2().getApiName())));
	}

}
