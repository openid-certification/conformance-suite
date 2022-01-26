package net.openid.conformance.apis.creditCard;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditCard.CardIdentificationResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditCard/cardIdentification/cardIdentificationResponse.json")
public class CardIdentificationResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		CardIdentificationResponseValidator condition = new CardIdentificationResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardIdentification/cardIdentificationResponseWithError.json")
	public void validateStructureWithMissingField() {
		CardIdentificationResponseValidator condition = new CardIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("isMultipleCreditCard", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardIdentification/cardIdentificationResponseWrongRegexp.json")
	public void validateStructureWithWrongRegexp() {
		CardIdentificationResponseValidator condition = new CardIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("identificationNumber", condition.getApiName())));
	}
	@Test
	@UseResurce("jsonResponses/creditCard/cardIdentification/cardIdentificationResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {
		CardIdentificationResponseValidator condition = new CardIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("productType", condition.getApiName())));
	}
}
