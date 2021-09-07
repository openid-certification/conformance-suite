package net.openid.conformance.openbanking_brasil.productsNServices.loans;

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
 * /personal-loans
 */

@ApiName("ProductsNServices Personal Loans")
public class PersonalLoansValidator extends AbstractJsonAssertingCondition {
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
				.Builder("personalLoans")
				.setValidator(this::assertPersonalLoans)
				.setMinItems(1)
				.setMaxItems(6)
				.build());
	}

	private void assertPersonalLoans(JsonObject personalLoans) {
		Set<String> types = Sets.newHashSet("EMPRESTIMO_CREDITO_PESSOAL_CONSIGNADO", "EMPRESTIMO_CREDITO_PESSOAL_SEM_CONSIGNACAO", "EMPRESTIMO_HOME_EQUITY", "EMPRESTIMO_MICROCREDITO_PRODUTIVO_ORIENTADO", "EMPRESTIMO_CHEQUE_ESPECIAL", "EMPRESTIMO_CONTA_GARANTIDA");
		Set<String> requiredWarranties = Sets.newHashSet("CESSAO_DIREITOS_CREDITORIOS", "CAUCAO",
			"PENHOR", "ALIENACAO_FIDUCIARIA", "HIPOTECA", "OPERACOES_GARANTIDAS_PELO_GOVERNO", "OUTRAS_GARANTIAS_NAO_FIDEJUSSORIAS", "SEGUROS_ASSEMELHADOS", "GARANTIA_FIDEJUSSORIA", "BENS_ARRENDADOS", "GARANTIAS_INTERNACIONAIS", "OPERACOES_GARANTIDAS_OUTRAS_ENTIDADES", "ACORDOS_COMPENSACAO", "NAO_APLICAVEL"
		);

		assertField(personalLoans, ProductsNServicesCommonFields.type(types).build());

		assertField(personalLoans,
			new ObjectField
				.Builder("fees")
				.setValidator(this::assertInnerFees)
				.build());

		assertField(personalLoans,
			new ObjectArrayField
				.Builder("interestRates")
				.setValidator(this::assertInterestRates)
				.setMinItems(1)
				.setOptional()
				.build());

		assertField(personalLoans,
			new StringArrayField
				.Builder("requiredWarranties")
				.setMinItems(1)
				.setMaxItems(14)
				.setEnums(requiredWarranties)
				.build());

		assertField(personalLoans,
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
				.setOptional()
				.build());
	}

	private void assertInterestRates(JsonObject interestRates) {
		Set<String> rateIndexers = Sets.newHashSet("SEM_INDEXADOR_TAXA", "PRE_FIXADO",
			"POS_FIXADO_TR_TBF", "POS_FIXADO_TJLP", "POS_FIXADO_LIBOR", "POS_FIXADO_TLP",
			"OUTRAS_TAXAS_POS_FIXADAS", "FLUTUANTES_CDI", "FLUTUANTES_SELIC",
			"OUTRAS_TAXAS_FLUTUANTES", "INDICES_PRECOS_IGPM", "INDICES_PRECOS_IPCA",
			"INDICES_PRECOS_IPCC", "OUTROS_INDICES_PRECO", "CREDITO_RURAL_TCR_PRE",
			"CREDITO_RURAL_TCR_POS", "CREDITO_RURAL_TRFC_PRE", "CREDITO_RURAL_TRFC_POS",
			"OUTROS_INDEXADORES");

		assertField(interestRates,
			new StringField
				.Builder("referentialRateIndexer")
				.setEnums(rateIndexers)
				.build());

		assertField(interestRates,
			new StringField
				.Builder("rate")
				.setPattern("(^[0-1](\\.[0-9]{2})$|^NA$)")
				.setMaxLength(4)
				.build());

		assertField(interestRates,
			new ObjectArrayField
				.Builder("applications")
				.setValidator(this::assertApplications)
				.setMinItems(4)
				.setMaxItems(4)
				.build());

		assertField(interestRates,
			new StringField
				.Builder("minimumRate")
				.setPattern("(^[0-9](\\.[0-9]{4})$|^NA$)")
				.setMaxLength(6)
				.build());

		assertField(interestRates,
			new StringField
				.Builder("maximumRate")
				.setPattern("(^[0-9](\\.[0-9]{4})$|^NA$)")
				.setMaxLength(6)
				.build());
	}

	private void assertApplications(JsonObject applications) {
		Set<String> intervals = Sets.newHashSet("1_FAIXA", "2_FAIXA", "3_FAIXA", "4_FAIXA");

		assertField(applications,
			new StringField
				.Builder("interval")
				.setEnums(intervals)
				.build());


		assertField(applications,
			new ObjectField
				.Builder("indexer")
				.setValidator(this::innerIndexer)
				.build());

		assertField(applications,
			new ObjectField
				.Builder("customers")
				.setValidator(this::innerCustomers)
				.build());
	}

	private void innerCustomers(JsonObject customers) {
		assertField(customers, ProductsNServicesCommonFields.rate().build());
	}

	private void innerIndexer(JsonObject indexer) {
		assertField(indexer, ProductsNServicesCommonFields.rate().setOptional().build());
	}

	private void assertServices(JsonObject innerServices) {
		assertField(innerServices, ProductsNServicesCommonFields.name().setMaxLength(250).build());

		assertField(innerServices, ProductsNServicesCommonFields.code().build());

		assertField(innerServices, ProductsNServicesCommonFields.chargingTriggerInfo().build());

		new ProductsNServicesCommonValidatorParts(this).assertPrices(innerServices);
		new ProductsNServicesCommonValidatorParts(this).applyAssertingForCommonMinimumAndMaximum(innerServices);
	}
}
