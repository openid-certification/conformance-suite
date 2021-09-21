package net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger_invoice_financings_apis.yaml
 * Api endpoint: /contracts
 * Api git hash: 127e9783733a0d53bde1239a0982644015abe4f1
 *
 */
@ApiName("Invoice Financing Contracts")
public class InvoiceFinancingContractsResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertJsonArrays(body, ROOT_PATH, this::assertInnerFields);
		
		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		final Set<String> productType = Set.of("DIREITOS_CREDITORIOS_DESCONTADOS");
		final Set<String> contractProductSubTypes = Set.of("DESCONTO_DUPLICATAS",
			"DESCONTO_CHEQUES", "ANTECIPACAO_FATURA_CARTAO_CREDITO",
			"OUTROS_DIREITOS_CREDITORIOS_DESCONTADOS", "OUTROS_TITULOS_DESCONTADOS");

		assertField(body,
			new StringField
				.Builder("contractId")
				.setMaxLength(100)
				.build());

		assertField(body,
			new StringField
				.Builder("brandName")
				.setMaxLength(80)
				.setPattern("[\\w\\W\\s]*")
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
				.setEnums(productType)
				.setMaxLength(32)
				.build());

		assertField(body,
			new StringField
				.Builder("productSubType")
				.setMaxLength(39)
				.setEnums(contractProductSubTypes)
				.build());

		assertField(body,
			new StringField
				.Builder("ipocCode")
				.setMaxLength(67)
				.build());
	}
}
