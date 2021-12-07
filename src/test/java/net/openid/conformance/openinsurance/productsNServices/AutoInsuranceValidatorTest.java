package net.openid.conformance.openinsurance.productsNServices;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.productsNServices.GetAutoInsuranceValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class AutoInsuranceValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/autoInsurance/GetAutoInsurance.json")
	public void validateStructure() {
		GetAutoInsuranceValidator condition = new GetAutoInsuranceValidator();
		run(condition);
	}
}
