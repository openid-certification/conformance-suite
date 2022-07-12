package net.openid.conformance.apis.creditOperations.advances.v1;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.advances.v1.AdvancesGuaranteesResponseValidator;
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
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("warrantyAmount", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesGuarantees/advancesGuaranteesResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		AdvancesGuaranteesResponseValidator condition = new AdvancesGuaranteesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("warrantyType", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesGuarantees/advancesGuaranteesResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		AdvancesGuaranteesResponseValidator condition = new AdvancesGuaranteesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("currency", condition.getApiName())));
	}
}