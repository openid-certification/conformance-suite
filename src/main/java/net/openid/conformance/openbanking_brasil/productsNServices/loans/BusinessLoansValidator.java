package net.openid.conformance.openbanking_brasil.productsNServices.loans;

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
 * Api endpoint: /business-loans
 * Api version: 1.0.2
 * Api git hash: ba747ce30bdf7208a246ebf1e8a2313f85263d91
 *
 */
@ApiName("ProductsNServices Business Loans")
public class BusinessLoansValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> TYPES = Sets.newHashSet("EMPRESTIMO_MICROCREDITO_PRODUTIVO_ORIENTADO",
		"EMPRESTIMO_CHEQUE_ESPECIAL", "EMPRESTIMO_CONTA_GARANTIDA",
		"EMPRESTIMO_CAPITAL_GIRO_PRAZO_VENCIMENTO_ATE_365_DIAS",
		"EMPRESTIMO_CAPITAL_GIRO_PRAZO_VENCIMENTO_SUPERIOR_365_DIAS",
		"EMPRESTIMO_CAPITAL_GIRO_ROTATIVO");
	public static final Set<String> REQUIRED_WARRANTIES = Sets.newHashSet("CESSAO_DIREITOS_CREDITORIOS", "CAUCAO",
		"PENHOR", "ALIENACAO_FIDUCIARIA", "HIPOTECA", "OPERACOES_GARANTIDAS_PELO_GOVERNO",
		"OUTRAS_GARANTIAS_NAO_FIDEJUSSORIAS", "SEGUROS_ASSEMELHADOS",
		"GARANTIA_FIDEJUSSORIA", "BENS_ARRENDADOS", "GARANTIAS_INTERNACIONAIS",
		"OPERACOES_GARANTIDAS_OUTRAS_ENTIDADES", "ACORDOS_COMPENSACAO",
		"NAO_APLICAVEL");
	public static final Set<String> RATE_INDEXERS = Sets.newHashSet("SEM_INDEXADOR_TAXA", "PRE_FIXADO",
		"POS_FIXADO_TR_TBF", "POS_FIXADO_TJLP", "POS_FIXADO_LIBOR", "POS_FIXADO_TLP",
		"OUTRAS_TAXAS_POS_FIXADAS", "FLUTUANTES_CDI", "FLUTUANTES_SELIC",
		"OUTRAS_TAXAS_FLUTUANTES", "INDICES_PRECOS_IGPM", "INDICES_PRECOS_IPCA",
		"INDICES_PRECOS_IPCC", "OUTROS_INDICES_PRECO", "CREDITO_RURAL_TCR_PRE",
		"CREDITO_RURAL_TCR_POS", "CREDITO_RURAL_TRFC_PRE", "CREDITO_RURAL_TRFC_POS",
		"OUTROS_INDEXADORES");
	public static final Set<String> INTERVALS = Sets.newHashSet("1_FAIXA", "2_FAIXA", "3_FAIXA", "4_FAIXA");
	private final CommonValidatorParts parts;

	public BusinessLoansValidator() {
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
				.Builder("businessLoans")
				.setValidator(this::assertBusinessLoans)
				.setMinItems(1)
				.setMaxItems(6)
				.build());
	}

	private void assertBusinessLoans(JsonObject businessLoans) {

		assertField(businessLoans, CommonFields.type(TYPES).build());

		assertField(businessLoans,
			new ObjectField
				.Builder("fees")
				.setValidator(this::assertInnerFees)
				.build());

		assertField(businessLoans,
			new ObjectArrayField
				.Builder("interestRates")
				.setValidator(this::assertInterestRates)
				.setMinItems(1)
				.setOptional()
				.build());

		assertField(businessLoans,
			new StringArrayField
				.Builder("requiredWarranties")
				.setMinItems(1)
				.setMaxItems(14)
				.setEnums(REQUIRED_WARRANTIES)
				.build());

		assertField(businessLoans,
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

		assertField(interestRates,
			new StringField
				.Builder("referentialRateIndexer")
				.setEnums(RATE_INDEXERS)
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

		assertField(applications,
			new StringField
				.Builder("interval")
				.setEnums(INTERVALS)
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
		assertField(customers, CommonFields.rate().build());
	}

	private void innerIndexer(JsonObject indexer) {
		assertField(indexer, CommonFields.rate().setOptional().build());
	}

	private void assertServices(JsonObject innerServices) {
		assertField(innerServices, CommonFields.name().setMaxLength(250).build());

		assertField(innerServices, CommonFields.code().build());

		assertField(innerServices, CommonFields.chargingTriggerInfo().build());

		parts.assertPrices(innerServices);
		parts.applyAssertingForCommonMinimumAndMaximum(innerServices);
	}
}
