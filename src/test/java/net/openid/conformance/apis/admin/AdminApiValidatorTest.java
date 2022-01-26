package net.openid.conformance.apis.admin;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.admin.GetMetricsAdminApiValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class AdminApiValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/admin/GetMetricsResponse.json")
	public void validateStructure() {
		GetMetricsAdminApiValidator condition = new GetMetricsAdminApiValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/admin/GetMetricsResponse_missing_field.json")
	public void validateStructureWithMissingField() {
		GetMetricsAdminApiValidator condition = new GetMetricsAdminApiValidator();
		ConditionError error = runAndFail(condition);
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("requestTime",
			condition.getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}
}
