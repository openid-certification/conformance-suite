package net.openid.conformance.openbanking_brasil.resourcesAPI.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.LinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger/openBanking/swagger-resources-apis-v2.yaml
 * Api endpoint: /resources
 * Api version: 2.0.1 Final
 **/
@ApiName("Resources V2")
public class ResourcesResponseValidatorV2 extends AbstractJsonAssertingCondition {

	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);

	public static final Set<String> ENUM_STATUS = SetUtils.createSet("AVAILABLE, UNAVAILABLE, TEMPORARILY_UNAVAILABLE, PENDING_AUTHORISATION");
	public static final Set<String> ACCOUNT = SetUtils.createSet("ACCOUNT, CREDIT_CARD_ACCOUNT, LOAN, FINANCING, UNARRANGED_ACCOUNT_OVERDRAFT, INVOICE_FINANCING");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertInnerFields)
				.setMinItems(0)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("resourceId")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{0,99}$")
				.setMaxLength(100)
				.setMinLength(1)
				.build());

		assertField(body,
			new StringField
				.Builder("type")
				.setEnums(ACCOUNT)
				.build());

		assertField(body,
			new StringField
				.Builder("status")
				.setEnums(ENUM_STATUS)
				.build());
	}
}
