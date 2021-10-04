package net.openid.conformance.apis.productsNServices.creditCard;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.productsNServices.creditCard.PersonalCreditCardValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/productsNServices/creditCard/personalCreditCardsResponseOK.json")
public class PersonalCreditCardValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		PersonalCreditCardValidator condition = new PersonalCreditCardValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/creditCard/personalCreditCardsResponse(MissingOptional).json")
	public void validateMissingOptional() {
		PersonalCreditCardValidator condition = new PersonalCreditCardValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/creditCard/personalCreditCardsResponse(WrongRegexp).json")
	public void validateStructureWrongPattern() {
		PersonalCreditCardValidator condition = new PersonalCreditCardValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchPatternMessage("rate")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/creditCard/personalCreditCardsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		PersonalCreditCardValidator condition = new PersonalCreditCardValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchEnumerationMessage("referentialRateIndexer")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/creditCard/personalCreditCardsResponse(MoreMaxItems).json")
	public void validateStructureMoreMaxItems() {
		PersonalCreditCardValidator condition = new PersonalCreditCardValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createArrayIsMoreThanMaxItemsMessage("otherCredits")));

	}

	@Test
	@UseResurce("jsonResponses/productsNServices/creditCard/personalCreditCardsResponse(LessMinItems).json")
	public void validateStructureLessMinItems() {
		PersonalCreditCardValidator condition = new PersonalCreditCardValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createArrayIsLessThanMaxItemsMessage("instalmentRates")));

	}
}
