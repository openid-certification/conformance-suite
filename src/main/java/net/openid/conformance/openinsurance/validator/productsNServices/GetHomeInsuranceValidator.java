package net.openid.conformance.openinsurance.validator.productsNServices;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.CommonValidatorParts;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api endpoint: /home-insurance/commercializationArea/{commercializationArea}
 * Api version: 1.0.0
 */

@ApiName("ProductsNServices Home Insurance")
public class GetHomeInsuranceValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> PROPERTY_TYPE = Sets.newHashSet("CASA", "APARTAMENTO");
	public static final Set<String> TARGET_AUDIENCES = Sets.newHashSet("PESSOA_NATURAL", "PESSOA_JURIDICA", "AMBAS");
	public static final Set<String> CONTRACTING_TYPES = Sets.newHashSet("COLETIVO", "INDIVIDUAL");
	public static final Set<String> PAYMENT_TYPES = Sets.newHashSet("A_VISTA", "PARCELADO", "AMBOS");
	public static final Set<String> CHARGE_TYPE_SIGNALING = Sets.newHashSet("GRATUITA", "PAGA");
	public static final Set<String> PAYMENT_METHODS = Sets.newHashSet("CARTAO_CREDITO", "CARTAO_DEBITO", "DEBITO_CONTA_CORRENTE", "DEBITO_CONTA_POUPANCA", "BOLETO_BANCARIO", "PIX", "CONSIGINACAO_FOLHA_PAGAMENTO", "PAGAMENTO_COM_PONTOS");
	public static final Set<String> CUSTOMER_SERVICES = Sets.newHashSet("REDE_REFERENCIADA", "LIVRE_ESCOLHA", "REDE_REFERENCIADA_LIVRE_ESCOLHA");
	public static final Set<String> TERMS = Sets.newHashSet("ANUAL", "ANUAL_INTERMITENTE", "PLURIANUAL", "PLURIANUAL_INTERMITENTE", "MENSAL", "MENSAL_INTERMITENTE", "DIARIO", "DIARIO_INTERMITENTE", "OUTROS");
	public static final Set<String> ADDITIONAL = Sets.newHashSet("SORTEIO_GRATUITO", "CLUBE_BENEFICIOS", "CASHBACK", "DESCONTOS", "OUTROS");
	public static final Set<String> SERVICES_PACKAGE = Sets.newHashSet("ATE_10_SERVICOS", "ATE_20_SERVICOS", "ACIMA_20_SERVICOS", "CUSTOMIZAVEL");
	public static final Set<String> PROPERTY_BUILD_TYPE = Sets.newHashSet("ALVENARIA", "MADEIRA", "METALICA", "MISTA");
	public static final Set<String> PROPERTY_USAGE_TYPE = Sets.newHashSet("HABITUAL", "VERANEIO", "DESOCUPADO", "CASA_ESCRITORIO", "ALUGUEL_TEMPORADA");
	public static final Set<String> IMPORTANCE_INSURED = Sets.newHashSet("PREDIO", "CONTEUDO", "AMBOS");
	public static final Set<String> COVERAGE_TYPES = Sets.newHashSet("IMOVEL_BASICA", "IMOVEL_AMPLA", "DANOS_ELETRICOS", "DANOS_POR_AGUA", "ALAGAMENTO", "RESPONSABILIDADE_CIVIL_FAMILIAR", "RESPONSABILIDADE_CIVIL_DANOS_MORAIS", "ROUBO_SUBTRACAO_BENS", "ROUBO_SUBTRACAO_BENS_FORA_LOCAL_SEGURADO", "TACOS_GOLFE_HOLE_ONE", "PEQUENAS_REFORMAS_OBRA", "GREVES_TUMULTOS_LOCKOUT", "MICROEMPREENDEDOR", "ESCRITORIO_RESIDENCIA", "DANOS_EQUIPAMENTOS_ELETRONICOS", "QUEBRA_VIDROS", "IMPACTO_VEICULOS", "VENDAVAL", "PERDA_PAGAMENTO_ALUGUEL", "BICICLETA", "RESPONSABILIDADE_CIVIL_BICICLETA", "RC_EMPREGADOR", "DESMORONAMENTO", "DESPESAS_EXTRAORDINARIAS", "JOIAS_OBRAS_ARTE", "TERREMOTO", "IMPACTO_AERONAVES", "PAISAGISMO", "INCENDIO", "QUEDA_RAIO", "EXPLOSAO", "OUTRAS");

	private final CommonValidatorParts parts;
	private static class Fields extends ProductNServicesCommonFields { }

	public GetHomeInsuranceValidator() {
		parts = new CommonValidatorParts(this);
	}

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body, new ObjectField
			.Builder("data")
			.setValidator(data -> {

				assertField(data, new ObjectField
					.Builder("brand")
					.setValidator(brand -> {
						assertField(brand, Fields.name().build());
						assertField(brand, new ObjectField
							.Builder("company")
							.setValidator(this::assertCompany)
							.setOptional()
							.build());
					})
					.setOptional().build());
			}).build());

		logFinalStatus();
		return environment;
	}

	private void assertCompany(JsonObject companies) {
		assertField(companies, Fields.name().setMaxLength(80).build());
		assertField(companies, Fields.cnpjNumber().setMaxLength(14).build());

		assertField(companies,
			new ObjectArrayField
				.Builder("products")
				.setValidator(this::assertProducts)
				.setOptional()
				.build());
	}

	private void assertProducts(JsonObject products) {
		assertField(products, Fields.name().setMaxLength(80).build());
		assertField(products, Fields.code().setMaxLength(80).build());

		assertField(products,
			new ObjectArrayField
				.Builder("coverages")
				.setValidator(this::assertCoverages)
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("propertyCharacteristics")
				.setValidator(this::assertPropertyCharacteristics)
				.build());

		assertField(products,
			new IntField
				.Builder("propertyZipCode")
				.setMaxLength(8)
				.build());

		assertField(products,
			new BooleanField
				.Builder("protective")
				.build());

		assertField(products,
			new StringField
				.Builder("additional")
				.setEnums(ADDITIONAL)
				.build());

		assertField(products,
			new StringField
				.Builder("additionalOthers")
				.setMaxLength(100)
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("assistanceServices")
				.setValidator(assistanceServices -> {
					assertField(assistanceServices,
						new StringField
							.Builder("assistanceServicesPackage")
							.setEnums(SERVICES_PACKAGE)
							.build());

					assertField(assistanceServices,
						new StringField
							.Builder("complementaryAssistanceServicesDetail")
							.setMaxLength(1000)
							.build());

					assertField(assistanceServices,
						new StringField
							.Builder("chargeTypeSignaling")
							.setEnums(CHARGE_TYPE_SIGNALING)
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
							.setOptional()
							.build());
				})
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("validity")
				.setValidator(validity -> {
					assertField(validity,
						new StringField
							.Builder("term")
							.setEnums(TERMS)
							.build());

					assertField(validity,
						new StringField
							.Builder("termOthers")
							.setMaxLength(100)
							.setOptional()
							.build());
				})
				.build());

		assertField(products,
			new StringArrayField
				.Builder("customerServices")
				.setEnums(CUSTOMER_SERVICES)
				.setOptional()
				.build());

		assertField(products,
			new StringArrayField
				.Builder("premiumRates")
				.setOptional()
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("premiumPayments")
				.setValidator(premiumPayments -> {
					assertField(premiumPayments,
						new StringField
							.Builder("paymentMethod")
							.setEnums(PAYMENT_METHODS)
							.build());

					assertField(premiumPayments,
						new StringField
							.Builder("paymentMethodDetail")
							.setMaxLength(100)
							.setOptional()
							.build());

					assertField(premiumPayments,
						new StringField
							.Builder("paymentType")
							.setEnums(PAYMENT_TYPES)
							.build());
				})
				.build());

		assertField(products,
			new ObjectField
				.Builder("minimumRequirements")
				.setValidator(minimumRequirements -> {
					assertField(minimumRequirements,
						new StringField
							.Builder("contractingType")
							.setEnums(CONTRACTING_TYPES)
							.build());

					assertField(minimumRequirements,
						new StringField
							.Builder("contractingMinRequirement")
							.setMaxLength(1024)
							.build());
				})
				.setOptional()
				.build());

		assertField(products,
			new StringArrayField
				.Builder("targetAudiences")
				.setMaxLength(30)
				.setEnums(TARGET_AUDIENCES)
				.build());
	}

	private void assertPropertyCharacteristics(JsonObject propertyCharacteristics) {
		assertField(propertyCharacteristics,
			new StringField
				.Builder("propertyType")
				.setEnums(PROPERTY_TYPE)
				.build());

		assertField(propertyCharacteristics,
			new StringField
				.Builder("propertyBuildType")
				.setEnums(PROPERTY_BUILD_TYPE)
				.build());

		assertField(propertyCharacteristics,
			new StringField
				.Builder("propertyUsageType")
				.setEnums(PROPERTY_USAGE_TYPE)
				.build());

		assertField(propertyCharacteristics,
			new StringField
				.Builder("importanceInsured")
				.setEnums(IMPORTANCE_INSURED)
				.build());
	}

	private void assertCoverages(JsonObject coverages) {
		assertField(coverages,
			new StringField
				.Builder("coverageType")
				.setMaxLength(1000)
				.setEnums(COVERAGE_TYPES)
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
			new ObjectField
				.Builder("minDeductibleAmount")
				.setValidator(parts::assertValue)
				.build());

		assertField(coverageAttributes,
			new IntField
				.Builder("insuredMandatoryParticipationPercentage")
				.setMaxLength(9)
				.build());
	}
}
