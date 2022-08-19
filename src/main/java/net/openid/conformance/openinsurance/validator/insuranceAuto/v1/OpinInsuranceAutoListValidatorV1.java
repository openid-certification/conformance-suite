package net.openid.conformance.openinsurance.validator.insuranceAuto.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openinsurance.validator.OpenInsuranceLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

/**
 * Api Source: swagger/openinsurance/insuranceAuto/v1/swagger-insurance-auto-api.yaml
 * Api endpoint: /
 * Api version: 1.0.0
 */

@ApiName("Insurance Auto List V1")
public class OpinInsuranceAutoListValidatorV1 extends AbstractJsonAssertingCondition {
	private final OpenInsuranceLinksAndMetaValidator linksAndMetaValidator = new OpenInsuranceLinksAndMetaValidator(this);

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertInnerFields)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertInnerFields(JsonObject identification) {
		assertField(identification,
			new StringField
				.Builder("brand")
				.setMaxLength(80)
				.build());

		assertField(identification,
			new ObjectArrayField
				.Builder("companies")
				.setValidator(this::assertCompanies)
				.build());
	}

	private void assertCompanies(JsonObject products) {
		assertField(products,
			new StringField
				.Builder("companyName")
				.setMaxLength(80)
				.build());

		assertField(products,
			new StringField
				.Builder("cnpjNumber")
				.setMaxLength(14)
				.setPattern("^\\d{14}$")
				.build());

		assertField(products,
			new StringArrayField
				.Builder("policies")
				.setMaxLength(60)
				.build());
	}
}
