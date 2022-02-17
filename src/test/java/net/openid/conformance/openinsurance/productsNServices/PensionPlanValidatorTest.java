package net.openid.conformance.openinsurance.productsNServices;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openinsurance.validator.productsNServices.GetPensionPlanValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class PensionPlanValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/pensionPlan/GetPensionPlanResponse.json")
	public void validateStructure() {
		GetPensionPlanValidator condition = new GetPensionPlanValidator();
		run(condition);
	}

	@Test
	@UseResurce("openinsuranceResponses/pensionPlan/GetPensionPlanResponse(MissingMandatoryField_updateIndex).json")
	public void validateStructureMissingMandatoryFieldUpdateIndex() {
		GetPensionPlanValidator condition = new GetPensionPlanValidator();
		ConditionError error = runAndFail(condition);
		String expected = ErrorMessagesUtils.createElementNotFoundMessage(
			"updateIndex", condition.getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("openinsuranceResponses/pensionPlan/GetPensionPlanResponse(MissingMandatoryField_updateIndex)OK.json")
	public void validateStructureMissingOptionalFieldUpdateIndexOK() {
		GetPensionPlanValidator condition = new GetPensionPlanValidator();
		run(condition);
	}

	@Test
	@UseResurce("openinsuranceResponses/pensionPlan/GetPensionPlanResponse(MissingMandatoryField_reclaimTable).json")
	public void validateStructureMissingMandatoryFieldReclaimTable() {
		GetPensionPlanValidator condition = new GetPensionPlanValidator();
		ConditionError error = runAndFail(condition);
		String expected = ErrorMessagesUtils.createElementNotFoundMessage(
			"reclaimTable", condition.getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

}
