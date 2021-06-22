package net.openid.conformance.openbanking_brasil.registrationData;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.fields.ArrayField;
import net.openid.conformance.util.fields.DatetimeField;
import net.openid.conformance.util.fields.StringArrayField;
import net.openid.conformance.util.fields.StringField;

import java.util.Set;

/**
 * This is validator for API-Dados Cadastrais "Identificacao pessoa natural"
 * See <a href="https://openbanking-brasil.github.io/areadesenvolvedor/#identificacao-pessoa-natural">Identificacao pessoa natural</a>
 **/

@ApiName("Natural Person Relationship")
public class NaturalPersonRelationshipResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertHasField(body, ROOT_PATH);
		assertInnerFields(body);

		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		Set<String> enumProductsServicesType = Sets.newHashSet("CONTA_DEPOSITO_A_VISTA", "CONTA_POUPANCA", "CONTA_PAGAMENTO_PRE_PAGA", "CARTAO_CREDITO", "OPERACAO_CREDITO", "SEGURO", "PREVIDENCIA", "INVESTIMENTO", "OPERACOES_CAMBIO", "CONTA_SALARIO", "CREDENCIAMENTO", "OUTROS");
		JsonObject data = findByPath(body, "$.data").getAsJsonObject();

		assertDateTimeField(data,
			new DatetimeField
				.Builder("updateDateTime")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$")
				.build());

		assertDateTimeField(data,
			new DatetimeField
				.Builder("startDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$")
				.build());

		assertHasField(data, "productsServicesType");

		assertStringArrayField(data,
			new StringArrayField
				.Builder("productsServicesType")
				.setMaxItems(12)
				.setMinItems(1)
				.setMaxLength(24)
				.setEnum(enumProductsServicesType)
				.build());

		assertHasField(data, "procurators");

		assertArrayField(data,
			new ArrayField
				.Builder("procurators")
				.setMinItems(1)
				.build());

		assertJsonArrays(data, "procurators", this::assertInnerFieldsProcurators);


		assertHasField(data, "accounts");

		assertArrayField(data,
			new ArrayField
				.Builder("accounts")
				.setMinItems(1)
				.build());

		assertJsonArrays(data, "accounts", this::assertInnerFieldsAccounts);

	}

	private void assertInnerFieldsProcurators(JsonObject data) {

		Set<String> enumType = Sets.newHashSet("REPRESENTANTE_LEGAL", "PROCURADOR", "NAO_POSSUI");

		assertStringField(data,
			new StringField
				.Builder("type")
				.setMaxLength(19)
				.setEnums(enumType)
				.build());

		assertStringField(data,
			new StringField
				.Builder("cnpjCpfNumber")
				.setMaxLength(14)
				.setPattern("^\\d{11}$|^\\d{14}$|^NA$")
				.build());

		assertStringField(data,
			new StringField
				.Builder("civilName")
				.setMaxLength(70)
				//.setPattern("\\w*\\W*") //TODO wrong pattern
				.build());

		assertStringField(data,
			new StringField
				.Builder("socialName")
				.setMaxLength(70)
			//	.setPattern("\\w*\\W*") //TODO wrong pattern
				.build());
	}

	private void assertInnerFieldsAccounts(JsonObject data) {
		Set<String> enumType = Sets.newHashSet("CONTA_DEPOSITO_A_VISTA", "CONTA_POUPANCA", "CONTA_PAGAMENTO_PRE_PAGA", "SEM_TIPO_CONTA");


		assertStringField(data,
			new StringField
				.Builder("compeCode")
				.setMaxLength(3)
				.setPattern("\\d{3}|^NA$")
				.build());

		assertStringField(data,
			new StringField
				.Builder("branchCode")
				.setMaxLength(4)
				.setPattern("\\d{4}|^NA$")
				.build());


		assertStringField(data,
			new StringField
				.Builder("number")
				.setMaxLength(20)
				.setPattern("^\\d{8,20}$|^NA$")
				.build());

		assertStringField(data,
			new StringField
				.Builder("checkDigit")
				.setMaxLength(1)
				.setPattern("\\w*\\W*")
				.build());

		assertStringField(data,
			new StringField
				.Builder("type")
				.setEnums(enumType)
				.build());
	}
}
