package net.openid.conformance.openbanking_brasil.creditOperations.financing.financingV2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openinsurance.validator.OpenBankingLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger/openinsurance/financings/swagger_financings_apis.yaml
 * URL: /contracts/
 * Api version: 2.0.0-RC1.0
 */

@ApiName("Financing V2")
public class FinancingResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final OpenBankingLinksAndMetaValidator linksAndMetaValidator = new OpenBankingLinksAndMetaValidator(this);

	public static final Set<String> ENUM_PRODUCT_TYPE = SetUtils.createSet("FINANCIAMENTOS, FINANCIAMENTOS_RURAIS, FINANCIAMENTOS_IMOBILIARIOS");
	public static final Set<String> ENUM_PRODUCT_SUB_TYPE = SetUtils.createSet("AQUISICAO_BENS_VEICULOS_AUTOMOTORES, AQUISICAO_BENS_OUTROS_BENS, MICROCREDITO, CUSTEIO, INVESTIMENTO, INDUSTRIALIZACAO, COMERCIALIZACAO, FINANCIAMENTO_HABITACIONAL_SFH, FINANCIAMENTO_HABITACIONAL_EXCETO_SFH");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertField(body,
			new ObjectArrayField
				.Builder(ROOT_PATH)
				.setValidator(this::assertInnerFields)
				.setMinItems(0)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("contractId")
				.setMaxLength(100)
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{1,100}$")
				.build());

		assertField(body,
			new StringField
				.Builder("brandName")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(80)
				.build());

		assertField(body,
			new StringField
				.Builder("companyCnpj")
				.setPattern("^\\d{14}$")
				.setMaxLength(14)
				.build());

		assertField(body,
			new StringField
				.Builder("productType")
				.setEnums(ENUM_PRODUCT_TYPE)
				.build());

		assertField(body,
			new StringField
				.Builder("productSubType")
				.setEnums(ENUM_PRODUCT_SUB_TYPE)
				.build());

		assertField(body,
			new StringField
				.Builder("ipocCode")
				.setMaxLength(67)
				.setMinLength(22)
				.setPattern("^\\d{22,67}$")
				.build());
	}
}
