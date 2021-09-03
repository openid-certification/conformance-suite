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
 * /personal-financings
 */

@ApiName("ProductsNServices Personal Financings")
public class PersonalFinancingsValidator extends AbstractJsonAssertingCondition {
	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body,
			new ObjectField
				.Builder(ROOT_PATH)
				.setValidator(this::assertInnerFields)
				.build());
		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		assertField(body,
			new ObjectField
				.Builder("brand")
				.setValidator(this::assertBrandFields)
				.build());
	}

	private void assertBrandFields(JsonObject brand) {
		assertField(brand, ProductsNServicesCommonFields.name().build());

		assertField(brand,
			new ObjectArrayField
				.Builder("companies")
				.setValidator(this::assertCompanies)
				.setMinItems(1)
				.build());
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, ProductsNServicesCommonFields.cnpjNumber().build());
		assertField(companies, ProductsNServicesCommonFields.name().build());
		assertField(companies, ProductsNServicesCommonFields.urlComplementaryList().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("personalFinancings")
				.setValidator(this::assertpersonalFinancings)
				.setMinItems(1)
				.setMaxItems(9)
				.build());
	}

	private void assertpersonalFinancings(JsonObject personalFinancings) {
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

		assertField(personalFinancings, ProductsNServicesCommonFields.type(types).build());

		assertField(personalFinancings,
			new ObjectField
				.Builder("fees")
				.setValidator(this::assertInnerFees)
				.build());

		new ProductsNServicesCommonValidatorParts(this).applyAssertingForCommonRates(personalFinancings,
			"interestRates", false);

		assertField(personalFinancings,
			new StringArrayField
				.Builder("requiredWarranties")
				.setMinItems(1)
				.setMaxItems(14)
				.setEnums(requiredWarranties)
				.build());

		assertField(personalFinancings,
			new StringField
				.Builder("termsConditions")
				.setMaxLength(2000)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}

	private void assertInnerFees(JsonObject innerFees) {
		assertField(innerFees,
			new ObjectArrayField
				.Builder("services")
				.setValidator(this::assertServices)
				.setMinItems(1)
				.build());
	}

	private void assertServices(JsonObject innerServices) {
		assertField(innerServices, ProductsNServicesCommonFields.name().setMaxLength(250).build());

		assertField(innerServices, ProductsNServicesCommonFields.code().build());

		assertField(innerServices, ProductsNServicesCommonFields.chargingTriggerInfo().build());

		new ProductsNServicesCommonValidatorParts(this).assertPrices(innerServices);
		new ProductsNServicesCommonValidatorParts(this).applyAssertingForCommonMinimumAndMaximum(innerServices);
	}
}
