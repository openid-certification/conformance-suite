package net.openid.conformance.openbanking_brasil.productsNServices.financings;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductsNServicesCommonFields;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductsNServicesCommonValidatorParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * https://openbanking-brasil.github.io/areadesenvolvedor/swagger/swagger_products_services_apis.yaml
 * URL: /business-financings
 */

@ApiName("ProductsNServices Business Financings")
public class BusinessFinancingsValidator extends AbstractJsonAssertingCondition {

	private static class Fields extends ProductsNServicesCommonFields {}
	private final ProductsNServicesCommonValidatorParts parts;

	public BusinessFinancingsValidator() {
		parts = new ProductsNServicesCommonValidatorParts(this);
	}

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertJsonObject(body, ROOT_PATH,
			(data) -> assertField(data,
				new ObjectField.Builder("brand").setValidator(
					(brand) -> {
						assertField(brand, Fields.name().build());
						assertField(brand,
							new ObjectArrayField.Builder("companies")
								.setMinItems(1)
								.setValidator(this::assertCompanies)
								.build());
					}
				).build())
		);
		return environment;
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, Fields.cnpjNumber().build());
		assertField(companies,Fields.name().build());
		assertField(companies, Fields.urlComplementaryList().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("businessFinancings")
				.setValidator(this::assertBusinessFinancings)
				.setMinItems(1)
				.setMaxItems(9)
				.build());
	}

	private void assertBusinessFinancings(JsonObject businessFinancings) {
		Set<String> types = Sets.newHashSet("FINANCIAMENTO_AQUISICAO_BENS_VEICULOS_AUTOMOTORES",
			"FINANCIAMENTO_AQUISICAO_BENS_OUTROS_BENS", "FINANCIAMENTO_MICROCREDITO",
			"FINANCIAMENTO_RURAL_CUSTEIO", "FINANCIAMENTO_RURAL_INVESTIMENTO", "FINANCIAMENTO_RURAL_COMERCIALIZACAO",
			"FINANCIAMENTO_RURAL_INDUSTRIALIZACAO", "FINANCIAMENTO_IMOBILIARIO_SISTEMA_FINANCEIRO_HABITACAO_SFH",
			"FINANCIAMENTO_IMOBILIARIO_SISTEMA_FINANCEIRO_HABITACAO_SFI");
		Set<String> requiredWarranties = Sets.newHashSet("CESSAO_DIREITOS_CREDITORIOS", "CAUCAO",
			"PENHOR", "ALIENACAO_FIDUCIARIA", "HIPOTECA", "OPERACOES_GARANTIDAS_PELO_GOVERNO",
			"OUTRAS_GARANTIAS_NAO_FIDEJUSSORIAS", "SEGUROS_ASSEMELHADOS", "GARANTIA_FIDEJUSSORIA",
			"BENS_ARRENDADOS", "GARANTIAS_INTERNACIONAIS", "OPERACOES_GARANTIDAS_OUTRAS_ENTIDADES",
			"ACORDOS_COMPENSACAO", "NAO_APLICAVEL");

		assertField(businessFinancings, ProductsNServicesCommonFields.type(types).build());

		assertField(businessFinancings,
			new ObjectField.Builder("fees").setValidator(
				fees -> assertField(fees,
					new ObjectArrayField.Builder("services")
						.setMinItems(1).setMaxItems(9).setValidator(
							services -> {
								assertField(services, Fields.name().setMaxLength(250).build());
								assertField(services, Fields.code().build());
								assertField(services, Fields.chargingTriggerInfo().build());
								parts.assertPrices(services);
								parts.applyAssertingForCommonMinimumAndMaximum(services);
							}).build())
			).build());

		parts.applyAssertingForCommonRates(businessFinancings,"interestRates", false);

		assertField(businessFinancings,
			new StringArrayField
				.Builder("requiredWarranties")
				.setMinItems(1)
				.setMaxItems(14)
				.setEnums(requiredWarranties)
				.build());

		assertField(businessFinancings,
			new StringField
				.Builder("termsConditions")
				.setMaxLength(2000)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}
}
