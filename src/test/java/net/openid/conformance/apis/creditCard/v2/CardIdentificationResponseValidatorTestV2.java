package net.openid.conformance.apis.creditCard.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditCard.v2.CardIdentificationResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditCard/creditCardV2/cardIdentificationV2/cardIdentificationResponse.json")
public class CardIdentificationResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new CardIdentificationResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardIdentificationV2/cardIdentificationResponseWithError.json")
	public void validateStructureWithMissingField() {

		ConditionError error = runAndFail(new CardIdentificationResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("isMultipleCreditCard", new CardIdentificationResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardIdentificationV2/cardIdentificationResponseWrongRegexp.json")
	public void validateStructureWithWrongRegexp() {

		ConditionError error = runAndFail(new CardIdentificationResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("identificationNumber", new CardIdentificationResponseValidatorV2().getApiName())));
	}
	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardIdentificationV2/cardIdentificationResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {

		ConditionError error = runAndFail(new CardIdentificationResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("productType", new CardIdentificationResponseValidatorV2().getApiName())));
	}
}
