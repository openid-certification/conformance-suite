package net.openid.conformance.openinsurance.validator.productsServices;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class GetOthersScopesValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/productsServices/othersScopesStructure.json")
	public void validateStructure() {
		run(new GetOthersScopesValidator());
	}
}
