package net.openid.conformance.apis.creditCard;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.account.AccountBalancesResponseValidator;
import net.openid.conformance.openbanking_brasil.creditCard.CreditCardBillValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;


@UseResurce("jsonResponses/creditCard/cardBillResponse.json")
public class CreditCardBillValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		CreditCardBillValidator condition = new CreditCardBillValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardBillResponseWithError.json")
	public void validateStructureWithMissingField() {
		CreditCardBillValidator condition = new CreditCardBillValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("$.data[0].payments[0].currency")));
	}
}
