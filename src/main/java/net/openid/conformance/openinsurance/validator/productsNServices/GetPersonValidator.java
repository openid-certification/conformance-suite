package net.openid.conformance.openinsurance.validator.productsNServices;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.CommonValidatorParts;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;

import static net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields.CURRENCY;
import static net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields.EXCLUDED_RISKS;

/**
 * Api Source: swagger/openinsurance/swagger-productsnservices-person.yaml
 * Api endpoint: /person/
 * Api version: 1.0.3
 * Git hash: b62c9f60c0df42cb67387ec0dd0b6d0fd986478a
 */

@ApiName("ProductsNServices Person")
public class GetPersonValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> CATEGORY = Sets.newHashSet("TRADICIONAL", "MICROSEGURO");
	public static final Set<String> OTHER_GUARANTEED_VALUES = Sets.newHashSet("SALDAMENTO", "BENEFICIO_PROLONGADO", "NAO_SE_APLICA");
	public static final Set<String> CONTRACT_TYPE = Sets.newHashSet("REPARTICAO_SIMPLES", "REPARTICAO_CAPITAIS", "CAPITALIZACAO");
	public static final Set<String> CRITERION = Sets.newHashSet("APOS_PERIODO_EM_ANOS", "A_CADA_PERIODO_EM_ANOS", "POR_MUDANCA_DE_FAIXA_ETARIA", "NAO_APLICAVEL");
	public static final Set<String> BENEFIT_RECALCULATION_CRITERIA = Sets.newHashSet("INDICE", "VINCULADO_SALDO_DEVEDOR", "VARIAVEL_ACORDO_CRITERIO_ESPECIFICO");
	public static final Set<String> PMBAC_UPDATE_INDEX = Sets.newHashSet("IPCA", "IGP-M", "INPC");
	public static final Set<String> VALIDITY = Sets.newHashSet("VITALICIA", "TEMPORARIA_PRAZO_FIXO", "TEMPORARIA_INTERMITENTE");
	private static final Set<String> ADDITIONAL = Sets.newHashSet("SORTEIO", "SERVICOS_ASSISTENCIAS_COMPLEMENTARES_PAGO", "SERVICOS_ASSISTENCIA_COMPLEMENTARES_GRATUITO", "OUTROS", "NAO_HA");
	private static final Set<String> ASSISTANCE_TYPE = Sets.newHashSet("ACOMPANHANTE_CASO_HOSPITALIZACAO_PROLONGADA", "ARQUITETO_VIRTUAL", "ASSESSORIA_FINANCEIRA", "AUTOMOVEL", "AUXILIO_NATALIDADE", "AVALIACAO_CLINICA_PREVENTIVA", "BOLSA_PROTEGIDA", "CESTA_BASICA", "CHECKUP_ODONTOLOGICO", "CLUBE_VANTAGENS_BENEFICIOS", "CONVALESCENCIA", "DECESSO", "DESCONTO_FARMACIAS_MEDICAMENTOS", "DESPESAS_FARMACEUTICAS_VIAGEM", "DIGITAL", "EDUCACIONAL", "EMPRESARIAL", "ENCANADOR", "ENTRETENIMENTO", "EQUIPAMENTOS_MEDICOS", "FIANCAS_DESPESAS_LEGAIS", "FISIOTERAPIA", "FUNERAL", "HELP_LINE", "HOSPEDAGEM_ACOMPANHANTE", "INTERRUPCAO_VIAGEM", "INVENTARIO", "MAIS_EM_VIDA", "MAMAE_BEBE", "MEDICA_ACIDENTE_DOENCA", "MOTOCICLETA", "MULHER", "NUTRICIONISTA", "ODONTOLOGICA", "ORIENTACAO_FITNESS", "ORIENTACAO_JURIDICA", "ORIENTACAO_NUTRICIONAL", "PERSONAL_FITNESS", "ORIENTACAO_PSICOSSOCIAL_FAMILIAR", "PERDA_ROUBO_CARTAO", "PET", "PRORROGACAO_ESTADIA", "PROTECAO_DADOS", "RECOLOCACAO_PROFISSIONAL", "REDE_DESCONTO_NUTRICIONAL", "RESIDENCIAL", "RETORNO_MENORES_SEGURADO", "SAQUE_COACAO", "SAUDE_BEM_ESTAR", "SEGUNDA_OPINIAO_MEDICA", "SENIOR", "SUSTENTAVEL_DESCARTE_ECOLOGICO", "TELEMEDICINA", "VIAGEM", "VITIMA", "OUTROS");
	public static final Set<String> INDEMNITY_PAYMENT_METHOD_1 = Sets.newHashSet("PAGAMENTO_CAPITAL_SEGURADO_VALOR_MONETARIO", "REEMBOLSO_DESPESAS", "PRESTACAO_SERVICOS");
	public static final Set<String> INDEMNITY_PAYMENT_METHOD_2 = Sets.newHashSet("UNICO", "SOB_FORMA_DE_RENDA");
	public static final Set<String> INDEMNITY_PAYMENT_FREQUENCY = Sets.newHashSet("INDENIZACAO_UNICA", "DIARIA_OU_PARCELA");
	public static final Set<String> INDEMNITY_PAYMENT_INCOME = Sets.newHashSet("CERTA", "TEMPORARIA", "TEMPORARIA_REVERSIVEL", "TEMPORARIO_MINIMO_GARANTIDO", "TEMPORARIA_REVERSIVEL_MINIMO_GARANTIDO", "VITALICIA", "VITALICIA_REVERSIVEL", "VITALICIA_MINIMO_GARANTIDO", "VITALICIA_REVERSIVEL_MINIMO_GARANTIDO");
	public static final Set<String> INSURANCE_MODALITY = Sets.newHashSet("FUNERAL", "PRESTAMISTA", "VIAGEM", "EDUCACIONAL", "DOTAL", "ACIDENTES_PESSOAIS", "VIDA", "PERDA_CERTIFICADO_HABILITACAOO_VOO", "DOENCAS_GRAVES_DOENCA_TERMINAL", "DESEMPREGO_PERDA_RENDA", "EVENTOS_ALEATORIOS");
	public static final Set<String> PAYMENT_METHOD = Sets.newHashSet("CARTAO_CREDITO", "DEBITO_CONTA", "DEBITO_CONTA_POUPANCA", "BOLETO_BANCARIO", "PIX", "CARTAO_DEBITO", "REGRA_PARCEIRO", "CONSIGNACAO_FOLHA_PAGAMENTO", "PONTOS_PROGRAMAS_BENEFICIO");
	public static final Set<String> FREQUENCY = Sets.newHashSet("DIARIA", "MENSAL", "UNICA", "ANUAL", "TRIMESTRAL", "SEMESTRAL", "FRACIONADO", "OUTRA");
	public static final Set<String> CONTRACTING_TYPE = Sets.newHashSet("COLETIVO", "INDIVIDUAL");
	public static final Set<String> TARGET_AUDIENCE = Sets.newHashSet("PESSOA_JURIDICA", "PESSOA_NATURAL");
	private static final Set<String> COVERAGE = Sets.newHashSet("ADIANTAMENTO_DOENCA_ESTAGIO_TERMINAL", "AUXILIO_CESTA_BASICA", "AUXILIO_FINANCEIRO_IMEDIATO", "CANCELAMENTO_DE_VIAGEM", "CIRURGIA", "COBERTURA_PARA_HERNIA", "COBERTURA_PARA_LER_DORT", "CUIDADOS_PROLONGADOS_ACIDENTE", "DESEMPREGO_PERDA_DE_RENDA", "DESPESAS_EXTRA_INVALIDEZ_PERMANENTE_TOTAL_PARCIAL_ACIDENTE_DEI", "DESPESAS_EXTRA_MORTE_DEM", "DESPESAS_MEDICAS_HOSPITALARES_ODONTOLOGICAS", "DESPESAS_MEDICAS_HOSPITALARES_ODONTOLOGICAS_BRASIL", "DESPESAS_MEDICAS_HOSPITALARES_ODONTOLOGICAS_EXTERIOR", "DIARIA_INCAPACIDADE_TOTAL_TEMPORARIA", "DIARIA_INTERNACAO_HOSPITALAR", "INTERNACAO_HOSPITALAR", "DIARIAS_INCAPACIDADE_PECUNIARIA_DIP", "DOENCA_GRAVE", "DOENCA_CONGENITA_FILHOS_DCF", "FRATURA_OSSEA", "DOENCAS_TROPICAIS", "INCAPACIDADE_TOTAL_OU_TEMPORARIA", "INVALIDEZ_PERMANENTE_TOTAL_PARCIAL", "INVALIDEZ_TOTAL_ACIDENTE", "INVALIDEZ_PARCIAL_ACIDENTE", "INVALIDEZ_FUNCIONAL_PERMANENTE_DOENCA", "INVALIDEZ_LABORATIVA_DOENCA", "MORTE", "MORTE_ACIDENTAL", "MORTE_CONJUGE", "MORTE_FILHOS", "MORTE_ADIATAMENTO_DOENCA_ESTAGIO_TERMINAL", "PAGAMENTO_ANTECIPADO_ESPECIAL_DOENCA_PROFISSIONAL_PAED", "PERDA_DA_AUTONOMIA_PESSOAL", "PERDA_INVOLUNTARIA_EMPREGO", "QUEIMADURA_GRAVE", "REGRESSO_ANTECIPADO_SANITARIO", "RENDA_INCAPACIDADE_TEMPORARIA", "RESCISAO_CONTRATUAL_CASO_MORTE_RCM", "RESCISAO_TRABALHISTA", "SERVICO_AUXILIO_FUNERAL", "SOBREVIVENCIA", "TRANSPLANTE_ORGAOS", "TRANSLADO", "TRANSLADO_MEDICO", "TRANSLADO_CORPO", "VERBA_RESCISORIA", "OUTRAS");

	private final CommonValidatorParts parts;

	private static class Fields extends ProductNServicesCommonFields {}

	public GetPersonValidator() {
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
					assertField(brand, Fields.name().build());
					assertField(brand,
						new ObjectArrayField.Builder("companies")
							.setValidator(this::assertPersonCompanies)
							.build());
				}
			).build())
		).build());
		logFinalStatus();
		return environment;
	}

	private void assertPersonCompanies(JsonObject companies) {
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
				.Builder("category")
				.setEnums(CATEGORY)
				.build());

		assertField(products,
			new StringField
				.Builder("insuranceModality")
				.setEnums(INSURANCE_MODALITY)
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("coverages")
				.setValidator(this::assertCoverages)
				.build());

		assertField(products,
			new StringArrayField
				.Builder("assistanceType")
				.setEnums(ASSISTANCE_TYPE)
				.setOptional()
				.build());

		assertField(products,
			new StringArrayField
				.Builder("additional")
				.setEnums(ADDITIONAL)
				.build());

		assertField(products,
			new StringArrayField
				.Builder("assistanceTypeOthers")
				.setOptional()
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("termsAndConditions")
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
				.build());

		assertField(products,
			new BooleanField
				.Builder("globalCapital")
				.build());

		assertField(products,
			new StringArrayField
				.Builder("validity")
				.setEnums(VALIDITY)
				.build());

		assertField(products,
			new ObjectField.Builder("pmbacRemuneration")
				.setValidator(pmbacRemuneration -> {
					assertField(pmbacRemuneration,
						new NumberField
							.Builder("interestRate")
							.setOptional()
							.build());

					assertField(pmbacRemuneration,
						new StringField
							.Builder("pmbacUpdateIndex")
							.setEnums(PMBAC_UPDATE_INDEX)
							.setOptional(parts.isOptionalFieldByFlag(products,"contractType","CAPITALIZACAO"))
							.build());
				})
				.setOptional()
				.build());

		assertField(products,
			new ObjectField.Builder("benefitRecalculation")
				.setValidator(benefitRecalculation -> {
					assertField(benefitRecalculation,
						new StringField
							.Builder("benefitRecalculationCriteria")
							.setEnums(BENEFIT_RECALCULATION_CRITERIA)
							.build());

					assertField(benefitRecalculation,
						new StringField
							.Builder("benefitUpdateIndex")
							.setEnums(PMBAC_UPDATE_INDEX)
							.setOptional()
							.build());
				})
				.setOptional()
				.build());

		assertField(products,
			new ObjectField
				.Builder("ageAdjustment")
				.setValidator(ageAdjustment -> {
					assertField(ageAdjustment,
						new StringField
							.Builder("criterion")
							.setEnums(CRITERION)
							.build());

					assertField(ageAdjustment,
						new IntField
							.Builder("frequency")
							.build());
				})
				.setOptional()
				.build());

		assertField(products,
			new StringField
				.Builder("contractType")
				.setEnums(CONTRACT_TYPE)
				.build());

		assertField(products,
			new ObjectField
				.Builder("reclaim")
				.setValidator(reclaim -> {
					assertField(reclaim,
						new ObjectArrayField
							.Builder("reclaimTable")
							.setValidator(this::assertReclaimTable)
							.setOptional(parts.isOptionalFieldByFlag(products, "contractType", "CAPITALIZACAO"))
							.build());

					assertField(reclaim,
						new StringField
							.Builder("differentiatedPercentage")
							.setOptional()
							.build());

					assertField(reclaim,
						new ObjectField
							.Builder("gracePeriod")
							.setValidator(parts::assertGracePeriod)
							.build());
				})
				.setOptional()
				.build());

		assertField(products,
			new StringField
				.Builder("otherGuaranteedValues")
				.setEnums(OTHER_GUARANTEED_VALUES)
				.build());

		assertField(products,
			new BooleanField
				.Builder("allowPortability")
				.build());

		assertField(products,
			new IntField
				.Builder("portabilityGraceTime")
				.build());

		assertField(products,
			new StringArrayField
				.Builder("indemnityPaymentMethod")
				.setEnums(INDEMNITY_PAYMENT_METHOD_2)
				.build());

		assertField(products,
			new StringArrayField
				.Builder("indemnityPaymentIncome")
				.setEnums(INDEMNITY_PAYMENT_INCOME)
				.build());

		assertField(products,
			new ObjectField
				.Builder("premiumPayment")
				.setValidator(this::assertPremiumPayment)
				.setOptional()
				.build());

		assertField(products,
			new ObjectField
				.Builder("minimunRequirements")
				.setValidator(this::assertMinimunRequirements)
				.setOptional()
				.build());

		assertField(products,
			new StringField
				.Builder("targetAudience")
				.setEnums(TARGET_AUDIENCE)
				.build());
	}

	private void assertMinimunRequirements(JsonObject minimunRequirements) {
		assertField(minimunRequirements,
			new StringField
				.Builder("contractingType")
				.setEnums(CONTRACTING_TYPE)
				.build());

		assertField(minimunRequirements,
			new StringField
				.Builder("contractingMinRequirement")
				.build());
	}

	private void assertPremiumPayment(JsonObject premiumPayment) {
		assertField(premiumPayment,
			new StringArrayField
				.Builder("paymentMethod")
				.setEnums(PAYMENT_METHOD)
				.build());

		assertField(premiumPayment,
			new StringArrayField
				.Builder("frequency")
				.setEnums(FREQUENCY)
				.build());

		assertField(premiumPayment,
			new StringField
				.Builder("premiumTax")
				.setOptional()
				.build());
	}

	private void assertReclaimTable(JsonObject reclaimTable) {
		assertField(reclaimTable,
			new IntField
				.Builder("initialMonthRange")
				.build());

		assertField(reclaimTable,
			new IntField
				.Builder("finalMonthRange")
				.build());

		assertField(reclaimTable,
			new NumberField
				.Builder("percentage")
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
			new StringArrayField
				.Builder("coverageOthers")
				.setOptional()
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
			new StringArrayField
				.Builder("indemnityPaymentMethod")
				.setEnums(INDEMNITY_PAYMENT_METHOD_1)
				.build());

		assertField(coverageAttributes,
			new StringArrayField
				.Builder("indemnityPaymentFrequency")
				.setEnums(INDEMNITY_PAYMENT_FREQUENCY)
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
			new StringArrayField
				.Builder("indemnifiablePeriod")
				.build());

		assertField(coverageAttributes,
			new IntField
				.Builder("maximumQtyIndemnifiableInstallments")
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
			new ObjectField
				.Builder("differentiatedGracePeriod")
				.setValidator(parts::assertGracePeriod)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new IntField
				.Builder("deductibleDays")
				.build());

		assertField(coverageAttributes,
			new NumberField
				.Builder("differentiatedDeductibleDays")
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new NumberField
				.Builder("deductibleBRL")
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("differentiatedDeductibleBRL")
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new StringArrayField
				.Builder("excludedRisks")
				.setEnums(EXCLUDED_RISKS)
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("excludedRisksURL")
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new BooleanField
				.Builder("allowApartPurchase")
				.build());
	}
}
