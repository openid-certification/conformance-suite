package net.openid.conformance.openinsurance.productsNServices;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.productsNServices.GetPensionPlanValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class PensionPlanValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/pensionPlan/GetPensionPlanResponse.json")
	public void validateStructure() {
		GetPensionPlanValidator condition = new GetPensionPlanValidator();
		run(condition);
	}
}
