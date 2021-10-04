package net.openid.conformance.apis.productsNServices.financings;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.productsNServices.financings.BusinessFinancingsValidator;
import net.openid.conformance.openbanking_brasil.productsNServices.financings.PersonalFinancingsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/productsNServices/financings/businessFinancingsResponseOK.json")
public class BusinessFinancingsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		BusinessFinancingsValidator condition = new BusinessFinancingsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/financings/businessFinancingsResponse(MissOptional).json")
	public void validateMissingOptional() {
		BusinessFinancingsValidator condition = new BusinessFinancingsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/financings/businessFinancingsResponse(WrongRegexp).json")
	public void validateStructureWrongPattern() {
		BusinessFinancingsValidator condition = new BusinessFinancingsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchPatternMessage("rate")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/financings/businessFinancingsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		BusinessFinancingsValidator condition = new BusinessFinancingsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchEnumerationMessage("referentialRateIndexer")));
	}
}
