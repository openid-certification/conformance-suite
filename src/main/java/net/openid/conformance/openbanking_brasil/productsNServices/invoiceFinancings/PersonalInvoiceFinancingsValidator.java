package net.openid.conformance.openbanking_brasil.productsNServices.invoiceFinancings;

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
 *
 * /personal-invoice-financings
 */

@ApiName("ProductsNServices Personal Invoice Financings")
public class PersonalInvoiceFinancingsValidator extends AbstractJsonAssertingCondition {

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
				.Builder("personalInvoiceFinancings")
				.setValidator(this::assertPersonalInvoiceFinancings)
				.setMinItems(1)
				.setMaxItems(5)
				.build());
	}

	private void assertPersonalInvoiceFinancings(JsonObject personalInvoiceFinancings) {
		Set<String> types = Sets.newHashSet("DESCONTO_DUPLICATAS", "DESCONTO_CHEQUES",
			"ANTECIPACAO_FATURA_CARTAO_CREDITO", "OUTROS_DIREITOS_CREDITORIOS_DESCONTADOS",
			"OUTROS_TITULOS_DESCONTADOS");
		Set<String> requiredWarranties = Sets.newHashSet("CESSAO_DIREITOS_CREDITORIOS", "CAUCAO",
			"PENHOR", "ALIENACAO_FIDUCIARIA", "HIPOTECA", "OPERACOES_GARANTIDAS_PELO_GOVERNO",
			"OUTRAS_GARANTIAS_NAO_FIDEJUSSORIAS", "SEGUROS_ASSEMELHADOS", "GARANTIA_FIDEJUSSORIA",
			"BENS_ARRENDADOS", "GARANTIAS_INTERNACIONAIS", "OPERACOES_GARANTIDAS_OUTRAS_ENTIDADES",
			"ACORDOS_COMPENSACAO", "NAO_APLICAVEL");

		assertField(personalInvoiceFinancings, ProductsNServicesCommonFields.type(types).build());

		assertField(personalInvoiceFinancings,
			new ObjectField
				.Builder("fees")
				.setValidator(this::assertInnerFees)
				.build());

		new ProductsNServicesCommonValidatorParts(this).applyAssertingForCommonRates(personalInvoiceFinancings,
			"interestRates", false);

		assertField(personalInvoiceFinancings,
			new StringArrayField
				.Builder("requiredWarranties")
				.setMinItems(1)
				.setMaxItems(14)
				.setEnums(requiredWarranties)
				.build());

		assertField(personalInvoiceFinancings,
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
