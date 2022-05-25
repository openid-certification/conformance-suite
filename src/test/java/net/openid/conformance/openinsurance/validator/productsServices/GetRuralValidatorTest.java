package net.openid.conformance.openinsurance.validator.productsServices;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class GetRuralValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/productsServices/ruralStructure.json")
	public void validateStructure() {
		run(new GetRuralValidator());
	}
}
