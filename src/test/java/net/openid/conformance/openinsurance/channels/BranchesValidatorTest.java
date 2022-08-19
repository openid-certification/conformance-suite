package net.openid.conformance.openinsurance.channels;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.channels.v1.BranchesValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class BranchesValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/channels/GetBranchesResponse.json")
	public void validateStructure() {
		run(new BranchesValidator());
	}
}
