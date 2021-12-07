package net.openid.conformance.openinsurance.validator.productsNServices;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.CommonValidatorParts;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

import static net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields.CURRENCY;
import static net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields.EXCLUDED_RISKS;

/**
 * Api endpoint: /pension-plan/
 * Api version: 1.0.0
 */

@ApiName("ProductsNServices PensionPlan")
public class GetPensionPlanValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> MODALITY = Sets.newHashSet("PECULIO", "RENDA", "PENSAO_PRAZO_CERTO", "PENSAO_MENORES_21", "PENSAO_MENORES_24", "PENSAO_CONJUGE_VITALICIA", "PENSAO_CONJUGE_TEMPORARIA");
	public static final Set<String> INDEMNITY_PAYMENT_METHOD = Sets.newHashSet("PAGAMENTO_UNICO", "FORMA_RENDA");
	public static final Set<String> COVERAGE_PERIOD = Sets.newHashSet("VITALICIA", "TEMPORARIA");
	public static final Set<String> INDEMNIFIABLE_PERIOD = Sets.newHashSet("PRAZO", "ATE_FIM_CICLO_DETERMINADO");
	private static final Set<String> COVERAGE = Sets.newHashSet("MORTE", "INVALIDEZ");
	private static final Set<String> ADDITIONAL = Sets.newHashSet("SORTEIO", "OUTROS");
	private static final Set<String> UPDATE_INDEX = Sets.newHashSet("FINANCEIRA", "IGPM", "INPC");
	private static final Set<String> PREMIUM_UPDATE_INDEX = Sets.newHashSet("IPCA", "IGPM", "INPC");
	private static final Set<String> REFRAMING_CRITERION = Sets.newHashSet("APOS_PERIODO_ANOS", "CADA_PERIODO_ANOS", "MUDANCA_FAIXA_ETARIA", "NAO_APLICAVEL");
	private static final Set<String> FINANCIAL_REGIME_CONTRACT_TYPE = Sets.newHashSet("REPARTICAO_SIMPLES", "REPARTICAO_CAPITAIS_COBERTURA", "CAPITALIZACAO");
	private static final Set<String> OTHER_GUARATEED_VALUES = Sets.newHashSet("SALDAMENTO", "BENEFICIO_PROLOGANDO", "NAO_APLICA");
	private static final Set<String> PROFIT_MODALITY = Sets.newHashSet("PAGAMENTO_UNICO", "FORMA_RENDA");
	private static final Set<String> TARGET_AUDIENCE = Sets.newHashSet("PESSOA_NATURAL", "PESSOA_JURIDICA");
	private static final Set<String> MIN_REQUIREMENTS_CONTRACT_TYPE = Sets.newHashSet("COLETIVO", "INDIVIDUAL");
	private static final Set<String> CONTRIBUTION_PAYMENT_METHOD = Sets.newHashSet("CARTAO_CREDITO", "DEBITO_CONTA", "DEBITO_CONTA_POUPANCA", "BOLETO_BANCARIO", "PIX", "TED_DOC", "CONSIGNACAO_FOLHA_PAGAMENTO", "PONTOS_PROGRAMA_BENEFICIO", "OUTROS");
	private static final Set<String> CONTRIBUTION_PERIODICITY = Sets.newHashSet("MENSAL", "UNICA", "ANUAL", "TRIMESTRAL", "SEMESTRAL", "BIMESTRAL", "OUTRAS");
	private static final Set<String> ASSISTANCE_TYPE = Sets.newHashSet("ACOMPANHANTE_CASO_HOSPITALIZACAO_PROLONGADA",
		"ARQUITETO_VIRTUAL", "ASSESSORIA_FINANCEIRA", "AUTOMOVEL", "AUXILIO_NATALIDADE", "AVALIACAO_CLINICA_PREVENTIVA",
		"BOLSA_PROTEGIDA", "CESTA_BASICA", "CHECKUP_ODONTOLOGICO", "CLUBE_DE_VANTAGENS_BENEFICIOS",
		"CONVALESCENCIA", "DECESSO_FAMILIAR_E_OU_INDIVIDUAL", "DESCONTO_FARMACIAS_MEDICAMENTOS",
		"DESPESAS_FARMACEUTICAS_VIAGEM", "DIGITAL", "EDUCACIONAL", "EMPRESARIAL", "ENTRETENIMENTO",
		"EQUIPAMENTOS_MEDICOS", "FIANCAS_E_DESPESAS_LEGAIS", "FISIOTERAPIA", "FUNERAL", "HELP_LINE", "HOSPEDAGEM_DE_ACOMPANHANTE", "INTERRUPCAO_DA_VIAGEM", "INVENTARIO", "MAIS_EM_VIDA", "MAMAE_BEBE", "MEDICA_POR_ACIDENTE_OU_DOENCA", "MOTOCICLETA", "MULHER", "NUTRICIONISTA", "ODONTOLOGICA", "ORIENTACAO_FITNESS", "ORIENTACAO_JURIDICA", "ORIENTACAO_PSICOSSOCIAL_FAMILIAR", "PERDA_ROUBO_CARTAO", "PET", "PRORROGACAO_DE_ESTADIA", "PROTECAO_DE_DADOS", "RECOLOCACAO_PROFISSIONAL", "REDE_DESCONTO_NUTRICIONAL", "RESIDENCIAL", "RETORNO_MENORES_E_OU_SEGURADO", "SAQUE_SOB_COACAO", "SAUDE_BEMESTAR", "SEGUNDA_OPINIAO_MEDICA", "SENIOR", "SUSTENTAVEL_(DESCARTE_ECOLOGICO)", "TELEMEDICINA", "VIAGEM", "VITIMAS");

	private final CommonValidatorParts parts;
	private static class Fields extends ProductNServicesCommonFields { }

	public GetPensionPlanValidator() {
		parts = new CommonValidatorParts(this);
	}

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body,
			new DatetimeField
				.Builder("requestTime")
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		//assertHasField(body, "data");

		 assertField(body, new ObjectField
				.Builder("brand")
				.setValidator(brand -> {
					assertField(brand, Fields.name().build());
					assertField(brand, new ObjectField
						.Builder("companies")
						.setValidator(this::assertCompanies)
						.build());
				}).build());
		logFinalStatus();
		return environment;
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, Fields.name().build());
		assertField(companies, Fields.cnpjNumber().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("products")
				.setValidator(this::assertProducts)
				.setOptional()
				.build());
	}

	private void assertProducts(JsonObject products) {
		assertField(products, Fields.name().build());
		assertField(products, Fields.code().build());

		assertField(products,
			new StringField
				.Builder("modality")
				.setEnums(MODALITY)
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("coverages")
				.setValidator(this::assertCoverages)
				.build());

		assertField(products,
			new StringField
				.Builder("additional")
				.setEnums(ADDITIONAL)
				.setOptional()
				.build());

		assertField(products,
			new StringField
				.Builder("additionalOthers")
				.setOptional()
				.build());

		assertField(products,
			new StringArrayField
				.Builder("assistanceType")
				.setEnums(ASSISTANCE_TYPE)
				.setOptional()
				.build());

		assertField(products,
			new StringArrayField
				.Builder("assistanceTypeOthers")
				.setOptional()
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("termsAndCondition")
				.setValidator(termsAndConditions -> {
					assertField(termsAndConditions,
						new StringField
							.Builder("susepProcessNumber")
							.build());

					assertField(termsAndConditions,
						new StringField
							.Builder("definition")
							.build());
				})
				.setOptional()
				.build());

		assertField(products,
			new ObjectField
				.Builder("updatePMBaC")
				.setValidator(updatePMBaC -> {
					assertField(updatePMBaC,
						new IntField
							.Builder("interestRate")
							.build());

					assertField(updatePMBaC,
						new StringField
							.Builder("updateIndex")
							.setEnums(UPDATE_INDEX)
							.build());
				})
				.setOptional()
				.build());

		assertField(products,
			new StringField
				.Builder("premiumUpdateIndex")
				.setEnums(PREMIUM_UPDATE_INDEX)
				.build());

		assertField(products,
			new ObjectField
				.Builder("ageReframing")
				.setValidator(updatePMBaC -> {
					assertField(updatePMBaC,
						new StringField
							.Builder("reframingCriterion")
							.setEnums(REFRAMING_CRITERION)
							.build());

					assertField(updatePMBaC,
						new IntField
							.Builder("reframingPeriodicity")
							.build());
				})
				.setOptional()
				.build());

		assertField(products,
			new StringField
				.Builder("financialRegimeContractType")
				.setEnums(FINANCIAL_REGIME_CONTRACT_TYPE)
				.build());

		assertField(products,
			new ObjectField
				.Builder("reclaim")
				.setValidator(this::assertReclaim)
				.setOptional()
				.build());

		assertField(products,
			new StringField
				.Builder("otherGuarateedValues")
				.setEnums(OTHER_GUARATEED_VALUES)
				.build());

		assertField(products,
			new StringField
				.Builder("profitModality")
				.setEnums(PROFIT_MODALITY)
				.build());

		assertField(products,
			new ObjectField
				.Builder("contributionPayment")
				.setValidator(contributionPayment -> {
					assertField(contributionPayment,
						new StringArrayField
							.Builder("contributionPaymentMethod")
							.setEnums(CONTRIBUTION_PAYMENT_METHOD)
							.build());

					assertField(contributionPayment,
						new StringArrayField
							.Builder("contributionPeriodicity")
							.setEnums(CONTRIBUTION_PERIODICITY)
							.build());
				})
				.setOptional()
				.build());

		assertField(products,
			new StringField
				.Builder("contributionTax")
				.setOptional()
				.build());

		assertField(products,
			new ObjectField
				.Builder("minimumRequirements")
				.setValidator(minimumRequirements -> {
					assertField(minimumRequirements,
						new StringField
							.Builder("minRequirementsContractType")
							.setEnums(MIN_REQUIREMENTS_CONTRACT_TYPE)
							.build());

					assertField(minimumRequirements,
						new StringField
							.Builder("minRequirementsContract")
							.build());
				})
				.build());

		assertField(products,
			new StringField
				.Builder("targetAudience")
				.setEnums(TARGET_AUDIENCE)
				.build());
	}

	private void assertReclaim(JsonObject reclaim) {
		assertField(reclaim,
			new ObjectArrayField
				.Builder("reclaimTable")
				.setValidator(reclaimTable -> {
					assertField(reclaimTable,
						new IntField
							.Builder("initialMonthRange")
							.build());

					assertField(reclaimTable,
						new IntField
							.Builder("finalMonthRange")
							.build());

					assertField(reclaimTable,
						new StringField
							.Builder("percentage")
							.build());
				})
				.build());

		assertField(reclaim,
			new StringField
				.Builder("differentiatedPercentage")
				.setOptional()
				.build());

		assertField(reclaim,
			new StringField
				.Builder("gracePeriod")
				.build());
	}

	private void assertCoverages(JsonObject coverages) {
		assertField(coverages,
			new StringField
				.Builder("coverage")
				.setEnums(COVERAGE)
				.setOptional()
				.build());

		assertField(coverages,
			new ObjectField
				.Builder("coverageAttributes")
				.setValidator(this::assertCoverageAttributes)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("coveragePeriod")
				.setEnums(COVERAGE_PERIOD)
				.setOptional()
				.build());
	}

	private void assertCoverageAttributes(JsonObject coverageAttributes) {
		assertField(coverageAttributes,
			new StringArrayField
				.Builder("indemnityPaymentMethod")
				.setEnums(INDEMNITY_PAYMENT_METHOD)
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("minValue")
				.setValidator(parts::assertValue)
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("maxValue")
				.setValidator(parts::assertValue)
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("indemnifiablePeriod")
				.setEnums(INDEMNIFIABLE_PERIOD)
				.build());

		assertField(coverageAttributes,
			new IntField
				.Builder("indemnifiableDeadline")
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("currency")
				.setEnums(CURRENCY)
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("gracePeriod")
				.setValidator(parts::assertGracePeriod)
				.build());

		assertField(coverageAttributes,
			new StringArrayField
				.Builder("excludedRisk")
				.setEnums(EXCLUDED_RISKS)
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("excludedRiskURL")
				.build());

	}
}
