package net.openid.conformance.openinsurance.productsNServices;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.productsNServices.GetLifePensionValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class LifePensionValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/lifePension/GetLifePensionResponse.json")
	public void validateStructure() {
		run(new GetLifePensionValidator());
	}
}
