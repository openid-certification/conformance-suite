package net.openid.conformance.openinsurance.validator.customers.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: swagger/openinsurance/customers/v1/swagger-customer-api.yaml
 * Api endpoint: /business/qualifications
 * Api version: 1.05
 **/
@ApiName("Business Qualifications V1")
public class OpinCustomersBusinessQualificationListValidatorV1 extends AbstractJsonAssertingCondition {

	private final OpinLinksAndMetaValidator opinLinksAndMetaValidator = new OpinLinksAndMetaValidator(this);
	public static final Set<String> ENUM_FREQUENCY = SetUtils.createSet("DIARIA, SEMANAL, QUINZENAL, MENSAL, BIMESTRAL, TRIMESTRAL, SEMESTRAL, ANUAL");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertData)
				.build());
		opinLinksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertData(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("updateDateTime")
				.setMaxLength(20)
				.build());

		assertField(body,
			new StringField
				.Builder("mainBranch")
				.setMaxLength(20)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("secondaryBranch")
				.setMaxLength(200)
				.setOptional()
				.build());

		assertField(body,
			new ObjectField
				.Builder("informedRevenue")
				.setValidator(this::assertInformedRevenue)
				.setOptional()
				.build());

		assertField(body,
			new ObjectField
				.Builder("informedPatrimony")
				.setValidator(this::assertInnerFieldsInformedPatrimony)
				.setOptional()
				.build());
	}

	private void assertInformedRevenue(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("incomeFrequency")
				.setEnums(ENUM_FREQUENCY)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$|^NA$")
				.setOptional()
				.setMaxLength(3)
				.build());

		assertField(data,
			new DoubleField
				.Builder("amount")
				.setOptional()
				.setMaxLength(20)
				.setMinLength(0)
				.setNullable()
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(data,
			new StringField
				.Builder("year")
				.setOptional()
				.setMaxLength(20)
				.build());
	}

	private void assertInnerFieldsInformedPatrimony(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$|^NA$")
				.setOptional()
				.setMaxLength(3)
				.build());

		assertField(data,
			new DoubleField
				.Builder("amount")
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setOptional()
				.setMaxLength(20)
				.setMinLength(0)
				.setNullable()
				.build());

		assertField(data,
			new StringField
				.Builder("year")
				.setOptional()
				.setMaxLength(20)
				.build());
	}
}
