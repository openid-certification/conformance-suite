package net.openid.conformance.openbanking_brasil.creditOperations.financing;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;


import java.util.Set;

/*
 * API: swagger_financings_apis.yaml
 * URL: /contracts
 * Api git hash: 127e9783733a0d53bde1239a0982644015abe4f1
 */

@ApiName("Financing")
public class FinancingResponseValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> ENUM_PRODUCT_TYPE = Sets.newHashSet("FINANCIAMENTOS", "FINANCIAMENTOS_RURAIS", "FINANCIAMENTOS_IMOBILIARIOS");
	public static final Set<String> ENUM_PRODUCT_SUB_TYPE = Sets.newHashSet("AQUISICAO_BENS_VEICULOS_AUTOMOTORES", "AQUISICAO_BENS_OUTROS_BENS", "MICROCREDITO", "CUSTEIO", "INVESTIMENTO", "INDUSTRIALIZACAO", "COMERCIALIZACAO", "FINANCIAMENTO_HABITACIONAL_SFH", "FINANCIAMENTO_HABITACIONAL_EXCETO_SFH");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertField(body, new ObjectArrayField.Builder(ROOT_PATH).setValidator(this::assertInnerFields).build());

		return environment;
	}

	private void assertInnerFields(JsonObject body) {

		assertField(body,
			new StringField
				.Builder("contractId")
				.setMaxLength(100)
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
				.setPattern("\\d{14}|^NA$")
				.setMaxLength(14)
				.build());

		assertField(body,
			new StringField
				.Builder("productType")
				.setEnums(ENUM_PRODUCT_TYPE)
				.setMaxLength(27)
				.build());

		assertField(body,
			new StringField
				.Builder("productSubType")
				.setEnums(ENUM_PRODUCT_SUB_TYPE)
				.setMaxLength(37)
				.build());

		assertField(body,
			new StringField
				.Builder("ipocCode")
				.setMaxLength(67)
				.build());
	}
}
