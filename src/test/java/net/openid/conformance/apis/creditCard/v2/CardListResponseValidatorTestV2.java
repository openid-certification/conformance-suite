package net.openid.conformance.apis.creditCard.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditCard.v2.CardAccountsDataResponseResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditCard/creditCardV2/cardListV2/cardListResponse.json")
public class CardListResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new CardAccountsDataResponseResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardListV2/cardListResponseWithError.json")
	public void validateStructureWithMissingField() {

		ConditionError error = runAndFail(new CardAccountsDataResponseResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("productType", new CardAccountsDataResponseResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardListV2/cardListResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {

		ConditionError error = runAndFail(new CardAccountsDataResponseResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("productType", new CardAccountsDataResponseResponseValidatorV2().getApiName())));
	}

}
