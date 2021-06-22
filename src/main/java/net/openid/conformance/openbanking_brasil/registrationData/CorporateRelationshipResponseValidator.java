package net.openid.conformance.openbanking_brasil.registrationData;

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
 * This is validator for API-Dados Cadastrais "Relacionamento Pessoa Jurídica"
 * See <a href="https://openbanking-brasil.github.io/areadesenvolvedor/?java#relacionamento-pessoa-natural">Relacionamento Pessoa Jurídica</a>
 **/

@ApiName("Corporate Relationship")
public class CorporateRelationshipResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		JsonObject data = findByPath(body, ROOT_PATH).getAsJsonObject();
		assertData(data);

		return environment;
	}

	private void assertData(JsonObject body) {
		final Set<String> productServiceTypes = Set.of("CONTA_DEPOSITO_A_VISTA",
			"CONTA_POUPANCA", "CONTA_PAGAMENTO_PRE_PAGA", "CARTAO_CREDITO", "OPERACAO_CREDITO",
			"SEGURO", "PREVIDENCIA", "INVESTIMENTO", "OPERACOES_CAMBIO", "CONTA_SALARIO",
			"CREDENCIAMENTO", "OUTROS");

		assertDateTimeField(body, new DatetimeField.Builder("updateDateTime").build());
		assertDateTimeField(body, new DatetimeField.Builder("startDate").build());

		assertStringArrayField(body,
			new StringArrayField
				.Builder("productsServicesType")
				.setMinItems(1)
				.setMaxItems(12)
				.setEnum(productServiceTypes)
				.setMaxLength(24)
				.build());

		assertProcurators(body);
		assertAccounts(body);
	}

	private void assertAccounts(JsonObject body) {
		assertHasField(body, "accounts");

		assertArrayField(body,
			new ArrayField.Builder("accounts")
				.setMinItems(1)
				.build());

		assertJsonArrays(body, "accounts",
			this::assertInnerFieldsForAccounts);
	}

	private void assertInnerFieldsForAccounts(JsonObject body) {
		final Set<String> types = Set.of("CONTA_DEPOSITO_A_VISTA", "CONTA_POUPANCA",
			"CONTA_PAGAMENTO_PRE_PAGA", "SEM_TIPO_CONTA");

		assertStringField(body,
			new StringField
				.Builder("compeCode")
				.setPattern("\\d{3}|^NA$")
				.setMaxLength(3)
				.build());

		assertStringField(body,
			new StringField
				.Builder("branchCode")
				.setPattern("\\d{4}|^NA$")
				.setMaxLength(4)
				.build());

		assertStringField(body,
			new StringField
				.Builder("number")
				.setPattern("^\\d{8,20}$|^NA$")
				.setMaxLength(20)
				.build());

		assertStringField(body,
			new StringField
				.Builder("checkDigit")
				.setPattern("\\w*\\W*")
				.setMaxLength(1)
				.build());

		assertStringField(body,
			new StringField
				.Builder("type")
				.setEnums(types)
				.build());
	}

	private void assertProcurators(JsonObject body) {
		assertHasField(body, "procurators");

		assertArrayField(body,
			new ArrayField.Builder("procurators")
				.setMinItems(1)
				.build());

		assertJsonArrays(body, "procurators",
			this::assertInnerFieldsForProcurators);
	}

	private void assertInnerFieldsForProcurators(JsonObject body) {
		final Set<String> types = Set.of("REPRESENTANTE_LEGAL", "PROCURADOR", "NAO_POSSUI");

		assertStringField(body,
			new StringField
				.Builder("type")
				.setMaxLength(19)
				.setEnums(types)
				.build());

		assertStringField(body,
			new StringField
				.Builder("cnpjCpfNumber")
				.setMaxLength(14)
				.setPattern("^\\d{11}$|^\\d{14}$|^NA$")
				.build());

		assertStringField(body,
			new StringField
				.Builder("civilName")
				.setMaxLength(70)
				//.setPattern("\\w*\\W*")TODO:wrong pattern
				.build());

		assertStringField(body,
			new StringField
				.Builder("socialName")
				.setMaxLength(70)
				//.setPattern("\\w*\\W*")TODO:wrong pattern
				.build());
	}
}
