package net.openid.conformance.openinsurance.admin;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openinsurance.validator.admin.v1.AdminMetricsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

@UseResurce("openinsuranceResponses/admin/adminMetricsValidatorV1.json")
public class AdminMetricsValidatorTest extends AbstractJsonResponseConditionUnitTest {
	@Test
	public void validateStructure() {
		run(new AdminMetricsValidator());
	}

	@Test
	@UseResurce("openinsuranceResponses/admin/adminMetricsValidatorV1(totalRecordsNOTFound).json")
	public void totalRecordsNOTFound() {
		AdminMetricsValidator condition = new AdminMetricsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
				.createElementNotFoundMessage("totalRecords", condition.getApiName())));
	}

	@Test
	@UseResurce("openinsuranceResponses/admin/adminMetricsValidatorV1(linksNOTFound).json")
	public void linksNOTFound() {
		AdminMetricsValidator condition = new AdminMetricsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
				.createElementNotFoundMessage("links", condition.getApiName())));
	}
}
