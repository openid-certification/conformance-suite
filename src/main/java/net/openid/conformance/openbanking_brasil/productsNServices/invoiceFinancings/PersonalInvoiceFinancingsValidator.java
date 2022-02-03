package net.openid.conformance.openbanking_brasil.productsNServices.invoiceFinancings;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.CommonFields;
import net.openid.conformance.openbanking_brasil.productsNServices.CommonValidatorParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/91e2ff8327cb35eb1ae571c7b2264e6173b34eeb/swagger/swagger_products_services_apis.yaml
 * Api endpoint: /personal-invoice-financings
 * Api version: 1.0.2
 * Api git hash: ba747ce30bdf7208a246ebf1e8a2313f85263d91
 *
 */
@ApiName("ProductsNServices Personal Invoice Financings")
public class PersonalInvoiceFinancingsValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> TYPES = Sets.newHashSet("DESCONTO_DUPLICATAS", "DESCONTO_CHEQUES",
		"ANTECIPACAO_FATURA_CARTAO_CREDITO", "OUTROS_DIREITOS_CREDITORIOS_DESCONTADOS",
		"OUTROS_TITULOS_DESCONTADOS");
	public static final Set<String> REQUIRED_WARRANTIES = Sets.newHashSet("CESSAO_DIREITOS_CREDITORIOS", "CAUCAO",
		"PENHOR", "ALIENACAO_FIDUCIARIA", "HIPOTECA", "OPERACOES_GARANTIDAS_PELO_GOVERNO",
		"OUTRAS_GARANTIAS_NAO_FIDEJUSSORIAS", "SEGUROS_ASSEMELHADOS", "GARANTIA_FIDEJUSSORIA",
		"BENS_ARRENDADOS", "GARANTIAS_INTERNACIONAIS", "OPERACOES_GARANTIDAS_OUTRAS_ENTIDADES",
		"ACORDOS_COMPENSACAO", "NAO_APLICAVEL");
	private final CommonValidatorParts parts;

	public PersonalInvoiceFinancingsValidator() {
		parts = new CommonValidatorParts(this);
	}

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertField(body, new ObjectField.Builder(ROOT_PATH).setValidator(
			data -> assertField(data, new ObjectField.Builder("brand").setValidator(
				brand -> {
					assertField(brand, CommonFields.name().build());
					assertField(brand,
						new ObjectArrayField.Builder("companies")
							.setMinItems(1)
							.setValidator(this::assertCompanies)
							.build());}
			).build())
		).build());
		logFinalStatus();
		return environment;
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, CommonFields.cnpjNumber().build());
		assertField(companies, CommonFields.name().build());
		assertField(companies, CommonFields.urlComplementaryList().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("personalInvoiceFinancings")
				.setValidator(this::assertPersonalInvoiceFinancings)
				.setMinItems(1)
				.setMaxItems(5)
				.build());
	}

	private void assertPersonalInvoiceFinancings(JsonObject personalInvoiceFinancings) {

		assertField(personalInvoiceFinancings, CommonFields.type(TYPES).build());

		assertField(personalInvoiceFinancings,
			new ObjectField
				.Builder("fees")
				.setValidator(this::assertInnerFees)
				.build());

		parts.applyAssertingForCommonRates(personalInvoiceFinancings,
			"interestRates", false);

		assertField(personalInvoiceFinancings,
			new StringArrayField
				.Builder("requiredWarranties")
				.setMinItems(1)
				.setMaxItems(14)
				.setEnums(REQUIRED_WARRANTIES)
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
		assertField(innerServices, CommonFields.name().setMaxLength(250).build());

		assertField(innerServices, CommonFields.code().build());

		assertField(innerServices, CommonFields.chargingTriggerInfo().build());

		parts.assertPrices(innerServices);
		parts.applyAssertingForCommonMinimumAndMaximum(innerServices);
	}
}
