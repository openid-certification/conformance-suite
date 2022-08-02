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
 * Api endpoint: /business/financial-relations
 * Api version: 2.0.1.final
 **/

@ApiName("Business Financial-relations V2")
public class BusinessRelationsResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);
	public static final Set<String> PRODUCT_SERVICE_TYPES = SetUtils.createSet("CONTA_DEPOSITO_A_VISTA, CONTA_POUPANCA, CONTA_PAGAMENTO_PRE_PAGA, CARTAO_CREDITO, OPERACAO_CREDITO, SEGURO, PREVIDENCIA, INVESTIMENTO, OPERACOES_CAMBIO, CONTA_SALARIO, CREDENCIAMENTO, OUTROS");
	public static final Set<String> TYPES = SetUtils.createSet("CONTA_DEPOSITO_A_VISTA, CONTA_POUPANCA, CONTA_PAGAMENTO_PRE_PAGA");
	public static final Set<String> PROCURATOR_TYPES = SetUtils.createSet("REPRESENTANTE_LEGAL, PROCURADOR");

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

	private void assertData(JsonObject body) {
		assertField(body,
			new DatetimeField
				.Builder("updateDateTime")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$")
				.setMaxLength(20)
				.build());

		assertField(body,
			new DatetimeField
				.Builder("startDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$")
				.setMaxLength(20)
				.build());

		assertField(body,
			new StringArrayField
				.Builder("productsServicesType")
				.setMinItems(1)
				.setMaxItems(12)
				.setEnums(PRODUCT_SERVICE_TYPES)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("procurators")
				.setValidator(this::assertInnerFieldsForProcurators)
				.setMinItems(0)
				.build());

		assertField(body,
			new ObjectArrayField.
				Builder("accounts")
				.setValidator(this::assertInnerFieldsForAccounts)
				.setMinItems(0)
				.build());
	}
	private void assertInnerFieldsForAccounts(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("compeCode")
				.setPattern("^\\d{3}$")
				.setMaxLength(3)
				.build());

		assertField(body,
			new StringField
				.Builder("branchCode")
				.setPattern("^\\d{4}$")
				.setMaxLength(4)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("number")
				.setPattern("^\\d{8,20}$")
				.setMaxLength(20)
				.build());

		assertField(body,
			new StringField
				.Builder("checkDigit")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(1)
				.build());

		assertField(body,
			new StringField
				.Builder("type")
				.setEnums(TYPES)
				.build());
	}

	private void assertInnerFieldsForProcurators(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("type")
				.setEnums(PROCURATOR_TYPES)
				.build());

		assertField(body,
			new StringField
				.Builder("cnpjCpfNumber")
				.setMaxLength(14)
				.setPattern("^\\d{11}$|^\\d{14}$")
				.build());

		assertField(body,
			new StringField
				.Builder("civilName")
				.setMaxLength(70)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("socialName")
				.setMaxLength(70)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());
	}
}
