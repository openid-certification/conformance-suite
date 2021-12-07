package net.openid.conformance.openinsurance.productsNServices;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.productsNServices.GetHomeInsuranceValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class HomeInsuranceValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/homeInsurance/GetHomeInsuranceResponse.json")
	public void validateStructure() {
		GetHomeInsuranceValidator condition = new GetHomeInsuranceValidator();
		run(condition);
	}
}
