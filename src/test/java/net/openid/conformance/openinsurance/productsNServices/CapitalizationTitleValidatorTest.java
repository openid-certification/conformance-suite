package net.openid.conformance.openinsurance.productsNServices;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.productsNServices.GetCapitalizationTitleValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class CapitalizationTitleValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/capitalizationTitle/GetCapitalizationTitleResponse.json")
	public void validateStructure() {
		GetCapitalizationTitleValidator condition = new GetCapitalizationTitleValidator();
		run(condition);
	}
}
