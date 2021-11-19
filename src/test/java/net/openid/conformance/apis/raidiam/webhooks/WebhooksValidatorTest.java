package net.openid.conformance.apis.raidiam.webhooks;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.webhooks.GetWebhooksValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class WebhooksValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/webhooks/GetWebhooksResponse.json")
	public void validateGetWebhooksValidator() {
		GetWebhooksValidator condition = new GetWebhooksValidator();
		run(condition);
	}
}
