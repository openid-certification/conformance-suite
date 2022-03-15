package net.openid.conformance.openinsurance.validator.productsNServices;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.CommonValidatorParts;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.openinsurance.validator.OpenInsuranceLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api source: swagger/openinsurance/swagger-productsnservices-autoinsurance.yaml
 * Api endpoint: src/main/java/net/openid/conformance/openinsurance/validator/productsNServices/auto-insurance
 * Api version: 1.0.2
 * Api git hash: b5dcb30363a2103b9d412bc3c79040696d2947d2
 */

@ApiName("ProductsNServices Auto Insurance")
public class GetAutoInsuranceValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> CONDITION = Sets.newHashSet("NOVAS", "USADAS", "AMBAS");
	public static final Set<String> PART_TYPE = Sets.newHashSet("ORIGINAIS", "COMPATIVEIS", "AMBAS");
	public static final Set<String> ADDITIONAL = Sets.newHashSet("SORTEIO_GRATUITO", "CLUBE_BENEFICIOS", "CASHBACK", "DESCONTOS", "OUTROS");
	public static final Set<String> SERVICE_PACKAGE = Sets.newHashSet("ATE_10_SERVICOS", "ATE_20_SERVICOS", "ACIMA_20_SERVICOS", "CUSTOMIZAVEL");
	public static final Set<String> TYPE_SIGNALING = Sets.newHashSet("GRATUITA", "PAGA" );
	public static final Set<String> TERMS = Sets.newHashSet("ANUAL",
		"ANUAL_INTERMITENTE",
		"PLURIANUAL",
		"PLURIANUAL_INTERMITENTE",
		"SEMESTRAL",
		"SEMESTRAL_INTERMITENTE",
		"MENSAL",
		"MENSAL_INTERMITENTE",
		"DIARIO",
		"DIARIO_INTERMITENTE",
		"OUTROS");
	public static final Set<String> CUSTOMER_SERVICE = Sets.newHashSet("REDE_REFERENCIADA", "LIVRE_ESCOLHA", "REDE_REFERENCIADA_LIVRE_ESCOLHA");
	public static final Set<String> PAYMENT_METHOD = Sets.newHashSet("CARTAO_CREDITO",
		"CARTAO_DEBITO",
		"DEBITO_CONTA_CORRENTE",
		"DEBITO_CONTA_POUPANCA",
		"BOLETO_BANCARIO",
		"PIX",
		"CONSIGINACAO_FOLHA_PAGAMENTO",
		"PONTOS_PROGRAMA_BENEFICIO",
		"OUTROS");
	public static final Set<String> PAYMENT_TYPE = Sets.newHashSet("A_VISTA", "PARCELADO", "AMBOS" );
	public static final Set<String> CONTRACTING_TYPE = Sets.newHashSet("COLETIVO", "INDIVIDUAL", "AMBAS"  );
	public static final Set<String> TARGET_AUDIENCE = Sets.newHashSet("PESSOA_NATURAL", "PESSOA_JURIDICA", "AMBAS" );
	public static final Set<String> GEOGRAPHIC_SCOPE_COVERAGE = Sets.newHashSet("NACIONAL", "REGIONAL", "INTERNACIONAL", "OUTROS_PAISES");
	public static final Set<String> DEDUCTIBLE_TYPES = Sets.newHashSet("NORMAL", "REDUZIDA", "ISENTA", "MAJORADA", "FLEXIVEL");
	public static final Set<String> CONTRACT_BASE_TYPE = Sets.newHashSet("VALOR_DETERMINADO", "VALOR_MERCADO", "AMBOS");
	public static final Set<String> COVERAGE = Sets.newHashSet("CASCO_COMPREENSIVA_COLISAO_INCENDIO_ROUBO_FURTO",
		"CASCO_INCENDIO_ROUBO_FURTO",
		"CASCO_ROUBO_FURTO",
		"CASCO_INCENDIO",
		"CASCO_ALAGAMENTO",
		"CASCO_COLISAO_INDENIZACAO_PARCIAL",
		"CASCO_COLISAO_INDENIZACAO_INTEGRAL",
		"RESPONSABILIDADE_CIVIL_FACULTATIVA_VEICULOS_RCFV",
		"RESPONSABILIDADE_CIVIL_FACULTATIVA_CONDUTOR_RCFC",
		"ACIDENTE_PESSOAIS_PASSAGEIROS_VEICULO",
		"ACIDENTE_PESSOAIS_PASSAGEIROS_CONDUTOR",
		"VIDROS",
		"DIARIA_INDISPONIBILIDADE",
		"LFR_LANTERNAS_FAROIS_RETROVISORES",
		"ACESSORIOS_EQUIPAMENTOS",
		"CARRO_RESERVA",
		"PEQUENOS_REPAROS",
		"RESPONSABILIDADE_CIVIL_CARTA_VERDE",
		"VOUCHER_MOBILIDADE",
		"DESPESAS_EXTRAORDINARIAS",
		"PEQUENOS_REPAROS",
		"GARANTIA_MECANICA",
		"OUTRAS");
	private final CommonValidatorParts parts;
	private static class Fields extends ProductNServicesCommonFields { }

	public GetAutoInsuranceValidator() {
		parts = new CommonValidatorParts(this);
	}

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body,
			new ObjectField
				.Builder("data").setValidator(data ->
					assertField(data,
						new ObjectField.Builder("brand")
							.setValidator(brand -> {
								assertField(brand, Fields.name().build());
								assertField(brand,
									new ObjectArrayField
										.Builder("company")
										.setValidator(this::assertCompany)
										.build());
							}).build())
				).build());
		new OpenInsuranceLinksAndMetaValidator(this).assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertCompany(JsonObject companies) {
		assertField(companies, Fields.name().build());
		assertField(companies, Fields.cnpjNumber().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("products")
				.setValidator(this::assertProducts)
				.build());
	}

	private void assertProducts(JsonObject products) {
		assertField(products, Fields.name().build());
		assertField(products, Fields.code().build());

		assertField(products,
			new ObjectArrayField
				.Builder("coverages")
				.setValidator(this::assertCoverages)
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("carParts")
				.setValidator(carParts -> {
					assertField(carParts,
						new StringField
							.Builder("carPartCondition")
							.setEnums(CONDITION)
							.build());

					assertField(carParts,
						new StringField
							.Builder("carPartType")
							.setEnums(PART_TYPE)
							.build());
				})
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("carModels")
				.setValidator(carModels -> {
					assertField(carModels,
						new StringField
							.Builder("manufacturer")
							.setMaxLength(20)
							.build());

					assertField(carModels,
						new StringField
							.Builder("model")
							.setMaxLength(20)
							.build());

					assertField(carModels,
						new IntField
							.Builder("year")
							.setMaxLength(4)
							.build());

					assertField(carModels,
						new StringField
							.Builder("fipeCode")
							.build());
				})
				.build());

		assertField(products,
			new StringField
				.Builder("vehicleOvernightZipCode")
				.setMaxLength(8)
				.build());

		assertField(products,
			new StringArrayField
				.Builder("additional")
				.setEnums(ADDITIONAL)
				.build());

		assertField(products,
			new StringField
				.Builder("additionalOthers")
				.setOptional()
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("assistanceServices")
				.setValidator(assistanceServices -> {
					assertField(assistanceServices,
						new StringArrayField
							.Builder("assistanceServicesPackage")
							.setEnums(SERVICE_PACKAGE)
							.build());

					assertField(assistanceServices,
						new StringField
							.Builder("assistanceServicesDetail")
							.setMaxLength(1000)
							.build());

					assertField(assistanceServices,
						new StringField
							.Builder("chargeTypeSignaling")
							.setEnums(TYPE_SIGNALING)
							.setOptional()
							.build());
				})
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("termsAndConditions")
				.setValidator(termsAndConditions -> {
					assertField(termsAndConditions,
						new StringField
							.Builder("susepProcessNumber")
							.setMaxLength(20)
							.build());

					assertField(termsAndConditions,
						new StringField
							.Builder("definition")
							.setMaxLength(1024)
							.build());
				})
				.build());

		assertField(products,
			new StringArrayField
				.Builder("terms")
				.setEnums(TERMS)
				.build());

		assertField(products,
			new StringArrayField
				.Builder("customerService")
				.setEnums(CUSTOMER_SERVICE)
				.build());

		assertField(products,
			new ObjectField
				.Builder("premiumPayment")
				.setValidator(premiumPayment -> {
					assertField(premiumPayment,
						new StringArrayField
							.Builder("paymentMethod")
							.setEnums(PAYMENT_METHOD)
							.build());

					assertField(premiumPayment,
						new StringArrayField
							.Builder("paymentType")
							.setEnums(PAYMENT_TYPE)
							.setOptional()
							.build());

					assertField(premiumPayment,
						new StringField
							.Builder("paymentDetail")
							.setOptional()
							.build());
				})
				.build());

		assertField(products,
			new ObjectField
				.Builder("minimumRequirements")
				.setValidator(minimumRequirements -> {
					assertField(minimumRequirements,
						new StringArrayField
							.Builder("contractingType")
							.setEnums(CONTRACTING_TYPE)
							.build());

					assertField(minimumRequirements,
						new StringField
							.Builder("contractingMinRequirement")
							.setMaxLength(1024)
							.build());
				})
				.build());

		assertField(products,
			new StringArrayField
				.Builder("targetAudiences")
				.setMaxLength(30)
				.setEnums(TARGET_AUDIENCE)
				.build());
	}

	private void assertCoverages(JsonObject coverages) {
		assertField(coverages,
			new StringField
				.Builder("coverage")
				.setEnums(COVERAGE)
				.build());

		assertField(coverages,
			new StringField
				.Builder("coverageDetail")
				.setMaxLength(1000)
				.build());

		assertField(coverages,
			new BooleanField
				.Builder("coveragePermissionSeparteAcquisition")
				.build());

		assertField(coverages,
			new ObjectField
				.Builder("coverageAttributes")
				.setValidator(this::assertCoverageAttributes)
				.setOptional()
				.build());
		}

	private void assertCoverageAttributes(JsonObject coverageAttributes) {
		assertField(coverageAttributes,
			new ObjectField
				.Builder("minLMI")
				.setValidator(parts::assertValue)
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("maxLMI")
				.setValidator(parts::assertValue)
				.build());

		assertField(coverageAttributes,
			new ObjectArrayField
				.Builder("contractBase")
				.setValidator(this::assertContractBase)
				.build());

		assertField(coverageAttributes,
			new IntField
				.Builder("newCarMaximumCalculatingPeriod")
				.setMaxLength(3)
				.build());

		assertField(coverageAttributes,
			new ObjectArrayField
				.Builder("newCardContractBase")
				.setValidator(this::assertContractBase)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("fullIndemnityPercentage")
				.setValidator(parts::assertValue)
				.build());

		assertField(coverageAttributes,
			new StringArrayField
				.Builder("deductibleType")
				.setEnums(DEDUCTIBLE_TYPES)
				.build());

		assertField(coverageAttributes,
			new BooleanField
				.Builder("fullIndemnityDeductible")
				.build());

		assertField(coverageAttributes,
			new BooleanField
				.Builder("deductiblePaymentByCoverage")
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("deductiblePercentage")
				.setValidator(parts::assertValue)
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("mandatoryParticipation")
				.setMaxLength(300)
				.build());

		assertField(coverageAttributes,
			new StringArrayField
				.Builder("geographicScopeCoverage")
				.setEnums(GEOGRAPHIC_SCOPE_COVERAGE)
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("geographicScopeCoverageOthers")
				.setOptional()
				.build());
		}

	private void assertContractBase(JsonObject contractBase) {
		assertField(contractBase,
			new StringField
				.Builder("contractBaseType")
				.setEnums(CONTRACT_BASE_TYPE)
				.build());

		assertField(contractBase,
			new ObjectField
				.Builder("contractBaseMinValue")
				.setValidator(parts::assertValue)
				.setOptional()
				.build());

		assertField(contractBase,
			new ObjectField
				.Builder("contractBaseMaxValue")
				.setValidator(parts::assertValue)
				.setOptional()
				.build());
		}
}
