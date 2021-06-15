package net.openid.conformance.apis.creditCard;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditCard.CreditCardAccountsLimitsResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditCard/cardLimitsResponse.json")
public class CreditCardAccountsLimitsResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		CreditCardAccountsLimitsResponseValidator condition = new CreditCardAccountsLimitsResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardLimitsResponse_with_missed_consolidationType_field.json")
	public void validateStructureWithMissingField() {
		CreditCardAccountsLimitsResponseValidator condition = new CreditCardAccountsLimitsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("$.data[0].consolidationType")));
	}

}
