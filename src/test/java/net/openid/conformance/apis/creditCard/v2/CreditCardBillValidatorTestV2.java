package net.openid.conformance.apis.creditCard.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditCard.v2.CreditCardBillValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;


@UseResurce("jsonResponses/creditCard/creditCardV2/cardBillV2/cardBillResponse.json")
public class CreditCardBillValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {

		run(new CreditCardBillValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardBillV2/cardBillResponseWithError.json")
	public void validateStructureWithMissingField() {

		ConditionError error = runAndFail(new CreditCardBillValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("currency",
			new CreditCardBillValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardBillV2/cardBillResponseWrongEnum.json")
	public void validateStructureWrongEnum() {

		ConditionError error = runAndFail(new CreditCardBillValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("type",
			new CreditCardBillValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardBillV2/cardBillResponseWrongRegexp.json")
	public void validateStructureWrongPattern() {

		ConditionError error = runAndFail(new CreditCardBillValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("amount",
			new CreditCardBillValidatorV2().getApiName())));
	}
}
