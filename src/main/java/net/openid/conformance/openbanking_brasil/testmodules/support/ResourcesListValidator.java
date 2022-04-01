package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.openinsurance.validator.OpenInsuranceLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

public class ResourcesListValidator extends AbstractJsonAssertingCondition {
	private final OpenInsuranceLinksAndMetaValidator linksAndMetaValidator = new OpenInsuranceLinksAndMetaValidator(this);

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertHasField(body, ROOT_PATH);
		assertField(body,
			new ObjectArrayField.Builder("data")
				.setValidator(this::assertInnerFields)
				.setMinItems(1)
				.build());

		linksAndMetaValidator.assertMetaAndLinks(body);

		return environment;
	}

	private void assertInnerFields(JsonObject resource) {

		assertField(resource,
			new StringField
				.Builder("type")
				.setEnums(EnumResourcesType.allTypes())
				.build());

		assertField(resource,
			new StringField
				.Builder("status")
				.setEnums(EnumResourcesStatus.allStatuses())
				.build());

		assertField(resource,
			new StringField
				.Builder("resourceId")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.setMinLength(1)
				.setMaxLength(100)
				.setOptional()
				.build());

	}
}
