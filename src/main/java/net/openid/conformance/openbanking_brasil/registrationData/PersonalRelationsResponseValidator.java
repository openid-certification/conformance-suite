package net.openid.conformance.openbanking_brasil.registrationData;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 *  * API: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_accounts_apis.yaml
 *  * URL: /personal/financial-relations
 *  * Api git hash: 152a9f02d94d612b26dbfffb594640f719e96f70
 **/
@ApiName("Natural Person Relationship")
public class PersonalRelationsResponseValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> ENUM_PRODUCTS_SERVICES_TYPE = Sets.newHashSet("CONTA_DEPOSITO_A_VISTA", "CONTA_POUPANCA", "CONTA_PAGAMENTO_PRE_PAGA", "CARTAO_CREDITO", "OPERACAO_CREDITO", "SEGURO", "PREVIDENCIA", "INVESTIMENTO", "OPERACOES_CAMBIO", "CONTA_SALARIO", "CREDENCIAMENTO", "OUTROS");
	public static final Set<String> ENUM_TYPE = Sets.newHashSet("REPRESENTANTE_LEGAL", "PROCURADOR", "NAO_SE_APLICA");
	public static final Set<String> ENUM_TYPE1 = Sets.newHashSet("CONTA_DEPOSITO_A_VISTA", "CONTA_POUPANCA", "CONTA_PAGAMENTO_PRE_PAGA", "SEM_TIPO_CONTA");
	public static final Set<String> SUBTYPES = Sets.newHashSet("INDIVIDUAL", "CONJUNTA_SIMPLES", "CONJUNTA_SOLIDARIA", "SEM_SUB_TIPO_CONTA");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertHasField(body, ROOT_PATH);
		assertInnerFields(body);

		return environment;
	}

	private void assertInnerFields(JsonElement body) {
		JsonObject data = findByPath(body, "$.data").getAsJsonObject();

		assertField(data,
			new DatetimeField
				.Builder("updateDateTime")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$")
				.build());

		assertField(data,
			new DatetimeField
				.Builder("startDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$")
				.build());

		assertHasField(data, "productsServicesType");

		assertField(data,
			new StringArrayField
				.Builder("productsServicesType")
				.setMaxItems(12)
				.setMinItems(1)
				.setMaxLength(24)
				.setEnums(ENUM_PRODUCTS_SERVICES_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("productsServicesTypeAdditionalInfo")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertHasField(data, "procurators");

		assertField(data,
			new ObjectArrayField
				.Builder("procurators")
				.setValidator(this::assertInnerFieldsProcurators)
				.setMinItems(1)
				.build());

		assertHasField(data, "accounts");

		assertField(data,
			new ObjectArrayField
				.Builder("accounts")
				.setValidator(this::assertInnerFieldsAccounts)
				.setMinItems(1)
				.build());
	}

	private void assertInnerFieldsProcurators(JsonObject data) {

		assertField(data,
			new StringField
				.Builder("type")
				.setMaxLength(19)
				.setEnums(ENUM_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("cpfNumber")
				.setMaxLength(14)
				.setPattern("^\\d{11}$|^NA$")
				.build());

		assertField(data,
			new StringField
				.Builder("civilName")
				.setMaxLength(70)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(data,
			new StringField
				.Builder("socialName")
				.setMaxLength(70)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}

	private void assertInnerFieldsAccounts(JsonObject data) {

		assertField(data,
			new StringField
				.Builder("compeCode")
				.setMaxLength(3)
				.setPattern("\\d{3}|^NA$")
				.build());

		assertField(data,
			new StringField
				.Builder("branchCode")
				.setMaxLength(4)
				.setPattern("\\d{4}|^NA$")
				.build());


		assertField(data,
			new StringField
				.Builder("number")
				.setMaxLength(20)
				.setPattern("^\\d{8,20}$|^NA$")
				.build());

		assertField(data,
			new StringField
				.Builder("checkDigit")
				.setMaxLength(1)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(data,
			new StringField
				.Builder("type")
				.setEnums(ENUM_TYPE1)
				.build());

		assertField(data,
			new StringField
				.Builder("subtype")
				.setEnums(SUBTYPES)
				.build());
	}
}
