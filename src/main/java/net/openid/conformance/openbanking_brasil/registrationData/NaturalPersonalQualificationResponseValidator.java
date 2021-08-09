package net.openid.conformance.openbanking_brasil.registrationData;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * This is validator for API - Dados Cadastrais - Qualificação Pessoa Natural
 * See <a href="https://openbanking-brasil.github.io/areadesenvolvedor/#qualificacao-pessoa-natural">Qualificação Pessoa Natural</a>
 */

@ApiName("Natural Personal Qualification")
public class NaturalPersonalQualificationResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertData(body);
		assertHasField(body, "$.data.informedIncome");
		assertHasField(body, "$.data.informedPatrimony");
		assertInformedIncome(body);
		assertInformedPatrimony(body);
		return environment;
	}

	private void assertData(JsonObject body) {
		Set<String> occupationCodes = Sets.newHashSet("RECEITA_FEDERAL", "CBO", "OUTRO");
		JsonObject data = findByPath(body, ROOT_PATH).getAsJsonObject();

		assertField(data,
			new StringField
				.Builder("updateDateTime")
				.setMaxLength(20)
				.build());

		assertField(data,
			new StringField
				.Builder("companyCnpj")
				.setPattern("\\d{14}|^NA$")
				.setMaxLength(14)
				.build());

		assertField(data,
			new StringField
				.Builder("occupationCode")
				.setEnums(occupationCodes)
				.build());

		assertField(data,
			new StringField
				.Builder("occupationDescription")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(100)
				.build());
	}

	private void assertInformedIncome(JsonObject body) {
		Set<String> frequencies = Sets.newHashSet("DIARIA", "SEMANAL", "QUINZENAL", "MENSAL", "BIMESTRAL",
			"TRIMESTRAL", "SEMESTRAL", "ANUAL", "SEM_FREQUENCIA_RENDA_INFORMADA", "OUTROS");

		JsonObject informedIncome = findByPath(body, "$.data.informedIncome").getAsJsonObject();
		assertField(informedIncome,
			new StringField
				.Builder("frequency")
				.setEnums(frequencies)
				.build());

		assertField(informedIncome,
			new DoubleField
				.Builder("amount")
				.setMinLength(0)
				.setNullable()
				.setMaxLength(20)
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(informedIncome,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$|^NA$")
				.setMaxLength(3)
				.build());

		assertField(informedIncome,
			new StringField
				.Builder("date")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$|^NA$")
				.setMaxLength(10)
				.build());
	}

	private void assertInformedPatrimony(JsonObject body) {
		JsonObject informedPatrimony = findByPath(body, "$.data.informedPatrimony").getAsJsonObject();

		assertField(informedPatrimony,
			new DoubleField
				.Builder("amount")
				.setMinLength(0)
				.setMaxLength(20)
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setNullable()
				.build());

		assertField(informedPatrimony,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$|^NA$")
				.setMaxLength(3)
				.build());

		assertField(informedPatrimony,
			new IntField
				.Builder("year")
				.setMaxLength(4)
				.setNullable()
				.setMaxValue(9999)
				.build());
	}
}
