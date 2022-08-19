package net.openid.conformance.openinsurance.resources;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.resources.v1.OpinResourcesListValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OpinResourcesValidatorsV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/resources/OpinResourcesListValidatorV1OK.json")
	public void validateStructureList() {
		run(new OpinResourcesListValidatorV1());
	}
}

