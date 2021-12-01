package net.openid.conformance.apis.productsNServices.openInsurance;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openbanking_brasil.productsNServices.openInsurance.GetPensionPlanValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class PensionPlanValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/productsNServices/pensionPlan/GetPensionPlanResponse.json")
	public void validateStructure() {
		GetPensionPlanValidator condition = new GetPensionPlanValidator();
		run(condition);
	}
}
