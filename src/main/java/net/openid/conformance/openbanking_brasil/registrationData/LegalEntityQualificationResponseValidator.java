package net.openid.conformance.openbanking_brasil.registrationData;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * This is validator for API-Dados Cadastrais "Qualificação Pessoa Jurídica"
 * See <a href="https://openbanking-brasil.github.io/areadesenvolvedor/#qualificacao-pessoa-juridica">Qualificação Pessoa Jurídica</a>
 **/

@ApiName("Legal Entity Qualification")
public class LegalEntityQualificationResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertHasField(body, ROOT_PATH);
		assertInnerFields(body);

		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		JsonObject data = findByPath(body, ROOT_PATH).getAsJsonObject();
		assertField(data,
			new DatetimeField
				.Builder("updateDateTime")
				.build());
		assertHasField(data, "economicActivities");

		assertField(data,
			new ArrayField
				.Builder("economicActivities")
				.setMinItems(1)
				.build());

		assertJsonArrays(data, "economicActivities", this::assertInnerFieldsEconomicActivities);
		assertInformedRevenue(data);
		assertInnerFieldsInformedPatrimony(data);
		assertInnerFieldsInformedPatrimony(data);

	}

	private void assertInnerFieldsEconomicActivities(JsonObject body) {

		assertField(body,
			new IntField
				.Builder("code")
				.setPattern("^\\d{7}$|^NA$")
				.setMaxLength(31)
				.build());

		assertField(body,
			new BooleanField
				.Builder("isMain")
				.build());
	}

	private void assertInformedRevenue(JsonObject body) {
		JsonObject data = findByPath(body, "informedRevenue").getAsJsonObject();
		Set<String> enumFrequency = Sets.newHashSet("DIARIA", "SEMANAL", "QUINZENAL", "MENSAL", "BIMESTRAL", "TRIMESTRAL", "SEMESTRAL", "ANUAL", "SEM_FREQUENCIA_FATURAMENTO_INFORMADO", "OUTROS");
		assertHasField(body, "informedRevenue");

		assertField(data,
			new StringField
				.Builder("frequency")
				.setEnums(enumFrequency)
				.build());

		assertField(data,
			new DoubleField
				.Builder("amount")
				.setNullable()
				.setMinLength(0)
				.setMaxLength(20)
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$|^NA$")
				.setMaxLength(3)
				.build());

		assertField(data,
			new StringField
				.Builder("frequencyAdditionalInfo")
				.setPattern("[\\w\\W\\s]*")
				.setPattern(".+")
				.setOptional()
				.setMaxLength(100)
				.build());

		assertField(data,
			new IntField
				.Builder("year")
				.setOptional()
				.setMaxLength(4)
				.setMaxValue(9999)
				.build());
	}

	private void assertInnerFieldsInformedPatrimony(JsonObject body) {
		JsonObject data = findByPath(body, "informedPatrimony").getAsJsonObject();
		assertHasField(body, "informedPatrimony");

		assertField(data,
			new DoubleField
				.Builder("amount")
				.setNullable()
				.setMinLength(0)
				.setMaxLength(20)
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$|^NA$")
				.setMaxLength(3)
				.build());

		assertField(data,
			new DatetimeField
				.Builder("date")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$|^NA$")
				.build());
	}
}
