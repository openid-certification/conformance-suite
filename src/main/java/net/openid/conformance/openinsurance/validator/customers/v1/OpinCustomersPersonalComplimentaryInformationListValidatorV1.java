package net.openid.conformance.openinsurance.validator.customers.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: swagger/openinsurance/customers/v1/swagger-customer-api.yaml
 * Api endpoint: /personal/complimentary-information
 * Api version: 1.05
 **/
@ApiName("Personal Complimentary-Information V1")
public class OpinCustomersPersonalComplimentaryInformationListValidatorV1 extends AbstractJsonAssertingCondition {
	private final OpinLinksAndMetaValidator opinLinksAndMetaValidator = new OpinLinksAndMetaValidator(this);

	public static final Set<String> ENUM_PRODUCTS_SERVICES_TYPE = SetUtils.createSet("MICROSSEGUROS, TITULOS_DE_CAPITALIZACAO, SEGUROS_DE_PESSOAS, PLANOS_DE_PREVIDENCIA_COMPLEMENTAR, SEGUROS_DE_DANOS");
	public static final Set<String> ENUM_TYPE = SetUtils.createSet("REPRESENTANTE_LEGAL, PROCURADOR, NAO_SE_APLICA");

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
			new StringField
				.Builder("updateDateTime")
				.setMaxLength(20)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$")
				.build());

		assertField(data,
			new StringField
				.Builder("startDate")
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("relationshipBeginning")
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("productsServices")
				.setValidator(this::assertProductsServices)
				.setMinItems(1)
				.build());
	}

	private void assertProductsServices(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("contract")
				.setMaxLength(17)
				.build());

		assertField(data,
			new StringField
				.Builder("type")
				.setMaxLength(24)
				.setEnums(ENUM_PRODUCTS_SERVICES_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("insuranceLineCode")
				.setMaxLength(4)
				.setOptional()
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("procurators")
				.setValidator(this::assertInnerFieldsProcurators)
				.setOptional()
				.setMinItems(1)
				.build());
	}

	private void assertInnerFieldsProcurators(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("nature")
				.setEnums(ENUM_TYPE)
				.setMaxLength(19)
				.build());

		assertField(data,
			new StringField
				.Builder("cpfNumber")
				.setMaxLength(11)
				.build());

		assertField(data,
			new StringField
				.Builder("civilName")
				.setMaxLength(70)
				.build());

		assertField(data,
			new StringField
				.Builder("socialName")
				.setMaxLength(70)
				.setPattern("^[\\w\\W]*$")
				.setOptional()
				.build());
	}
}
