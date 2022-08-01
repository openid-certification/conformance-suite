package net.openid.conformance.openbanking_brasil.registrationData.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.LinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api url: swagger/openinsurance/registrationData/swagger-customers-v2.yaml
 * Api endpoint: /personal/qualifications
 * Api version: 2.0.1.final
 **/

@ApiName("Natural Personal Qualification V2")
public class PersonalQualificationResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);
	public static final Set<String> OCCUPATION_CODES = SetUtils.createSet("RECEITA_FEDERAL, CBO, OUTRO");
	public static final Set<String> FREQUENCIES = SetUtils.createSet("DIARIA, SEMANAL, QUINZENAL, MENSAL, BIMESTRAL, TRIMESTRAL, SEMESTRAL, ANUAL, OUTROS");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectField
				.Builder("data")
				.setValidator(this::assertData)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertData(JsonElement data) {
		assertField(data,
			new DatetimeField
				.Builder("updateDateTime")
				.setMaxLength(20)
				.build());

		assertField(data,
			new StringField
				.Builder("companyCnpj")
				.setPattern("^\\d{14}$")
				.setMaxLength(14)
				.build());

		assertField(data,
			new StringField
				.Builder("occupationCode")
				.setEnums(OCCUPATION_CODES)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("occupationDescription")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("informedIncome")
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

	private void assertInformedIncome(JsonElement informedIncome) {
		assertField(informedIncome,
			new StringField
				.Builder("frequency")
				.setEnums(FREQUENCIES)
				.build());

		assertField(informedIncome,
			new ObjectField
				.Builder("amount")
				.setValidator(this::assertAmount)
				.build());

		assertField(informedIncome,
			new DatetimeField
				.Builder("date")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.build());
	}

	private void assertAmount(JsonObject amount) {
		assertField(amount,
			new StringField
				.Builder("amount")
				.setMinLength(4)
				.setMaxLength(20)
				.setPattern("^\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(amount,
			new StringField
				.Builder("currency")
				.setPattern("^[A-Z]{3}$")
				.setMaxLength(3)
				.build());
	}

	private void assertInformedPatrimony(JsonElement informedPatrimony) {
		assertField(informedPatrimony,
			new ObjectField
				.Builder("amount")
				.setValidator(this::assertAmount)
				.build());

		assertField(informedPatrimony,
			new IntField
				.Builder("year")
				.setMaxValue(9999)
				.build());
	}
}
