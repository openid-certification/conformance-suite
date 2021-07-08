package net.openid.conformance.apis.creditOperations.advances;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.advances.AdvancesResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/advances/advancesResponse/advancesResponse.json")
public class AdvancesResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		AdvancesResponseValidator condition = new AdvancesResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesResponse/advancesResponseWithError.json")
	public void validateStructureWithMissingField() {
		AdvancesResponseValidator condition = new AdvancesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("ipocCode")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesResponse/advancesResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		AdvancesResponseValidator condition = new AdvancesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("productType")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesResponse/advancesResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		AdvancesResponseValidator condition = new AdvancesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("companyCnpj")));
	}
}
