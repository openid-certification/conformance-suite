package net.openid.conformance.apis.productsNServices.openInsurance;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.productsNServices.openInsurance.GetPersonValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class PersonValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/productsNServices/person/GetPersonResponse.json")
	public void validateStructure() {
		GetPersonValidator condition = new GetPersonValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/person/GetPersonResponse(MissOptional).json")
	public void validateStructureMissOptionalField() {
		GetPersonValidator condition = new GetPersonValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/person/GetPersonResponse(WrongEnum).json")
	public void validateStructureWrongEnumInStringArray() {
		GetPersonValidator condition = new GetPersonValidator();
		ConditionError error = runAndFail(condition);
		String expected = condition.createFieldValueNotMatchEnumerationMessage("indemnityPaymentMethod");
		assertThat(error.getMessage(), containsString(expected));
	}
}
