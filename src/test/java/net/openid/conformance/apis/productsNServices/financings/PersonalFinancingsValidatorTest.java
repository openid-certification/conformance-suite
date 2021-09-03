package net.openid.conformance.apis.productsNServices.financings;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.productsNServices.financings.PersonalFinancingsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/productsNServices/financings/personalFinancingsResponseOK.json")
public class PersonalFinancingsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		PersonalFinancingsValidator condition = new PersonalFinancingsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/financings/personalFinancingsResponse(MissOptional).json")
	public void validateMissingOptional() {
		PersonalFinancingsValidator condition = new PersonalFinancingsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/financings/personalFinancingsResponse(WrongRegexp).json")
	public void validateStructureWrongPattern() {
		PersonalFinancingsValidator condition = new PersonalFinancingsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchPatternMessage("rate")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/financings/personalFinancingsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		PersonalFinancingsValidator condition = new PersonalFinancingsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchEnumerationMessage("interval")));
	}
}
