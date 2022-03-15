package net.openid.conformance.openinsurance.discovery;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.discovery.StatusListValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

@UseResurce("openinsuranceResponses/discovery/StatusListValidatorResponse.json")
public class StatusListValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void evaluate() {
		run(new StatusListValidator());
	}
}
