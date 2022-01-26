package net.openid.conformance.apis.account;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.account.AccountIdentificationResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/account/identification/accountIdentificationResponse.json")
public class AccountIdentificationResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		AccountIdentificationResponseValidator condition = new AccountIdentificationResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/account/identification/accountIdentificationResponseWithError.json")
	public void validateStructureWithMissingField() {
		AccountIdentificationResponseValidator condition = new AccountIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createElementNotFoundMessage("currency", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/account/identification/accountIdentificationResponseWrongFieldRegexp.json")
	public void validateStructureWithMissingWrongFieldRegexp() {
		AccountIdentificationResponseValidator condition = new AccountIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("number", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/account/identification/accountIdentificationResponseWrongEnum.json")
	public void validateStructureWithMissingWrongEnum() {
		AccountIdentificationResponseValidator condition = new AccountIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("type", condition.getApiName())));
	}
}
