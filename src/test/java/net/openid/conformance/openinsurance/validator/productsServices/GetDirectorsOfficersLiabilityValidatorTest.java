package net.openid.conformance.openinsurance.validator.productsServices;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;


@UseResurce("openinsuranceResponses/productsServices/directorsOfficersLiabilityStructure.json")
public class GetDirectorsOfficersLiabilityValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new GetDirectorsOfficersLiabilityValidator());
	}
}
