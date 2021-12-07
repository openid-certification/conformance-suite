package net.openid.conformance.openinsurance;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.admin.AdminMetricsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

@UseResurce("openinsuranceResponses/admin/adminMetrics.json")
public class AdminMetricsValidatorTest extends AbstractJsonResponseConditionUnitTest {
	@Test
	public void validateStructure() {
		run(new AdminMetricsValidator());
	}
}
