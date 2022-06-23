package net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.v2;

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
 * Api: swagger/openinsurance/discountedCreditRights/swagger-invoice-financings.yaml
 * Api endpoint: /contracts
 * Api version: 2.0.0-RC1.0
 * Git hash:
 */
@ApiName("Invoice Financing Contracts V2")
public class InvoiceFinancingContractsResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final OpenBankingLinksAndMetaValidator linksAndMetaValidator = new OpenBankingLinksAndMetaValidator(this);

	final Set<String> PRODUCT_TYPE = SetUtils.createSet("DIREITOS_CREDITORIOS_DESCONTADOS");
	final Set<String> PRODUCT_SUB_TYPE = SetUtils.createSet("DESCONTO_DUPLICATAS, DESCONTO_CHEQUES, ANTECIPACAO_FATURA_CARTAO_CREDITO, OUTROS_DIREITOS_CREDITORIOS_DESCONTADOS, OUTROS_TITULOS_DESCONTADOS");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertField(body,
			new ObjectArrayField.Builder(ROOT_PATH)
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
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{1,100}$")
				.setMaxLength(100)
				.setMinLength(1)
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
				.setPattern("^\\d{14}$")
				.setMaxLength(14)
				.build());

		assertField(body,
			new StringField
				.Builder("productType")
				.setEnums(PRODUCT_TYPE)
				.build());

		assertField(body,
			new StringField
				.Builder("productSubType")
				.setEnums(PRODUCT_SUB_TYPE)
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
