package net.openid.conformance.apis.account.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.testmodules.account.AccountIdentificationResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/account/accountV2/identificationV2/accountIdentificationResponse.json")
public class AccountIdentificationResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new AccountIdentificationResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/account/accountV2/identificationV2/accountIdentificationResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new AccountIdentificationResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createElementNotFoundMessage("currency", new AccountIdentificationResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/account/accountV2/identificationV2/accountIdentificationResponseWrongFieldRegexp.json")
	public void validateStructureWithMissingWrongFieldRegexp() {
		ConditionError error = runAndFail(new AccountIdentificationResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("number", new AccountIdentificationResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/account/accountV2/identificationV2/accountIdentificationResponseWrongEnum.json")
	public void validateStructureWithMissingWrongEnum() {
		ConditionError error = runAndFail(new AccountIdentificationResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("type", new AccountIdentificationResponseValidatorV2().getApiName())));
	}
}
