package net.openid.conformance.apis.raidiam.webhooks;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.raidiam.validators.webhooks.GetWebhooksValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class WebhooksValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/webhooks/GetWebhooksResponse.json")
	public void validateGetWebhooksValidator() {
		GetWebhooksValidator condition = new GetWebhooksValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/webhooks/GetWebhooksResponse_maxLengthError.json")
	public void validateStructureWithWrongMaxLength() {
		GetWebhooksValidator condition = new GetWebhooksValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("AuthorisationServerId",
			condition.getApiName())));
	}
}
