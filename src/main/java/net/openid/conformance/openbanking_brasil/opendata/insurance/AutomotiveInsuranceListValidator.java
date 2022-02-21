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
 * Api endpoint: /automotives
 * Api version: 1.0.0-rc1.0
 * Git hash: f3774e4268d7cd7c8a5977a31dae8f727cc9153d
 */

@ApiName("Automotive Insurance List")
public class AutomotiveInsuranceListValidator extends AbstractJsonAssertingCondition {

	private static class Fields extends ProductNServicesCommonFields {
	}

	private final CommonOpendataParts parts;

	public AutomotiveInsuranceListValidator() {
		parts = new CommonOpendataParts(this);
	}

	public static final Set<String> TYPE = Sets.newHashSet("ROUBO_TOTAL_OU_PARCIAL", "FURTO_TOTAL_OU_PARCIAL", "ABALROAMENTO", "DESPESAS_NECESSARIAS_SOCORRO_SALVAMENTO", "DESPESAS_HIGIENIZACAO_VEÍCULO", "DESPESAS_EXTRAORDINARIAS", "DESPESAS_EXTRAORDINARIAS_MOTO", "DESPESAS_MEDICO_HOSPITALARES", "DESPESAS_ODONTOLOGICAS", "CREDITOS_CORRIDAS_APLICATIVOS_TRANSPORTE", "COBERTURA_ADICIONAL_PARA_OPCIONAIS", "DESPESAS_EXTRAS_INDENIZACAO_INTEGRAL", "DESPESAS_EXTRAS_EM_INDENIZACAO_PARCIAL", "SEGURO_GARANTIDO_CASO_INDENIZACAO_INTEGRAL", "REPARO_RAPIDO_SUPERMARTELINHO", "ISENCAO_FRANQUIA", "DESCONTO_FRANQUIA", "COBERTURA_VEICULO_REBOCADO", "DANOS_CORPORAIS_RCFV", "DANOS_MATERIAIS_RCFV", "DANOS_MORAIS_RCFV", "DANOS_ESTETICOS_RCFV", "EXTENSAO_COBERTURA_DANOS_CORPORAIS_RCFV", "DANOS_CORPORAIS_RCFC", "DANOS_MATERIAIS_RCFC", "DANOS_MORAIS_RCFC", "DANOS_ESTETICOS_RCFC", "CARTA_VERDE_DANOS_CORPORAIS", "CARTA_VERDE_DANOS_MATERIAIS", "APP_DMHO_PASSAGEIRO", "APP_INVALIDEZ_PERMANENTE_POR_PASSAGEIRO", "APP_MORTE_PASSAGEIRO", "APP_INVALIDEZ_PERMANENTE_TOTAL_PARCIAL", "VIDROS", "RETROVISORES", "FAROIS", "LANTERNAS", "DESPESAS_LOCACAO", "ROUBO_FURTO_RADIO", "ROUBO_FURTO_CD", "ROUBO_FURTO_KIT_GAS", "ROUBO_FURTO_TACOGRAFO", "TAXIMETRO", "LUMINOSO", "CARROCERIA", "EQUIPAMENTOS_ESPECIAIS_OPCIONAIS", "ACESSORIOS", "BLINDAGEM", "COBERTURA_BENS_DEIXADOS_INTERIOR_VEICULO", "COBERTURA_VEICULOS_ADAPTADOS_DEFICIENTES_FISICOS", "EIXO_ADICIONAL", "EQUIPAMENTOS", "REPARO_AIR_BAG_REPOSICAO", "COBERTURA_PARA_CHOQUE", "ENVELOPAMENTO", "DIARIA_INDISPONIBILIDADE", "MOTOR_TRANSMISSÃO", "MOTOR_TRANSMISSAO_DIRECAO_SUSPENSAO_FREIOS", "MOTOR_TRANSMISSAO_DIRECAO_SUSPENSAO_FREIOS_SISTEMA_ELETRICO_AR_CONDICIONADO", "COMPLETA", "CONFORTO", "SIMPLES", "GARANTIA_FRANQUIA_AUTOMOVEL", "OUTRAS_COBERTURAS_AUTO");
	public static final Set<String> DEDUCTIBLE_TYPES = Sets.newHashSet("NORMAL", "REDUZIDA", "ISENTA", "MAJORADA", "FLEXIVEL");
	public static final Set<String> GEOGRAPHIC_SCOPE_COVERAGE = Sets.newHashSet("NACIONAL", "REGIONAL", "INTERNACIONAL", "OUTROS_PAISES");
	public static final Set<String> PART_TYPE = Sets.newHashSet("ORIGINAIS", "COMPATIVEIS", "AMBAS");
	public static final Set<String> CONDITION = Sets.newHashSet("NOVAS", "USADAS", "AMBAS");
	public static final Set<String> ADDITIONAL = Sets.newHashSet("SORTEIO_GRATUITO", "CLUBE_BENEFICIOS", "CASHBACK", "DESCONTOS", "OUTROS");
	public static final Set<String> SERVICE_PACKAGE = Sets.newHashSet("ATE_10_SERVICOS", "ATE_20_SERVICOS", "ACIMA_20_SERVICOS", "CUSTOMIZAVEL");
	public static final Set<String> CHARGE_TYPE = Sets.newHashSet("GRATUITA", "PAGO");
	public static final Set<String> TERMS = Sets.newHashSet("ANUAL", "ANUAL_INTERMITENTE", "PLURIANUAL", "PLURIANUAL_INTERMITENTE", "SEMESTRAL", "SEMESTRAL_INTERMITENTE", "MENSAL", "MENSAL_INTERMITENTE", "DIARIO", "DIARIO_INTERMITENTE", "OUTROS");
	public static final Set<String> CUSTOMER_SERVICE = Sets.newHashSet("REDE_REFERENCIADA", "LIVRE_ESCOLHA", "REDE_REFERENCIADA_LIVRE_ESCOLHA");
	public static final Set<String> PAYMENT_METHOPAYMENT_METHODS = Sets.newHashSet("CARTAO_CREDITO", "CARTAO_DEBITO", "DEBITO_CONTA_CORRENTE", "DEBITO_CONTA_POUPANCA", "BOLETO_BANCARIO", "PIX", "CONSIGNACAO_FOLHA_PAGAMENTO", "PONTOS_PROGRAMA_BENEFÍCIO", "OUTROS");
	public static final Set<String> PAYMENT_TYPE = Sets.newHashSet("A_VISTA", "PARCELADO", "AMBOS");
	public static final Set<String> CONTRACTING_TYPE = Sets.newHashSet("COLETIVO", "INDIVIDUAL", "AMBAS");
	public static final Set<String> TARGET_AUDIENCE = Sets.newHashSet("PESSOA_NATURAL", "PESSOA_JURIDICA", "PESSOA_NATURAL_JURIDICA");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertData)
				.mustNotBeEmpty()
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
				.Builder("carParts")
				.setValidator(carParts -> {
					assertField(carParts,
						new StringField
							.Builder("condition")
							.setEnums(CONDITION)
							.setOptional()
							.build());

					assertField(carParts,
						new StringField
							.Builder("type")
							.setMaxLength(11)
							.setEnums(PART_TYPE)
							.setOptional()
							.build());
				})
				.setMinItems(1)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("carModels")
				.setValidator(carModels -> {
					assertField(carModels,
						new StringField
							.Builder("model")
							.setMaxLength(20)
							.setOptional()
							.build());

					assertField(carModels,
						new NumberField
							.Builder("year")
							.setMaxValue(9999)
							.setOptional()
							.build());

					assertField(carModels,
						new StringField
							.Builder("manufacturer")
							.setMaxLength(20)
							.setOptional()
							.build());

					assertField(carModels,
						new StringField
							.Builder("fipeCode")
							.setMaxLength(20)
							.setOptional()
							.build());
				})
				.setOptional()
				.build());

		assertField(data,
			new NumberField
				.Builder("vehicleOvernightPostalCode")
				.setMaxLength(8)
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
							.setEnums(SERVICE_PACKAGE)
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
							.setEnums(CHARGE_TYPE)
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
							.setMaxLength(20)
							.setPattern("^\\d{5}\\.\\d{6}/\\d{4}-\\d{2}$")
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
				.setEnums(TERMS)
				.setMaxLength(23)
				.build());

		assertField(data,
			new StringField
				.Builder("termAdditionalInfo")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("customerService")
				.setEnums(CUSTOMER_SERVICE)
				.setMaxLength(31)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("premiumPayment")
				.setValidator(premiumPayment -> {
					assertField(premiumPayment,
						new StringArrayField
							.Builder("paymentMethods")
							.setEnums(PAYMENT_METHOPAYMENT_METHODS)
							.setMinItems(1)
							.setMaxLength(27)
							.build());

					assertField(premiumPayment,
						new StringField
							.Builder("paymentType")
							.setEnums(PAYMENT_TYPE)
							.setMaxLength(15)
							.build());

					assertField(premiumPayment,
						new StringField
							.Builder("paymentMethodsAdditionalInfo")
							.setOptional()
							.setMaxLength(100)
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
							.setEnums(CONTRACTING_TYPE)
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
				.Builder("targetAudiences")
				.setMaxLength(23)
				.setEnums(TARGET_AUDIENCE)
				.setOptional()
				.build());
	}

	private void assertCoverages(JsonObject coverages) {
		assertField(coverages,
			new StringField
				.Builder("type")
				.setEnums(TYPE)
				.setMaxLength(75)
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
				.setValidator(this::assertAttributes)
				.build());
	}

	private void assertAttributes(JsonObject coverageAttributes) {
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
								.setMaxLength(21)
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
				.Builder("newCar")
				.setValidator(newCar -> {

					assertField(newCar,
						new ObjectField
							.Builder("contractBase")
							.setValidator(this::assertContractBase)
							.setOptional()
							.build());

					assertField(newCar,
						new IntField
							.Builder("MaximumCalculatingPeriod")
							.setMaxLength(3)
							.setOptional()
							.build());
				})
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("fullIndemnityPercentage")
				.setMaxLength(4)
				.setPattern("^\\d{1,5}\\.\\d{2}$")
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new StringArrayField
				.Builder("deductibleTypes")
				.setMaxLength(10)
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
				new StringField
						.Builder("deductiblePercentage")
						.setMaxLength(4)
						.setPattern("^\\d\\.\\d{2}$")
						.setOptional()
						.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("mandatoryParticipation")
				.setMaxLength(300)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("geographicScope")
				.setValidator(geographicScope -> {
					assertField(geographicScope,
						new StringArrayField
							.Builder("coverage")
							.setMinItems(1)
							.setMaxLength(13)
							.setEnums(GEOGRAPHIC_SCOPE_COVERAGE)
							.setOptional()
							.build());

					assertField(geographicScope,
						new StringField
							.Builder("details")
							.setMaxLength(255)
							.setOptional()
							.build());
				})
					.setOptional()
					.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("contractBase")
				.setValidator(this::assertContractBase)
				.setOptional()
				.build());
	}

	private void assertContractBase(JsonObject contractBase) {
		assertField(contractBase,
			new ObjectField
				.Builder("determinedValue")
				.setValidator(determinedValue -> {
						assertField(determinedValue,
							new ObjectField
								.Builder("min")
								.setValidator(this::assertContractBaseDeterminedValue)
								.build());

						assertField(determinedValue,
							new ObjectField
								.Builder("max")
								.setValidator(this::assertContractBaseDeterminedValue)
								.build());
					}
				)
				.setOptional()
				.build());

		assertField(contractBase,
			new ObjectField
				.Builder("marketValuePercentage")
				.setValidator(marketValuePercentage -> {
						assertField(marketValuePercentage,
							new StringField
								.Builder("min")
								.setPattern("^\\d\\.\\d{2}$")
								.build());

						assertField(marketValuePercentage,
							new StringField
								.Builder("max")
								.setPattern("^\\d\\.\\d{2}$")
								.build());
					}
				)
				.setOptional()
				.build());
	}

	private void assertContractBaseDeterminedValue(JsonObject contractBaseDeterminedValue) {
		assertField(contractBaseDeterminedValue,
			new StringField
				.Builder("amount")
				.setMaxLength(21)
				.setPattern("^\\d{1,16}\\.\\d{2,4}$")
				.build());

		assertField(contractBaseDeterminedValue,
			new StringField
				.Builder("currency")
				.setPattern("^[A-Z]{3}$")
				.setOptional()
				.build());
	}
}
