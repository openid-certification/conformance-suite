package net.openid.conformance.openinsurance.validator.resources.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api Source: swagger/openinsurance/resources/v1/swagger-resources.yaml
 * Api endpoint: /
 * Api version: 1.0.4
 */

@ApiName("Resources List V1")
public class OpinResourcesListValidatorV1 extends AbstractJsonAssertingCondition {
	public static final Set<String> TYPE = SetUtils.createSet("CUSTOMERS_PERSONAL_IDENTIFICATIONS, CUSTOMERS_PERSONAL_QUALIFICATION, CUSTOMERS_PERSONAL_ADITTIONALINFO, CUSTOMERS_BUSINESS_IDENTIFICATIONS, CUSTOMERS_BUSINESS_QUALIFICATION, CUSTOMERS_BUSINESS_ADITTIONALINFO, CAPITALIZATION_TITLES, PENSION_RISK, DAMAGES_AND_PEOPLE_PATRIMONIAL, DAMAGES_AND_PEOPLE_AERONAUTICAL, DAMAGES_AND_PEOPLE_NAUTICAL, DAMAGES_AND_PEOPLE_NUCLEAR, DAMAGES_AND_PEOPLE_OIL, DAMAGES_AND_PEOPLE_RESPONSABILITY, DAMAGES_AND_PEOPLE_TRANSPORT, DAMAGES_AND_PEOPLE_FINANCIAL_RISKS, DAMAGES_AND_PEOPLE_RURAL, DAMAGES_AND_PEOPLE_AUTO, DAMAGES_AND_PEOPLE_HOUSING, DAMAGES_AND_PEOPLE_PEOPLE, DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD");
	public static final Set<String> STATUS = SetUtils.createSet("AVAILABLE, UNAVAILABLE, TEMPORARILY_UNAVAILABLE, PENDING_AUTHORISATION");
	public static final String pattern = "^(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$";

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertInnerFields)
				.build());

		assertField(body,
			new ObjectField
				.Builder("links")
				.setValidator(this::assertLinks)
				.build());

		assertField(body,
			new ObjectField
				.Builder("meta")
				.setValidator(this::assertMeta)
				.build());

		logFinalStatus();
		return environment;
	}

	private void assertInnerFields(JsonObject identification) {
		assertField(identification,
			new StringField
				.Builder("resourceId")
				.setMinLength(1)
				.setMaxLength(100)
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.build());

		assertField(identification,
			new StringField
				.Builder("type")
				.setEnums(TYPE)
				.build());

		assertField(identification,
			new StringField
				.Builder("status")
				.setEnums(STATUS)
				.build());
	}

	public void assertLinks(JsonObject links) {
		assertField(links,
			new StringField
				.Builder("self")
				.setMaxLength(2000)
				.setPattern(pattern)
				.build());

		assertField(links,
			new StringField
				.Builder("first")
				.setMaxLength(2000)
				.setPattern(pattern)
				.setOptional()
				.build());

		assertField(links,
			new StringField
				.Builder("prev")
				.setMaxLength(2000)
				.setPattern(pattern)
				.setOptional()
				.build());

		assertField(links,
			new StringField
				.Builder("next")
				.setMaxLength(2000)
				.setPattern(pattern)
				.setOptional()
				.build());

		assertField(links,
			new StringField
				.Builder("last")
				.setMaxLength(2000)
				.setPattern(pattern)
				.setOptional()
				.build());
	}

	public void assertMeta(JsonObject meta) {
		assertField(meta,
			new IntField
				.Builder("totalRecords")
				.build());

		assertField(meta,
			new IntField
				.Builder("totalPages")
				.build());

		assertField(meta,
			new StringField
				.Builder("requestDateTime")
				.setMaxLength(20)
				.build());
	}
}
