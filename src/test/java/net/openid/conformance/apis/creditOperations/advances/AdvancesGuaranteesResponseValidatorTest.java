package net.openid.conformance.apis.creditOperations.advances;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.advances.AdvancesGuaranteesResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/advances/advancesGuarantees/advancesGuaranteesResponse.json")
public class AdvancesGuaranteesResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		AdvancesGuaranteesResponseValidator condition = new AdvancesGuaranteesResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesGuarantees/advancesGuaranteesResponseWithError.json")
	public void validateStructureWithMissingField() {
		AdvancesGuaranteesResponseValidator condition = new AdvancesGuaranteesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("warrantyAmount")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesGuarantees/advancesGuaranteesResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		AdvancesGuaranteesResponseValidator condition = new AdvancesGuaranteesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("warrantyType")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesGuarantees/advancesGuaranteesResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		AdvancesGuaranteesResponseValidator condition = new AdvancesGuaranteesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("currency")));
	}
}
