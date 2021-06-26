package net.openid.conformance.apis.creditCard;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditCard.CreditCardAccountsLimitsResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditCard/cardLimits/cardLimitsResponse.json")
public class CreditCardAccountsLimitsResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		CreditCardAccountsLimitsResponseValidator condition = new CreditCardAccountsLimitsResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardLimits/cardLimitsResponse_with_missed_consolidationType_field.json")
	public void validateStructureWithMissingField() {
		CreditCardAccountsLimitsResponseValidator condition = new CreditCardAccountsLimitsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("consolidationType")));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardLimits/cardLimitsResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		CreditCardAccountsLimitsResponseValidator condition = new CreditCardAccountsLimitsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("creditLineLimitType")));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardLimits/cardLimitsResponseWrongRegexp.json")
	public void validateStructureWrongPattern() {
		CreditCardAccountsLimitsResponseValidator condition = new CreditCardAccountsLimitsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("limitAmount")));
	}


}
