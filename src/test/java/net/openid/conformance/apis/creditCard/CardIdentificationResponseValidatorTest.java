package net.openid.conformance.apis.creditCard;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.account.AccountBalancesResponseValidator;
import net.openid.conformance.openbanking_brasil.creditCard.CardIdentificationResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditCard/cardIdentificationResponse.json")
public class CardIdentificationResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		CardIdentificationResponseValidator condition = new CardIdentificationResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardIdentificationResponseWithError.json")
	public void validateStructureWithMissingField() {
		CardIdentificationResponseValidator condition = new CardIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("$.data.paymentMethod.isMultipleCreditCard")));
	}
}
