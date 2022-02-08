package net.openid.conformance.openbanking_brasil.opendata.insurance;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.opendata.CommonOpendataParts;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;


/**
 * Api: swagger/opendata/swagger-opendata-insurance.yaml
 * Api endpoint: /homes
 * Api version: 1.0.0-rc1.0
 * Git hash: f3774e4268d7cd7c8a5977a31dae8f727cc9153d
 */

@ApiName("Homes Insurance List")
public class HomeInsuranceListValidator extends AbstractJsonAssertingCondition {

	private static class Fields extends ProductNServicesCommonFields {
	}

	private final CommonOpendataParts parts;

	public HomeInsuranceListValidator() {
		parts = new CommonOpendataParts(this);
	}

	public static final Set<String> COVERAGE_TYPE = Sets.newHashSet("IMOVEL_BASICA","IMOVEL_AMPLA","DANOS_ELETRICOS","DANOS_AGUA","ALAGAMENTO","RESPONSABILIDADE_CIVIL_FAMILIAR","RESPONSABILIDADE_CIVIL_DANOS_MORAIS","ROUBO_SUBTRACAO_BENS","ROUBO_SUBTRACAO_BENS_FORA_LOCAL_SEGURADO","TACOS_GOLFE_HOLE_ONE","PEQUENAS_REFORMAS_OBRAS","GRAVES_TUMULTOS_LOCKOUT","MICROEMPREENDEDOR","ESCRITORIO_RESIDENCIA","DANOS_EQUIPAMENTOS_ELETRONICOS","QUEBRA_VIDROS","IMPACTO_VEICULOS","VENDAVAL","PERDA_PAGAMENTO_ALUGUEL","BICICLETA","RESPONSABILIDADE_CIVIL_BICICLETA","RC_EMPREGADOR","DESMORONAMENTO","DESPESAS","JOIAS_OBRAS_ARTE","TERREMOTO","IMPACTO_AERONAVES","PAISAGISMO","INCENDIO","QUEDA_RAIO","EXPLOSAO", "OUTROS");
	public static final Set<String> PROPERTY_TYPE = Sets.newHashSet("CASA", "APARTAMENTO");
	public static final Set<String> PROPERTY_USAGE_TYPE = Sets.newHashSet("HABITUAL","VERANEIO","DESOCUPADO","CASA_ESCRITORIO","ALUGUEL_TEMPORADA");
	public static final Set<String> IMPORTANCE_INSURED = Sets.newHashSet("PREDIO", "CONTEUDO", "AMBOS");
	public static final Set<String> ADDITIONAL = Sets.newHashSet("SORTEIO_GRATUITO","CLUBE_BENEFICIOS","CASHBACK","DESCONTOS","OUTROS");
	public static final Set<String> SERVICES_PACKAGE = Sets.newHashSet("ATE_10_SERVICOS","ATE_20_SERVICOS","ACIMA_20_SERVICOS","CUSTOMIZAVEL");
	public static final Set<String> CHARGE_TYPE_SIGNALING = Sets.newHashSet("GRATUITA", "PAGO");
	public static final Set<String> TERM = Sets.newHashSet("ANUAL","ANUAL_INTERMITENTE","PLURIANUAL","PLURIANUAL_INTERMITENTE","SEMESTRAL","SEMESTRAL_INTERMITENTE","MENSAL","MENSAL_INTERMITENTE","DIARIO","DIARIO_INTERMITENTE","OUTROS");
	public static final Set<String> CUSTOMER_SERVICES = Sets.newHashSet("REDE_REFERENCIADA","LIVRE_ESCOLHA","REDE_REFERENCIADA_LIVRE_ESCOLHA");
	public static final Set<String> PAYMENT_METHODS = Sets.newHashSet("CARTAO_CREDITO","CARTAO_DEBITO","DEBITO_CONTA_CORRENTE","DEBITO_CONTA_POUPANCA","BOLETO_BANCARIO","PIX","CONSIGNACAO_FOLHA_PAGAMENTO","PONTOS_PROGRAMA_BENEFÍCIO","OUTROS");
	public static final Set<String> PAYMENT_TYPES = Sets.newHashSet("A_VISTA", "PARCELADO", "AMBOS");
	public static final Set<String> CONTRACTING_TYPES = Sets.newHashSet("COLETIVO", "INDIVIDUAL", "AMBAS");
	public static final Set<String> BUILD_TYPES = Sets.newHashSet("ALVENARIA", "MADEIRA", "MISTA");
	public static final Set<String> TARGET_AUDIENCES = Sets.newHashSet("PESSOA_NATURAL","PESSOA_JURIDICA","PESSOA_NATURAL_JURIDICA");


	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField.Builder("data")
				.setValidator(this::assertData)
				.build());

		logFinalStatus();
		return environment;
	}

	private void assertData(JsonObject data) {
		assertField(data,
			new ObjectField
				.Builder("participant")
				.setValidator(parts::assertParticipantIdentification)
				.build());

		assertField(data,
			new ObjectField
				.Builder("society")
				.setValidator(parts::assertSocietyIdentification)
				.build());

		assertField(data, Fields.name().setMaxLength(80).build());
		assertField(data, Fields.code().setMaxLength(80).build());

		assertField(data,
			new ObjectArrayField
				.Builder("coverages")
				.setValidator(this::assertCoverages)
				.setMinItems(1)
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("propertyCharacteristics")
				.setValidator(this::assertPropertyCharacteristics)
				.setMinItems(1)
				.build());

		assertField(data,
			new StringField
				.Builder("propertyPostalCode")
				.setPattern("^\\d{8}$")
				.setMaxLength(8)
				.setOptional()
				.build());

		assertField(data,
			new BooleanField
				.Builder("protective")
				.setOptional()
				.build());

		assertField(data,
			new StringArrayField
				.Builder("additionals")
				.setEnums(ADDITIONAL)
				.setMaxLength(16)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("assistanceServices")
				.setValidator(assistanceServices -> {
					assertField(assistanceServices,
						new StringField
							.Builder("package")
							.setEnums(SERVICES_PACKAGE)
							.setMaxLength(17)
							.setOptional()
							.build());

					assertField(assistanceServices,
						new StringField
							.Builder("detail")
							.setMaxLength(1000)
							.setOptional()
							.build());

					assertField(assistanceServices,
						new StringField
							.Builder("chargeType")
							.setEnums(CHARGE_TYPE_SIGNALING)
							.setMaxLength(8)
							.setOptional()
							.build());
				})
				.setMinItems(1)
				.setOptional()
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("termsAndConditions")
				.setValidator(termsAndConditions -> {
					assertField(termsAndConditions,
						new StringField
							.Builder("susepProcessNumber")
							.setPattern("^\\d{5}\\.\\d{6}/\\d{4}-\\d{2}$")
							.setMaxLength(20)
							.build());

					assertField(termsAndConditions,
						new StringField
							.Builder("detail")
							.setMaxLength(1024)
							.build());
				})
				.setMinItems(1)
				.build());

		assertField(data,
			new StringArrayField
				.Builder("terms")
				.setEnums(TERM)
				.setMaxLength(23)
				.build());

		assertField(data,
			new StringField
				.Builder("termsAdditionalInfo")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("customerService")
				.setEnums(CUSTOMER_SERVICES)
				.setMaxLength(31)
				.setOptional()
				.build());


		assertField(data,
			new ObjectField
				.Builder("premiumPayment")
				.setValidator(premiumPayments -> {
					assertField(premiumPayments,
						new StringArrayField
							.Builder("paymentMethods")
							.setEnums(PAYMENT_METHODS)
							.setMinItems(1)
							.setMaxLength(27)
							.build());

					assertField(premiumPayments,
						new StringField
							.Builder("paymentType")
							.setEnums(PAYMENT_TYPES)
							.setMaxLength(15)
							.build());

					assertField(premiumPayments,
						new StringField
							.Builder("paymentMethodsAdditionalInfo")
							.setMaxLength(100)
							.setOptional()
							.build());
				})
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("minimumRequirement")
				.setValidator(minimumRequirements -> {
					assertField(minimumRequirements,
						new StringField
							.Builder("contractType")
							.setEnums(CONTRACTING_TYPES)
							.setMaxLength(10)
							.build());

					assertField(minimumRequirements,
						new StringField
							.Builder("contractingMinRequirement")
							.setMaxLength(1024)
							.setOptional()
							.build());
				})
				.build());

		assertField(data,
			new StringField
				.Builder("targetAudience")
				.setEnums(TARGET_AUDIENCES)
				.setMaxLength(23)
				.build());
	}

	private void assertPropertyCharacteristics(JsonObject propertyCharacteristics) {
		assertField(propertyCharacteristics,
			new StringField
				.Builder("type")
				.setEnums(PROPERTY_TYPE)
				.setMaxLength(11)
				.build());

		assertField(propertyCharacteristics,
				new StringArrayField
					 .Builder("buildTypes")
					 .setEnums(BUILD_TYPES)
					 .setMaxLength(9)
					 .build());

		assertField(propertyCharacteristics,
			new StringArrayField
				.Builder("usageTypes")
				.setEnums(PROPERTY_USAGE_TYPE)
				.setMaxLength(17)
				.build());

		assertField(propertyCharacteristics,
			new StringArrayField
				.Builder("importanceInsureds")
				.setEnums(IMPORTANCE_INSURED)
				.setMaxLength(8)
				.build());
	}

	private void assertCoverages(JsonObject coverages) {
		assertField(coverages,
			new StringField
				.Builder("type")
				.setEnums(COVERAGE_TYPE)
				.setMaxLength(40)
				.build());

		assertField(coverages,
			new StringField
				.Builder("detail")
				.setMaxLength(1000)
				.setOptional()
				.build());

		assertField(coverages,
			new BooleanField
				.Builder("permissionSeparateAcquisition")
				.setOptional()
				.build());

		assertField(coverages,
			new ObjectField
				.Builder("attributes")
				.setValidator(this::assertCoverageAttributes)
				.build());
	}

	private void assertCoverageAttributes(JsonObject coverageAttributes) {
		assertField(coverageAttributes,
			new ObjectField
				.Builder("minLMI")
				.setValidator(minLMI -> {
						assertField(minLMI,
							new StringField
								.Builder("amount")
								.setPattern("^\\d{1,16}\\.\\d{2,4}$")
								.build());

					assertField(minLMI,
						new StringField
							.Builder("currency")
							.setPattern("^[A-Z]{3}$")
							.build());
					}
				)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("maxLMI")
				.setValidator(minLMI -> {
						assertField(minLMI,
							new StringField
								.Builder("amount")
								.setPattern("^\\d{1,16}\\.\\d{2,4}$")
								.setMaxLength(21)
								.build());

						assertField(minLMI,
							new StringField
								.Builder("currency")
								.setPattern("^[A-Z]{3}$")
								.build());
					}
				)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("minDeductibleAmount")
				.setValidator(minLMI -> {
						assertField(minLMI,
							new StringField
								.Builder("amount")
								.setPattern("^\\d{1,16}\\.\\d{2,4}$")
								.build());

						assertField(minLMI,
							new StringField
								.Builder("currency")
								.setPattern("^[A-Z]{3}$")
								.build());
					}
				)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("insuredMandatoryParticipationPercentage")
				.setPattern("^\\d\\.\\d{6}$")
				.setMaxLength(8)
				.setOptional()
				.build());
	}
}
