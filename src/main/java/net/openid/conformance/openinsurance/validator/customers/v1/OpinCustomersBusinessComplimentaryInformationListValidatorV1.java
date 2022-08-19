package net.openid.conformance.openinsurance.validator.customers.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: swagger/openinsurance/customers/v1/swagger-customer-api.yaml
 * Api endpoint: /business/complimentary-information
 * Api version: 1.05
 **/

@ApiName("Business Complimentary-Information V1")
public class OpinCustomersBusinessComplimentaryInformationListValidatorV1 extends AbstractJsonAssertingCondition {
	private final OpinLinksAndMetaValidator opinLinksAndMetaValidator = new OpinLinksAndMetaValidator(this);
	public static final Set<String> PRODUCT_SERVICE_TYPES = SetUtils.createSet("MICROSSEGUROS, TITULOS_DE_CAPITALIZACAO, SEGUROS_DE_PESSOAS, PLANOS_DE_PREVIDENCIA_COMPLEMENTAR, SEGUROS_DE_DANOS");
	public static final Set<String> NATURE = SetUtils.createSet("REPRESENTANTE_LEGAL, PROCURADOR, NAO_SE_APLICA");

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

	private void assertData(JsonObject body) {
		assertField(body,
			new DatetimeField
				.Builder("updateDateTime")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$")
				.setMaxLength(20)
				.build());

		assertField(body,
			new StringField
				.Builder("startDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("relationshipBeginning")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.build());

		assertField(body,
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
				.setEnums(PRODUCT_SERVICE_TYPES)
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
				.setValidator(this::assertInnerFieldsForProcurators)
				.setOptional()
				.setMinItems(1)
				.build());
	}

	private void assertInnerFieldsForProcurators(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("nature")
				.setEnums(NATURE)
				.setMaxLength(19)
				.build());

		assertField(body,
			new StringField
				.Builder("cpfNumber")
				.setMaxLength(11)
				.build());

		assertField(body,
			new StringField
				.Builder("civilName")
				.setMaxLength(70)
				.build());

		assertField(body,
			new StringField
				.Builder("socialName")
				.setMaxLength(70)
				.setPattern("^[\\w\\W]*$")
				.setOptional()
				.build());
	}
}
