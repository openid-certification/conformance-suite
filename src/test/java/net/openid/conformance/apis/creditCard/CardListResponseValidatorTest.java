package net.openid.conformance.apis.creditCard;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditCard.CardListResponseResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditCard/cardList/cardListResponse.json")
public class CardListResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		CardListResponseResponseValidator condition = new CardListResponseResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardList/cardListResponseWithError.json")
	public void validateStructureWithMissingField() {
		CardListResponseResponseValidator condition = new CardListResponseResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("productType")));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardList/cardListResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {
		CardListResponseResponseValidator condition = new CardListResponseResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("productType")));
	}

	@Test
	@UseResurce("jsonResponses/creditCard/cardList/cardListResponseWrongRegexp.json")
	public void validateStructureWithWrongRegexp() {
		CardListResponseResponseValidator condition = new CardListResponseResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("companyCnpj")));
	}
}
