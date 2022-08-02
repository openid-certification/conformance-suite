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
 * Api endpoint: /personal/financial-relations
 * Api version: 2.0.1.final
 **/
@ApiName("Natural Person Relationship V2")
public class PersonalRelationsResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);

	public static final Set<String> ENUM_PRODUCTS_SERVICES_TYPE = SetUtils.createSet("CONTA_DEPOSITO_A_VISTA, CONTA_POUPANCA, CONTA_PAGAMENTO_PRE_PAGA, CARTAO_CREDITO, OPERACAO_CREDITO, SEGURO, PREVIDENCIA, INVESTIMENTO, OPERACOES_CAMBIO, CONTA_SALARIO, CREDENCIAMENTO, OUTROS");
	public static final Set<String> ENUM_TYPE = SetUtils.createSet("REPRESENTANTE_LEGAL, PROCURADOR");
	public static final Set<String> ENUM_TYPE1 = SetUtils.createSet("CONTA_DEPOSITO_A_VISTA, CONTA_POUPANCA, CONTA_PAGAMENTO_PRE_PAGA");
	public static final Set<String> SUBTYPES = SetUtils.createSet("INDIVIDUAL, CONJUNTA_SIMPLES, CONJUNTA_SOLIDARIA");

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
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$")
				.build());

		assertField(data,
			new DatetimeField
				.Builder("startDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$")
				.build());

		assertField(data,
			new StringArrayField
				.Builder("productsServicesType")
				.setMaxItems(12)
				.setMinItems(1)
				.setEnums(ENUM_PRODUCTS_SERVICES_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("productsServicesTypeAdditionalInfo")
				.setPattern("^[\\w\\W]*$")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("procurators")
				.setValidator(this::assertInnerFieldsProcurators)
				.setMinItems(0)
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("accounts")
				.setValidator(this::assertInnerFieldsAccounts)
				.setMinItems(0)
				.build());
	}

	private void assertInnerFieldsProcurators(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("type")
				.setEnums(ENUM_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("cpfNumber")
				.setMaxLength(11)
				.setPattern("^\\d{11}$")
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
				.setOptional()
				.build());
	}

	private void assertInnerFieldsAccounts(JsonObject data) {

		assertField(data,
			new StringField
				.Builder("compeCode")
				.setMaxLength(3)
				.setPattern("^\\d{3}$")
				.build());

		assertField(data,
			new StringField
				.Builder("branchCode")
				.setMaxLength(4)
				.setPattern("^\\d{4}$")
				.setOptional()
				.build());


		assertField(data,
			new StringField
				.Builder("number")
				.setMaxLength(20)
				.setPattern("^\\d{8,20}$")
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
