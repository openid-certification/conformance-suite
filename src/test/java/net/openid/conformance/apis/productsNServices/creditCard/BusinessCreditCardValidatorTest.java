package net.openid.conformance.apis.productsNServices.creditCard;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.productsNServices.creditCard.BusinessCreditCardValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/productsNServices/creditCard/businessCardsResponseOK.json")
public class BusinessCreditCardValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		BusinessCreditCardValidator condition = new BusinessCreditCardValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/creditCard/businessCardsResponseOK(MissOptional).json")
	public void validateMissingOptional() {
		BusinessCreditCardValidator condition = new BusinessCreditCardValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/creditCard/businessCardsResponse(WrongPattern).json")
	public void validateStructureWrongPattern() {
		BusinessCreditCardValidator condition = new BusinessCreditCardValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchPatternMessage("rate")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/creditCard/businessCardsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		BusinessCreditCardValidator condition = new BusinessCreditCardValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchEnumerationMessage("referentialRateIndexer")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/creditCard/businessCardsResponse(MoreMaxItems).json")
	public void validateStructureMoreMaxItems() {
		BusinessCreditCardValidator condition = new BusinessCreditCardValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createArrayIsMoreThanMaxItemsMessage("otherCredits")));

	}

	@Test
	@UseResurce("jsonResponses/productsNServices/creditCard/businessCardsResponse(LessMinItems).json")
	public void validateStructureLessMinItems() {
		BusinessCreditCardValidator condition = new BusinessCreditCardValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createArrayIsLessThanMaxItemsMessage("instalmentRates")));

	}
}
