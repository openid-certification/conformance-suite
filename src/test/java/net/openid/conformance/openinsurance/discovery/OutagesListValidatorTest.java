package net.openid.conformance.openinsurance.discovery;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.discovery.OutagesListValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

@UseResurce("openinsuranceResponses/discovery/OutagesListValidatorResponse.json")
public class OutagesListValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void evaluate() {
		run(new OutagesListValidator());
	}
}
