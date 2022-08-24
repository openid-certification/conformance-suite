package net.openid.conformance.openinsurance.validator.customers.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.NumberField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: swagger/openinsurance/customers/v1/swagger-customer-api.yaml
 * Api endpoint: /personal/qualifications
 * Api version: 1.05
 **/

@ApiName("Personal Qualifications V1")
public class OpinCustomersPersonalQualificationListValidatorV1 extends AbstractJsonAssertingCondition {
	private final OpinLinksAndMetaValidator opinLinksAndMetaValidator = new OpinLinksAndMetaValidator(this);
	public static final Set<String> OCCUPATION_CODES = SetUtils.createSet("RFB, CBO, OUTROS");
	public static final Set<String> FREQUENCIES = SetUtils.createSet("DIARIA, SEMANAL, QUINZENAL, MENSAL, BIMESTRAL, TRIMESTRAL, SEMESTRAL, ANUAL");
	public static final Set<String> IDENTIFICATION = SetUtils.createSet("NAO_EXPOSTO, PESSOA_POLITICAMENTE_EXPOSTA_PPE, PESSOA_PROXIMA_A_PESSOA_POLITICAMENTE_EXPOSTA_PPEE, SEM_INFORMACAO");
	public static final Set<String> PENSION_PLAN = SetUtils.createSet("SIM, NAO, NAO_SE_APLICA");

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

	private void assertData(JsonElement data) {
		assertField(data,
			new DatetimeField
				.Builder("updateDateTime")
				.setMaxLength(20)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$")
				.build());

		assertField(data,
			new StringField
				.Builder("pepIdentification")
				.setEnums(IDENTIFICATION)
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("occupation")
				.setValidator(this::assertOccupation)
				.setOptional()
				.setMinItems(1)
				.build());

		assertField(data,
			new StringField
				.Builder("lifePensionPlans")
				.setEnums(PENSION_PLAN)
				.build());

		assertField(data,
			new ObjectField
				.Builder("informedRevenue")
				.setValidator(this::assertInformedIncome)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("informedPatrimony")
				.setValidator(this::assertInformedPatrimony)
				.setOptional()
				.build());
	}

	private void assertOccupation(JsonObject occupation) {
		assertField(occupation,
			new StringField
				.Builder("details")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(occupation,
			new StringField
				.Builder("occupationCode")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(occupation,
			new StringField
				.Builder("occupationCodeType")
				.setEnums(OCCUPATION_CODES)
				.setMaxLength(100)
				.setOptional()
				.build());
	}

	private void assertInformedIncome(JsonElement informedIncome) {
		assertField(informedIncome,
			new StringField
				.Builder("incomeFrequency")
				.setEnums(FREQUENCIES)
				.setOptional()
				.build());

		assertField(informedIncome,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$|^NA$")
				.setMaxLength(3)
				.setOptional()
				.build());

		assertField(informedIncome,
			new NumberField
				.Builder("amount")
				.setMinLength(0)
				.setMaxLength(20)
				.setNullable()
				.setOptional()
				.build());

		assertField(informedIncome,
			new StringField
				.Builder("year")
				.setMaxLength(20)
				.setOptional()
				.build());
	}

	private void assertInformedPatrimony(JsonElement informedPatrimony) {
		assertField(informedPatrimony,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$|^NA$")
				.setMaxLength(3)
				.setOptional()
				.build());

		assertField(informedPatrimony,
			new DoubleField
				.Builder("amount")
				.setMinLength(0)
				.setMaxLength(20)
				.setOptional()
				.setNullable()
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(informedPatrimony,
			new StringField
				.Builder("year")
				.setMaxLength(20)
				.setOptional()
				.build());
	}
}
