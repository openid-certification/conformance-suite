package net.openid.conformance.openinsurance.validator.productsServices;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class GetEnvironmentalLiabilityValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/productsServices/environmentalLiabilityStructure.json")
	public void validateStructure() {
		GetEnvironmentalLiabilityValidator condition = new GetEnvironmentalLiabilityValidator();
		run(condition);
	}
}
