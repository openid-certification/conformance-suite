package net.openid.conformance.raidiam.validators.webhooks;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;
/**
 * Api url: ****
 * Api endpoint: GET /organisations/authorisationservers/webhooks
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory GET Webhooks")
public class GetWebhooksValidator extends AbstractJsonAssertingCondition {

	private static final Set<String> WEBHOOK_STATUS = Sets.newHashSet("Confirmed", "Pending", "Deactivated");

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body,
				new ObjectArrayField
						.Builder("$")
						.setValidator(array -> {
							assertField(array,
									new StringField
											.Builder("AuthorisationServerId")
											.setMaxLength(40)
											.setOptional()
											.build());

							assertField(array,
									new StringField
											.Builder("WebhookStatus")
											.setEnums(WEBHOOK_STATUS)
											.setOptional()
											.build());
						})
						.build());

		return environment;
	}
}
