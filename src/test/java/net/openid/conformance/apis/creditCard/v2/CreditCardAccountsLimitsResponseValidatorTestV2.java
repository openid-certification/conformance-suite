package net.openid.conformance.apis.creditCard.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditCard.v2.CreditCardAccountsLimitsResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditCard/creditCardV2/cardLimitsV2/cardLimitsResponse.json")
public class CreditCardAccountsLimitsResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {

		run(new CreditCardAccountsLimitsResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardLimitsV2/cardLimitsResponse_with_missed_consolidationType_field.json")
	public void validateStructureWithMissingField() {

		ConditionError error = runAndFail(new CreditCardAccountsLimitsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("consolidationType", new CreditCardAccountsLimitsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardLimitsV2/cardLimitsResponseWrongEnum.json")
	public void validateStructureWrongEnum() {

		ConditionError error = runAndFail(new CreditCardAccountsLimitsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("creditLineLimitType", new CreditCardAccountsLimitsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardLimitsV2/cardLimitsResponseWrongRegexp.json")
	public void validateStructureWrongPattern() {

		ConditionError error = runAndFail(new CreditCardAccountsLimitsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("amount", new CreditCardAccountsLimitsResponseValidatorV2().getApiName())));
	}


}
