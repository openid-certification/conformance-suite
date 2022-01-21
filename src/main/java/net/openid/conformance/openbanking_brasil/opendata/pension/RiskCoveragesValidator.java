package net.openid.conformance.openbanking_brasil.opendata.pension;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;


/**
 * Api url: https://sensedia.github.io/areadesenvolvedor/swagger/swagger_pension_apis.yaml
 * Api endpoint: /risk-coverages
 * Api version: 1.0.0
 */

@ApiName("Pension Risk Coverages")
public class RiskCoveragesValidator extends AbstractJsonAssertingCondition {
	private static class Fields extends ProductNServicesCommonFields {
	}
	private final CommonOpenDataParts parts;
	public RiskCoveragesValidator() {
		parts = new CommonOpenDataParts(this);
	}

	public static final Set<String> MODALITY = Sets.newHashSet("FUNERAL","PRESTAMISTA","VIAGEM","EDUCACIONAL","DOTAL","ACIDENTES_PESSOAIS","VIDA","PERDA_CERTIFICADO_HABILITACAO_VOO","DOENCAS_GRAVES_DOENCA_TERMINAL","DESEMPREGO_PERDA_RENDA","EVENTOS_ALEATORIOS","PECULIO","PENSAO_PRAZO_CERTO","PENSAO_MENORES_21","PENSAO_MENORES_24","PENSAO_CONJUGE_VITALICIA","PENSAO_CONJUGE_TEMPORARIA");
	public static final Set<String> CATEGORY = Sets.newHashSet("TRADICIONAL","MICROSSEGURO");
	public static final Set<String> PERIOD = Sets.newHashSet("VITALICIA","TEMPORARIA");
	public static final Set<String> IND_PERIOD = Sets.newHashSet("QUANTIDADE_DETERMINADA_PARCELAS","ATE_FIM_CICLO_DETERMINADO");
	public static final Set<String> TYPE = Sets.newHashSet("MORTE","INVALIDEZ");
	public static final Set<String> PROFIT_MODALITY = Sets.newHashSet("PAGAMENTO_UNICO","SOB_FORMA_DE_RENDA");
	public static final Set<String> UNIT = Sets.newHashSet( "DIAS","MESES","NAO_APLICA");
	public static final Set<String> INDEX = Sets.newHashSet( "IPCA","IGP_M","INPC");
	public static final Set<String> RISKS = Sets.newHashSet("ATO_RECONHECIMENTO_PERIGOSO","ATO_ILICITO_DOLOSO_PRATICADO_SEGURADO","OPERACOES_DE_GUERRA","FURACOES_CICLONES_TERREMOTOS","MATERIAL_NUCLEAR","DOENCAS_LESOES_PREEXISTENTES","EPIDEMIAS_PANDEMIAS","SUICIDIO","ATO_ILICITO_DOLOSO_PRATICADO_CONTROLADOR","OUTROS");
	public static final Set<String> ASSISTANCE_TYPE = Sets.newHashSet("ACOMPANHANTE_CASO_HOSPITALIZACAO_PROLONGADA","ARQUITETO_VIRTUAL","ASSESSORIA_FINANCEIRA","AUTOMOVEL","AUXILIO_NATALIDADE","AVALIACAO_CLINICA_PREVENTIVA","BOLSA_PROTEGIDA","CESTA_BASICA","CHECKUP_ODONTOLOGICO","CLUBE_VANTAGENS_BENEFICIOS","CONVALESCENCIA","DECESSO","DESCONTO_FARMACIAS_MEDICAMENTOS","DESPESAS_FARMACEUTICAS_VIAGEM","DIGITAL","EDUCACIONAL","EMPRESARIAL","ENCANADOR","ENTRETENIMENTO","EQUIPAMENTOS_MEDICOS","FIANCAS_DESPESAS_LEGAIS","FISIOTERAPIA","FUNERAL","HELP_LINE","HOSPEDAGEM_ACOMPANHANTE","INTERRUPCAO_VIAGEM","INVENTARIO","MAIS_EM_VIDA","MAMAE_BEBE","MEDICA_ACIDENTE_DOENCA","MOTOCICLETA","MULHER","NUTRICIONISTA","ODONTOLOGICA","ORIENTACAO_FITNESS","ORIENTACAO_JURIDICA","ORIENTACAO_NUTRICIONAL","PERSONAL_FITNESS","ORIENTACAO_PSICOSSOCIAL_FAMILIAR","PERDA_ROUBO_CARTAO","PET","PRORROGACAO_ESTADIA","PROTECAO_DADOS","RECOLOCACAO_PROFISSIONAL","REDE_DESCONTO_NUTRICIONAL","RESIDENCIAL","RETORNO_MENORES_SEGURADO","SAQUE_COACAO","SAUDE_BEM_ESTAR","SEGUNDA_OPINIAO_MEDICA","SENIOR","SUSTENTAVEL_DESCARTE_ECOLOGICO","TELEMEDICINA","VIAGEM","VITIMA","OUTROS");
	public static final Set<String> ADDITIONAL = Sets.newHashSet("SORTEIO","SERVICOS_ASSISTENCIAS_COMPLEMENTARES_PAGO","SERVICOS_ASSISTENCIA_COMPLEMENTARES_GRATUITO","OUTROS","NAO_HA");
	public static final Set<String> TERMS = Sets.newHashSet("VITALICIA","TEMPORARIA_PRAZO_FIXO","TEMPORARIA_INTERMITENTE");
	public static final Set<String> CRITERIAS = Sets.newHashSet("INDICE","VINCULADO_SALDO_DEVEDOR","VARIAVEL_ACORDO_CRITERIO_ESPECIFICO");
	public static final Set<String> CRITERIAS_AGE_ADJ = Sets.newHashSet("APOS_PERIODO_EM_ANOS","A_CADA_PERIODO_EM_ANOS","POR_MUDANCA_DE_FAIXA_ETARIA","NAO_APLICAVEL");
	public static final Set<String> FIN_REGIMES = Sets.newHashSet("REPARTICAO_SIMPLES","REPARTICAO_CAPITAIS","CAPITALIZACAO");
	public static final Set<String> OTHER_GUARANTEED_VALUES = Sets.newHashSet("SALDAMENTO","BENEFICIO_PROLONGADO","NAO_APLICA");
	public static final Set<String> INDEMNITY_PAYMENT_METHOD = Sets.newHashSet("UNICO","SOB_FORMA_DE_RENDA");
	public static final Set<String> INDEMNITY_PAYMENT_METHODS = Sets.newHashSet("PAGAMENTO_CAPITAL_SEGURADO_VALOR_MONETARIO","REEMBOLSO_DESPESAS","PRESTACAO_SERVICOS");
	public static final Set<String> INDEMNITY_PAYMENT_INCOME = Sets.newHashSet("CERTA","TEMPORARIA","TEMPORARIA_REVERSIVEL","TEMPORARIO_MINIMO_GARANTIDO","TEMPORARIA_REVERSIVEL_MINIMO_GARANTIDO","VITALICIA","VITALICIA_REVERSIVEL","VITALICIA_MINIMO_GARANTIDO","VITALICIA_REVERSIVEL_MINIMO_GARANTIDO");
	public static final Set<String> PAYMENT_METHODS = Sets.newHashSet("CARTAO_CREDITO","CARTAO_DEBITO","DEBITO_CONTA_CORRENTE","DEBITO_CONTA_POUPANCA","BOLETO_BANCARIO","PIX","CONSIGNACAO_FOLHA_PAGAMENTO","PONTOS_PROGRAMA_BENEFÍCIO","OUTROS");
	public static final Set<String> FREQUENCY = Sets.newHashSet("DIARIA","MENSAL","UNICA","ANUAL","TRIMESTRAL","SEMESTRAL","FRACIONADO","OUTRA");
	public static final Set<String> CONTRACT_TYPE = Sets.newHashSet("COLETIVO_AVERBADO","COLETIVO_INSTITUIDO","INDIVIDUAL");
	public static final Set<String> TARGET_AUDIENCES = Sets.newHashSet("PESSOA_NATURAL","PESSOA_JURIDICA","PESSOA_NATURAL_JURIDICA");
	public static final Set<String> PAYMENT_FREQUENCIES = Sets.newHashSet("INDENIZACAO_UNICA","DIARIA_OU_PARCELA");

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

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
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("society")
				.setValidator(termsAndConditions -> {
					assertField(termsAndConditions, Fields.name().setMaxLength(80).build());
					assertField(termsAndConditions, Fields.cnpjNumber().setPattern("^\\d{14}$").build());
				})
				.build());

		assertField(data, Fields.name().setMaxLength(80).build());
		assertField(data, Fields.code().setMaxLength(80).build());

		assertField(data,
			new StringField
				.Builder("category")
				.setEnums(CATEGORY)
				.setMaxLength(12)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("modality")
				.setEnums(MODALITY)
				.setMaxLength(33)
				.setOptional()
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("coverages")
				.setValidator(this::assertCoverages)
				.build());

		assertField(data,
			new StringArrayField
				.Builder("assistanceTypes")
				.setEnums(ASSISTANCE_TYPE)
				.setMaxLength(43)
				.build());

		assertField(data,
			new StringArrayField
				.Builder("assistanceAdditionalInfos")
				.setOptional()
				.build());

		assertField(data,
			new StringArrayField
				.Builder("additionals")
				.setEnums(ADDITIONAL)
				.setMaxLength(44)
				.build());

		assertField(data,
			new ObjectField
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
				.build());

		assertField(data,
			new BooleanField
				.Builder("globalCapital")
				.setOptional()
				.build());

		assertField(data,
			new StringArrayField
				.Builder("terms")
				.setEnums(TERMS)
				.setMaxLength(23)
				.build());

		assertField(data,
			new ObjectField
				.Builder("pmbacRemuneration")
				.setValidator(pmbacRemuneration -> {
					assertField(pmbacRemuneration,
						new StringField
							.Builder("interestRate")
							.setPattern("^\\d{1,16}\\.\\d{4}$")
							.setOptional()
							.build());

					assertField(pmbacRemuneration,
						new StringArrayField
							.Builder("updateIndexes")
							.setMaxLength(9)
							.setEnums(INDEX)
							.setOptional()
							.build());
				})
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("benefitRecalculation")
				.setValidator(benefitRecalculation -> {
					assertField(benefitRecalculation,
						new StringArrayField
							.Builder("criterias")
							.setMaxLength(35)
							.setEnums(CRITERIAS)
							.build());

					assertField(benefitRecalculation,
						new StringArrayField
							.Builder("updateIndexes")
							.setMaxLength(9)
							.setEnums(INDEX)
							.build());
				})
				.build());

		assertField(data,
			new ObjectField
				.Builder("ageAdjustment")
				.setValidator(ageAdjustment -> {
					assertField(ageAdjustment,
						new StringArrayField
							.Builder("criterias")
							.setMaxLength(27)
							.setEnums(CRITERIAS_AGE_ADJ)
							.setOptional()
							.build());

					assertField(ageAdjustment,
						new IntField
							.Builder("frequency")
							.setMaxLength(3)
							.build());
				})
				.setOptional()
				.build());

		assertField(data,
			new StringArrayField
				.Builder("financialRegimeContractTypes")
				.setEnums(FIN_REGIMES)
				.setMaxLength(29)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("reclaim")
				.setValidator(this::assertReclaim)
				.setOptional()
				.build());

		assertField(data,
			new StringArrayField
				.Builder("otherGuaranteedValues")
				.setEnums(OTHER_GUARANTEED_VALUES)
				.setMaxLength(20)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("profitModality")
				.setEnums(PROFIT_MODALITY)
				.setMaxLength(19)
				.setOptional()
				.build());

		assertField(data,
			new BooleanField
				.Builder("allowPortability")
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("portabilityGraceTime")
				.setValidator(gracePeriod->{
					assertField(gracePeriod,
						new NumberField
							.Builder("amount")
							.setMaxValue(9999999)
							.build());

					assertField(gracePeriod,
						new StringField
							.Builder("unit")
							.setEnums(UNIT)
							.setMaxLength(10)
							.build());
				})
				.setOptional()
				.build());

		assertField(data,
			new StringArrayField
				.Builder("indemnityPaymentMethods")
				.setEnums(INDEMNITY_PAYMENT_METHOD)
				.setMaxLength(18)
				.setOptional()
				.build());

		assertField(data,
			new StringArrayField
				.Builder("indemnityPaymentIncomes")
				.setEnums(INDEMNITY_PAYMENT_INCOME)
				.setMaxLength(38)
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
						new StringArrayField
							.Builder("frequencies")
							.setEnums(FREQUENCY)
							.setMaxLength(10)
							.setMinItems(1)
							.build());
				})
				.build());

		assertField(data,
			new StringField
				.Builder("contributionTax")
				.setMaxLength(500)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("minimumRequirement")
				.setValidator(minimumRequirements -> {
					assertField(minimumRequirements,
						new StringField
							.Builder("contractType")
							.setEnums(CONTRACT_TYPE)
							.setMaxLength(19)
							.build());

					assertField(minimumRequirements,
						new StringField
							.Builder("contractingMinRequirement")
							.setMaxLength(1024)
							.setOptional()
							.build());
				})
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("targetAudience")
				.setEnums(TARGET_AUDIENCES)
				.setMaxLength(23)
				.build());
	}

	private void assertCoverages(JsonObject coverages) {
		assertField(coverages,
			new StringField
				.Builder("type")
				.setMaxLength(9)
				.setEnums(TYPE)
				.build());

		assertField(coverages,
			new StringArrayField
				.Builder("additionalInfos")
				.setOptional()
				.build());

		assertField(coverages,
			new ObjectField
				.Builder("attributes")
				.setValidator(this::assertAttributes)
				.build());

		assertField(coverages,
			new StringField
				.Builder("coveragePeriod")
				.setMaxLength(10)
				.setEnums(PERIOD)
				.setOptional()
				.build());
	}

	private void assertAttributes(JsonObject coverageAttributes) {
		assertField(coverageAttributes,
			new StringArrayField
				.Builder("indemnityPaymentMethods")
				.setEnums(INDEMNITY_PAYMENT_METHODS)
				.setMaxLength(42)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new StringArrayField
				.Builder("indemnityPaymentFrequencies")
				.setEnums(PAYMENT_FREQUENCIES)
				.setMaxLength(17)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new IntField
				.Builder("indemnifiableDeadline")
				.setMaxValue(9999)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("minValue")
				.setValidator(minValue ->
					assertField(minValue,
					new ObjectField
						.Builder("currencyValue")
						.setValidator(this::assertCurrencyValue)
						.setOptional()
						.build())
				)
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("maxValue")
				.setValidator(maxValue ->
					assertField(maxValue,
					new ObjectField
						.Builder("currencyValue")
						.setValidator(this::assertCurrencyValue)
						.setOptional()
						.build())
				)
				.build());

		assertField(coverageAttributes,
			new StringArrayField
				.Builder("indemnifiablePeriods")
				.setMaxLength(31)
				.setEnums(IND_PERIOD)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new IntField
				.Builder("maximumQtyIndemnifiableInstallments")
				.setMaxLength(999999999)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("gracePeriod")
				.setValidator(this::assertGracePeriod)
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("differentiatedGracePeriod")
				.setValidator(this::assertGracePeriod)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new IntField
				.Builder("deductibleDays")
				.setMaxValue(999999999)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new IntField
				.Builder("differentiatedDeductibleDays")
				.setMaxValue(999999999)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("deductible")
				.setValidator(deductible -> assertField(deductible,
					new ObjectField
						.Builder("currencyValue")
						.setValidator(this::assertCurrencyValue)
						.setOptional()
						.build())
				)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("differentiatedDeductible")
				.setValidator(differentiatedDeductible ->
					assertField(differentiatedDeductible,
					new ObjectField
						.Builder("currencyValue")
						.setValidator(this::assertCurrencyValue)
						.setOptional()
						.build())
				)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("excludedRisks")
				.setEnums(RISKS)
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("excludedRisksURL")
				.setMaxLength(1024)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new BooleanField
				.Builder("allowApartPurchase")
				.setOptional()
				.build());
	}

	private void assertCurrencyValue(JsonObject currencyValue) {
		assertField(currencyValue,
			new StringField
				.Builder("amount")
				.setMaxLength(21)
				.setPattern("^\\d{1,16}\\.\\d{2,4}$")
				.build());

		assertField(currencyValue,
			new StringField
				.Builder("currency")
				.setPattern("^[A-Z]{3}$")
				.build());
	}

	private void assertGracePeriod(JsonObject gracePeriod) {
		assertField(gracePeriod,
			new NumberField
				.Builder("amount")
				.setMaxValue(999999999)
				.build());

		assertField(gracePeriod,
			new StringField
				.Builder("unit")
				.setEnums(UNIT)
				.setMaxLength(10)
				.build());
	}

	private void assertReclaim(JsonObject reclaim) {
		assertField(reclaim,
			new ObjectField
				.Builder("table")
				.setValidator(table -> {
					assertField(table,
						new IntField
							.Builder("initialMonthRange")
							.setMinValue(1)
							.setMaxValue(12)
							.setOptional()
							.build());

					assertField(table,
						new IntField
							.Builder("finalMonthRange")
							.setMinValue(1)
							.setMaxValue(12)
							.setOptional()
							.build());

					assertField(table,
						new StringField
							.Builder("percentage")
							.setPattern("^[0-1]\\.\\d{4}$")
							.setOptional()
							.build());
				})
				.setOptional()
				.build());

		assertField(reclaim,
			new StringField
				.Builder("differentiatedPercentage")
				.setMaxLength(1024)
				.setOptional()
				.build());

		assertField(reclaim,
			new ObjectField
				.Builder("gracePeriod")
				.setValidator(gracePeriod-> {
					assertField(gracePeriod,
						new NumberField
							.Builder("amount")
							.setMaxValue(999999999)
							.build());

					assertField(gracePeriod,
						new StringField
							.Builder("unit")
							.setEnums(UNIT)
							.setMaxLength(10)
							.build());
				})
				.setOptional()
				.build());
	}
}
