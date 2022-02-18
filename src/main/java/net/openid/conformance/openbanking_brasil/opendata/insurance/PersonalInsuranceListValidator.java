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
 * Api endpoint: /personals
 * Api version: 1.0.0-rc1.0
 * Git hash: f3774e4268d7cd7c8a5977a31dae8f727cc9153d
 */

@ApiName("Personal Insurance List")
public class PersonalInsuranceListValidator extends AbstractJsonAssertingCondition {
	private static class Fields extends ProductNServicesCommonFields {
	}

	private final CommonOpendataParts parts;

	public PersonalInsuranceListValidator() {
		parts = new CommonOpendataParts(this);
	}

	public static final Set<String> COVERAGE_TYPE = Sets.newHashSet("ADIANTAMENTO_DOENCA_ESTAGIO_TERMINAL", "AUXILIO_CESTA_BASICA", "AUXILIO_FINANCEIRO_IMEDIATO", "CANCELAMENTO_DE_VIAGEM", "CIRURGIA", "COBERTURA_PARA_HERNIA", "COBERTURA_PARA_LER_DORT", "CUIDADOS_PROLONGADOS_ACIDENTE", "DESEMPREGO_PERDA_DE_RENDA", "DESPESAS_EXTRA_INVALIDEZ_PERMANENTE_TOTAL_PARCIAL_ACIDENTE_DEI", "DESPESAS_EXTRA_MORTE_DEM", "DESPESAS_MEDICAS_HOSPITALARES_ODONTOLOGICAS", "DESPESAS_MEDICAS_HOSPITALARES_ODONTOLOGICAS_BRASIL", "DESPESAS_MEDICAS_HOSPITALARES_ODONTOLOGICAS_EXTERIOR", "DIARIA_INCAPACIDADE_TOTAL_TEMPORARIA", "DIARIA_INTERNACAO_HOSPITALAR", "INTERNACAO_HOSPITALAR", "DIARIAS_INCAPACIDADE_PECUNIARIA_DIP", "DOENCA_CONGENITA_FILHOS_DCF", "FRATURA_OSSEA", "DOENCAS_TROPICAIS", "INCAPACIDADE_TOTAL_OU_TEMPORARIA", "INVALIDEZ_PERMANENTE_TOTAL_PARCIAL", "INVALIDEZ_TOTAL_ACIDENTE", "INVALIDEZ_PARCIAL_ACIDENTE", "INVALIDEZ_FUNCIONAL_PERMANENTE_DOENCA", "INVALIDEZ_LABORATIVA_DOENCA", "MORTE", "MORTE_ACIDENTAL", "MORTE_CONJUGE", "MORTE_FILHOS", "MORTE_ADIATAMENTO_DOENCA_ESTAGIO_TERMINAL", "PAGAMENTO_ANTECIPADO_ESPECIAL_DOENCA_PROFISSIONAL_PAED", "PERDA_DA_AUTONOMIA_PESSOAL", "PERDA_INVOLUNTARIA_EMPREGO", "QUEIMADURA_GRAVE", "REGRESSO_ANTECIPADO_SANITARIO", "RENDA_INCAPACIDADE_TEMPORARIA", "RESCISAO_CONTRATUAL_CASO_MORTE_RCM", "RESCISAO_TRABALHISTA", "SERVICO_AUXILIO_FUNERAL", "SOBREVIVENCIA", "TRANSPLANTE_ORGAOS", "TRASLADO", "TRANSLADO_CORPO", "VERBA_RESCISORIA", "OUTROS");
	public static final Set<String> INDEMNITY_PAYMENT_METHOD = Sets.newHashSet("PAGAMENTO_CAPITAL_SEGURADO_VALOR_MONETARIO", "REEMBOLSO_DESPESAS", "PRESTACAO_SERVICOS");
	public static final Set<String> INDEMNITY_PAYMENT_FREQUENCY = Sets.newHashSet("INDENIZACAO_UNICA", "DIARIA_OU_PARCELA");
	public static final Set<String> INDEMNITY_PAYMENT_INCOME = Sets.newHashSet("CERTA", "TEMPORARIA", "TEMPORARIA_REVERSIVEL", "TEMPORARIO_MINIMO_GARANTIDO", "TEMPORARIA_REVERSIVEL_MINIMO_GARANTIDO", "VITALICIA", "VITALICIA_REVERSIVEL", "VITALICIA_MINIMO_GARANTIDO", "VITALICIA_REVERSIVEL_MINIMO_GARANTIDO");
	public static final Set<String> PERIOD = Sets.newHashSet("QUANTIDADE_DETERMINADA_PARCELAS", "ATE_FIM_CICLO_DETERMINADO");
	public static final Set<String> UNITS = Sets.newHashSet("DIAS", "MESES", "NAO_APLICA");
	public static final Set<String> RISKS = Sets.newHashSet("ATO_RECONHECIMENTO_PERIGOSO", "ATO_ILICITO_DOLOSO_PRATICADO_SEGURADO", "OPERACOES_DE_GUERRA", "FURACOES_CICLONES_TERREMOTOS", "MATERIAL_NUCLEAR", "DOENCAS_LESOES_PREEXISTENTES", "EPIDEMIAS_PANDEMIAS", "SUICIDIO", "ATO_ILICITO_DOLOSO_PRATICADO_CONTROLADOR", "OUTROS");
	public static final Set<String> ASSISTANCE_TYPE = Sets.newHashSet("ACOMPANHANTE_CASO_HOSPITALIZACAO_PROLONGADA", "ARQUITETO_VIRTUAL", "ASSESSORIA_FINANCEIRA", "AUTOMOVEL", "AUXILIO_NATALIDADE", "AVALIACAO_CLINICA_PREVENTIVA", "BOLSA_PROTEGIDA", "CESTA_BASICA", "CHECKUP_ODONTOLOGICO", "CLUBE_VANTAGENS_BENEFICIOS", "CONVALESCENCIA", "DECESSO", "DESCONTO_FARMACIAS_MEDICAMENTOS", "DESPESAS_FARMACEUTICAS_VIAGEM", "DIGITAL", "EDUCACIONAL", "EMPRESARIAL", "ENCANADOR", "ENTRETENIMENTO", "EQUIPAMENTOS_MEDICOS", "FIANCAS_DESPESAS_LEGAIS", "FISIOTERAPIA", "FUNERAL", "HELP_LINE", "HOSPEDAGEM_ACOMPANHANTE", "INTERRUPCAO_VIAGEM", "INVENTARIO", "MAIS_EM_VIDA", "MAMAE_BEBE", "MEDICA_ACIDENTE_DOENCA", "MOTOCICLETA", "MULHER", "NUTRICIONISTA", "ODONTOLOGICA", "ORIENTACAO_FITNESS", "ORIENTACAO_JURIDICA", "ORIENTACAO_NUTRICIONAL", "PERSONAL_FITNESS", "ORIENTACAO_PSICOSSOCIAL_FAMILIAR", "PERDA_ROUBO_CARTAO", "PET", "PRORROGACAO_ESTADIA", "PROTECAO_DADOS", "RECOLOCACAO_PROFISSIONAL", "REDE_DESCONTO_NUTRICIONAL", "RESIDENCIAL", "RETORNO_MENORES_SEGURADO", "SAQUE_COACAO", "SAUDE_BEM_ESTAR", "SEGUNDA_OPINIAO_MEDICA", "SENIOR", "SUSTENTAVEL_DESCARTE_ECOLOGICO", "TELEMEDICINA", "VIAGEM", "VITIMA", "OUTROS");
	public static final Set<String> TERMS = Sets.newHashSet("VITALICIA", "TEMPORARIA_PRAZO_FIXO", "TEMPORARIA_INTERMITENTE");
	public static final Set<String> UPDATE_INDEX = Sets.newHashSet("IPCA", "IGP_M", "INPC");
	public static final Set<String> ADDITIONAL = Sets.newHashSet("SORTEIO", "SERVICOS_ASSISTENCIAS_COMPLEMENTARES_PAGO", "SERVICOS_ASSISTENCIA_COMPLEMENTARES_GRATUITO", "OUTROS");
	public static final Set<String> OTHER_GUARANTEED_VALUES = Sets.newHashSet("SALDAMENTO", "BENEFICIO_PROLONGADO", "NAO_APLICA");
	public static final Set<String> PAYMENT_METHODS = Sets.newHashSet("CARTAO_CREDITO", "CARTAO_DEBITO", "DEBITO_CONTA_CORRENTE", "DEBITO_CONTA_POUPANCA", "BOLETO_BANCARIO", "PIX", "CONSIGNACAO_FOLHA_PAGAMENTO", "PONTOS_PROGRAMA_BENEFÍCIO", "OUTROS");
	public static final Set<String> FREQUENCY = Sets.newHashSet("DIARIA", "MENSAL", "UNICA", "ANUAL", "TRIMESTRAL", "SEMESTRAL", "FRACIONADO", "OUTRA");
	public static final Set<String> CONTRACTING_TYPES = Sets.newHashSet("COLETIVO", "INDIVIDUAL", "AMBAS");
	public static final Set<String> TARGET_AUDIENCES = Sets.newHashSet("PESSOA_NATURAL", "PESSOA_JURIDICA", "PESSOA_NATURAL_JURIDICA");
	public static final Set<String> MODALITY = Sets.newHashSet("FUNERAL", "PRESTAMISTA", "VIAGEM", "EDUCACIONAL", "DOTAL", "ACIDENTES_PESSOAIS", "VIDA", "PERDA_CERTIFICADO_HABILITACAO_VOO", "DOENCAS_GRAVES_DOENCA_TERMINAL", "DESEMPREGO_PERDA_RENDA", "EVENTOS_ALEATORIOS");
	public static final Set<String> CATEGORY = Sets.newHashSet("TRADICIONAL", "MICROSSEGURO");
	public static final Set<String> CRITERIAS = Sets.newHashSet("INDICE","VINCULADO_SALDO_DEVEDOR","VARIAVEL_ACORDO_CRITERIO_ESPECIFICO");
	public static final Set<String> CRITERIAS_AGE_ADJ = Sets.newHashSet("APOS_PERIODO_ANOS","CADA_PERIODO_ANOS","MUDANCA_FAIXA_ETARIA","NAO_APLICAVE");
	public static final Set<String> FIN_REGIMES = Sets.newHashSet("REPARTICAO_SIMPLES", "REPARTICAO_CAPITAIS", "CAPITALIZACAO");
	public static final Set<String> INDEMNITY_PAYMENT_METHODS = Sets.newHashSet("UNICO", "SOB_FORMA_DE_RENDA");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField.Builder("data")
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
				.setMinItems(1)
				.build());

		assertField(data,
			new StringArrayField
				.Builder("assistanceTypes")
				.setEnums(ASSISTANCE_TYPE)
				.setMaxLength(43)
				.build());

		assertField(data,
			new StringArrayField
				.Builder("assistanceAdittionalInfos")
				.setOptional()
				.build());

		assertField(data,
			new StringArrayField
				.Builder("additionals")
				.setEnums(ADDITIONAL)
				.setMaxLength(44)
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
							.setEnums(UPDATE_INDEX)
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
							.setEnums(UPDATE_INDEX)
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
				.Builder("financialRegimes")
				.setEnums(FIN_REGIMES)
				.setMaxLength(19)
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
			new BooleanField
				.Builder("allowPortability")
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("portabilityGraceTime")
				.setValidator(this::assertGracePeriod)
				.setOptional()
				.build());

		assertField(data,
			new StringArrayField
				.Builder("indemnityPaymentMethods")
				.setEnums(INDEMNITY_PAYMENT_METHODS)
				.setMaxLength(18)
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

					assertField(premiumPayments,
						new StringField
							.Builder("contributionTax")
							.setMaxLength(500)
							.setOptional()
							.build());
				})
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

	private void assertReclaim(JsonObject reclaim) {
		assertField(reclaim,
			new ObjectArrayField
				.Builder("table")
				.setValidator(reclaimTable -> {
					assertField(reclaimTable,
						new IntField
							.Builder("initialMonthRange")
							.setMaxLength(2)
							.setOptional()
							.build());

					assertField(reclaimTable,
						new IntField
							.Builder("finalMonthRange")
							.setMaxLength(2)
							.setOptional()
							.build());

					assertField(reclaimTable,
						new StringField
							.Builder("percentage")
							.setMaxLength(6)
							.setPattern("^\\d\\.\\d{4}$")
							.setOptional()
							.build());
				})
				.setOptional()
				.setMinItems(1)
				.build());

		assertField(reclaim,
			new ObjectField
				.Builder("gracePeriod")
				.setValidator(this::assertGracePeriod)
				.build());

		assertField(reclaim,
			new StringField
				.Builder("differenciatedPercentage")
				.setOptional()
				.build());
	}

	private void assertCoverages(JsonObject coverages) {
		assertField(coverages,
			new StringField
				.Builder("type")
				.setEnums(COVERAGE_TYPE)
				.setMaxLength(62)
				.build());

		assertField(coverages,
			new StringArrayField
				.Builder("typeAdditionalInfo")
				.setMaxLength(100)
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
			new StringArrayField
				.Builder("indemnityPaymentMethods")
				.setEnums(INDEMNITY_PAYMENT_METHOD)
				.setMaxLength(42)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new StringArrayField
				.Builder("indemnityPaymentFrequencies")
				.setEnums(INDEMNITY_PAYMENT_FREQUENCY)
				.setMaxLength(17)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("minValue")
				.setValidator(minValue -> {
						assertField(minValue,
							new StringField
								.Builder("amount")
								.setPattern("^\\d{1,16}\\.\\d{2,4}$")
								.setMaxLength(21)
								.build());

						assertField(minValue,
							new StringField
								.Builder("currency")
								.setPattern("^[A-Z]{3}$")
								.build());
					}
				)
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("maxValue")
				.setValidator(maxValue -> {
						assertField(maxValue,
							new StringField
								.Builder("amount")
								.setPattern("^\\d{1,16}\\.\\d{2,4}$")
								.setMaxLength(21)
								.build());

						assertField(maxValue,
							new StringField
								.Builder("currency")
								.setPattern("^[A-Z]{3}$")
								.build());
					}
				)
				.build());

		assertField(coverageAttributes,
			new StringArrayField
				.Builder("indemnifiablePeriods")
				.setMaxLength(31)
				.setEnums(PERIOD)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new IntField
				.Builder("maximumQtyIndemnifiableInstallments")
				.setMaxLength(10)
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
				.setMaxLength(10)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new IntField
				.Builder("differentiatedDeductibleDays")
				.setMaxLength(10)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("deductible")
				.setValidator(deductible -> {
						assertField(deductible,
							new StringField
								.Builder("amount")
								.setPattern("^\\d{1,16}\\.\\d{2,4}$")
								.setMaxLength(21)
								.build());

						assertField(deductible,
							new StringField
								.Builder("currency")
								.setPattern("^[A-Z]{3}$")
								.setOptional()
								.build());
					}
				)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("differentiatedDeductible")
				.setValidator(differentiatedDeductible -> {
						assertField(differentiatedDeductible,
							new StringField
								.Builder("amount")
								.setPattern("^\\d{1,16}\\.\\d{2,4}$")
								.setMaxLength(21)
								.build());

						assertField(differentiatedDeductible,
							new StringField
								.Builder("currency")
								.setPattern("^[A-Z]{3}$")
								.setOptional()
								.build());
					}
				)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new StringArrayField
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

	private void assertGracePeriod(JsonObject gracePeriod) {
		assertField(gracePeriod,
			new IntField
				.Builder("amount")
				.setMaxValue(999999999)
				.build());

		assertField(gracePeriod,
			new StringField
				.Builder("unit")
				.setEnums(UNITS)
				.setMaxLength(10)
				.build());
	}
}
