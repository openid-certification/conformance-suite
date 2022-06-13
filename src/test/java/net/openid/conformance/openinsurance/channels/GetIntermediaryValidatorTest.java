package net.openid.conformance.openinsurance.channels;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.channels.GetIntermediaryValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

@UseResurce("openinsuranceResponses/productsServices/intermediaryStructure.json")
public class GetIntermediaryValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new GetIntermediaryValidator());
	}
}
