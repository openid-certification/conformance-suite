package net.openid.conformance.openbanking_brasil.opendata.pension;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api url: https://sensedia.github.io/areadesenvolvedor/swagger/swagger_pension_apis.yaml
 * Api endpoint: /survival-coverages
 * Api version: 1.0.0
 */

@ApiName("Pension Survival Coverages")
public class SurvivalCoveragesValidator extends AbstractJsonAssertingCondition {
	private static class Fields extends ProductNServicesCommonFields {
	}

	private final CommonOpenDataParts parts;

	public SurvivalCoveragesValidator() {
		parts = new CommonOpenDataParts(this);
	}

	public static final Set<String> TYPE = Sets.newHashSet("PGBL","PRGP","PAGP","PRSA","PRI","PDR","VGBL","VRGP","VAGP","VRSA","VRI","VDR","DEMAIS_PRODUTOS_PREVIDENCIA");
	public static final Set<String> SEGMENT = Sets.newHashSet("SEGURO_DE_PESSOAS", "PREVIDENCIA");
	public static final Set<String> MODALITY = Sets.newHashSet("CONTRIBUICAO_VARIAVEL", "BENEFICIO_DEFINIDO");
	public static final Set<String> CONTRACT_TYPE = Sets.newHashSet("COLETIVO", "INDIVIDUAL", "AMBAS");
	public static final Set<String> UNIT = Sets.newHashSet("DIAS", "MESES", "NAO_APLICA");
	public static final Set<String> INDEX = Sets.newHashSet("IPCA", "IGP_M", "INPC");
	public static final Set<String> PEREODICITY = Sets.newHashSet("DIÁRIO", "SEMANAL", "QUINZENAL", "MENSAL", "BIMESTRAL", "TRIMESTRAL", "SEMESTRAL", "ANUAL");
	public static final Set<String> PREMIUM_PAYMENT_METHODS = Sets.newHashSet("CARTAO_CREDITO","DEBITO_CONTA","DEBITO_CONTA_POUPANCA","BOLETO_BANCARIO","PIX","CARTAO_DEBITO","REGRA_PARCEIRO","CONSIGNACAO_FOLHA_PAGAMENTO","PONTOS_PROGRAMA_BENEFICIO","TED_DOC","OUTROS");
	public static final Set<String> TARGET_AUDIENCE = Sets.newHashSet("PESSOA_NATURAL","PESSOA_JURIDICA","PESSOA_NATURAL_JURIDICA");
	public static final Set<String> TYPE_PERFORMANCE_FEE = Sets.newHashSet("DIRETAMENTE","INDIRETAMENTE","NAO_APLICA");
	public static final Set<String> INCOME_MODALITY = Sets.newHashSet("PAGAMENTO_UNICO","RENDA_PRAZO_CERTO","RENDA_TEMPORARIA","RENDA_TEMPORARIA_REVERSIVEL","RENDA_TEMPORARIA_MINMO_GARANTIDO","RENDA_TEMPORARIA_REVERSIVEL_MININO_GARANTIDO","RENDA_VITALICIA","RENDA_VITALICIA_REVERSIVEL_BENEFICIARIO_INDICADO","RENDA_VITALICIA_CONJUGE_CONTINUIDADE_MENORES","RENDA_VITALICIA_MINIMO_GARANTIDO","RENDA_VITALICIA_PRAZO_MINIMO_GRANTIDO");
	public static final Set<String> BIOMETRIC_TABLE = Sets.newHashSet("AT_2000_MALE","AT_2000_FEMALE","AT_2000_MALE_FEMALE","AT_2000_MALE_SUAVIZADA_10","AT_2000_FEMALE_SUAVIZADA_10","AT_2000_MALE_FEMALE_SUAVIZADA_10","AT_2000_MALE_SUAVIZADA_15","AT_2000_FEMALE_SUAVIZADA_15","AT_2000_MALE_FEMALE_SUAVIZADA_15","AT_83_MALE","AT_83_FEMALE","AT_83_MALE_FEMALE","BR_EMSSB_MALE","BR_EMSSB_FEMALE","BR_EMSSB_MALE_FEMALE");


	@Override
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
				.setValidator(termsAndConditions -> {
					assertField(termsAndConditions, Fields.name().setMaxLength(80).build());
					assertField(termsAndConditions, Fields.cnpjNumber().setPattern("^\\d{14}$").build());
				})
				.setOptional()
				.build());

		assertField(data, Fields.name().setMaxLength(80).build());
		assertField(data, Fields.code().setMaxLength(80).build());

		assertField(data,
			new StringField
				.Builder("segment")
				.setEnums(SEGMENT)
				.setMaxLength(20)
				.build());

		assertField(data,
			new StringField
				.Builder("modality")
				.setEnums(MODALITY)
				.setMaxLength(21)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(1024)
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
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("type")
				.setEnums(TYPE)
				.setMaxLength(27)
				.build());

		assertField(data,
			new ObjectField
				.Builder("defferalPeriod")
				.setValidator(this::assertDefferalPeriod)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("grantPeriodBenefit")
				.setValidator(this::assertGrantPeriodBenefit)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("costs")
				.setValidator(costs -> {
					assertField(costs,
						new ObjectField
							.Builder("loadingAntecipated")
							.setValidator(this::assertValue)
							.build());

					assertField(costs,
						new ObjectField
							.Builder("loadingLate")
							.setValidator(this::assertValue)
							.build());
				})
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("minimumRequirements")
				.setValidator(minimumRequirements -> {
					assertField(minimumRequirements,
						new StringField
							.Builder("contractType")
							.setEnums(CONTRACT_TYPE)
							.setMaxLength(27)
							.build());

					assertField(minimumRequirements,
						new BooleanField
							.Builder("participantQualified")
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
				.setEnums(TARGET_AUDIENCE)
				.setMaxLength(23)
				.build());
	}

	private void assertGrantPeriodBenefit(JsonObject grantPeriodBenefit) {
		assertField(grantPeriodBenefit,
			new StringArrayField
				.Builder("incomeModalities")
				.setEnums(INCOME_MODALITY)
				.setMinItems(1)
				.build());

		assertField(grantPeriodBenefit,
			new StringArrayField
				.Builder("biometricTable")
				.setEnums(BIOMETRIC_TABLE)
				.setMinItems(1)
				.setMaxLength(32)
				.build());

		assertField(grantPeriodBenefit,
			new StringField
				.Builder("interestRate")
				.setPattern("^[0-1]\\.\\d{6}$")
				.setMaxLength(10)
				.setOptional()
				.build());

		assertField(grantPeriodBenefit,
			new StringField
				.Builder("updateIndex")
				.setEnums(INDEX)
				.setMaxLength(5)
				.setOptional()
				.build());

		assertField(grantPeriodBenefit,
			new StringField
				.Builder("reversalFinancialResults")
				.setPattern("^[0-1]\\.\\d{6}$")
				.setMaxLength(8)
				.setOptional()
				.build());

		assertField(grantPeriodBenefit,
			new ObjectArrayField
				.Builder("investmentFunds")
				.setValidator(this::assertInvestmentFunds)
				.setOptional()
				.build());

	}

	private void assertValue(JsonObject value) {
		assertField(value,
			new StringField
				.Builder("minValue")
				.setPattern("^[0-1]\\.\\d{6}$")
				.setMaxLength(8)
				.build());

		assertField(value,
			new StringField
				.Builder("maxValue")
				.setPattern("^[0-1]\\.\\d{6}$")
				.setMaxLength(8)
				.build());
	}

	private void assertDefferalPeriod(JsonObject defferalPeriod) {
		assertField(defferalPeriod,
			new StringField
				.Builder("interestRate")
				.setPattern("^[0-1]\\.\\d{6}$")
				.setMaxLength(10)
				.setOptional()
				.build());

		assertField(defferalPeriod,
			new StringField
				.Builder("updateIndex")
				.setEnums(INDEX)
				.setMaxLength(5)
				.setOptional()
				.build());

		assertField(defferalPeriod,
			new StringField
				.Builder("otherMinimumPerformanceGarantees")
				.setMaxLength(12)
				.setOptional()
				.build());

		assertField(defferalPeriod,
			new StringField
				.Builder("reversalFinancialResults")
				.setPattern("^[0-1]\\.\\d{6}$")
				.setMaxLength(8)
				.setOptional()
				.build());

		assertField(defferalPeriod,
			new ObjectField
				.Builder("minimumPremium")
				.setValidator(minimumPremium -> {
					assertField(minimumPremium,
						new StringField
							.Builder("currency")
							.setPattern("^[A-Z]{3}$")
							.setOptional()
							.build());

					assertField(minimumPremium,
						new StringField
							.Builder("periodicity")
							.setEnums(PEREODICITY)
							.setMaxLength(10)
							.setOptional()
							.build());

					assertField(minimumPremium,
						new StringField
							.Builder("amount")
							.setPattern("^\\d{1,16}\\.\\d{2,4}$")
							.setMaxLength(21)
							.setOptional()
							.build());
				})
				.setOptional()
				.build());

		assertField(defferalPeriod,
			new StringArrayField
				.Builder("premiumPaymentMethods")
				.setMaxLength(27)
				.setEnums(PREMIUM_PAYMENT_METHODS)
				.build());

		assertField(defferalPeriod,
			new BooleanField
				.Builder("permissionExtraordinaryContributions")
				.setOptional()
				.build());

		assertField(defferalPeriod,
			new BooleanField
				.Builder("permissionScheduledFinancialPayments")
				.setOptional()
				.build());

		assertField(defferalPeriod,
			new ObjectField
				.Builder("gracePeriod")
				.setValidator(this::assertGracePeriod)
				.setOptional()
				.build());

		assertField(defferalPeriod,
			new IntField
				.Builder("redemptionPaymentTerm")
				.setMaxValue(9999)
				.setOptional()
				.build());

		assertField(defferalPeriod,
			new NumberField
				.Builder("portabilityPaymentTerm")
				.setMaxValue(9999)
				.setOptional()
				.build());

		assertField(defferalPeriod,
			new ObjectArrayField
				.Builder("investmentFunds")
				.setValidator(this::assertInvestmentFunds)
				.setOptional()
				.build());
	}

	private void assertInvestmentFunds(JsonObject investmentFunds) {
		assertField(investmentFunds, Fields.cnpjNumber().setPattern("\\d{14}$").setOptional().build());
		assertField(investmentFunds, Fields.name().setMaxLength(80).setOptional().build());

		assertField(investmentFunds,
			new StringField
				.Builder("maximumAdministrationFee")
				.setMaxLength(10)
				.setPattern("^[0-1]\\.\\d{6}$")
				.setOptional()
				.build());

		assertField(investmentFunds,
			new StringField
				.Builder("typePerformanceFee")
				.setEnums(TYPE_PERFORMANCE_FEE)
				.setMaxLength(13)
				.setOptional()
				.build());

		assertField(investmentFunds,
			new StringField
				.Builder("maximumPerformanceFee")
				.setMaxLength(10)
				.setPattern("^[0-1]\\.\\d{6}$")
				.setOptional()
				.build());

		assertField(investmentFunds,
			new BooleanField
				.Builder("eligibilityRule")
				.setOptional()
				.build());

		assertField(investmentFunds,
			new StringField
				.Builder("minimumContributionAmount")
				.setPattern("^\\d{1,16}\\.\\d{2,4}$")
				.setOptional()
				.build());

		assertField(investmentFunds,
			new StringField
				.Builder("minimumMathematicalProvisionAmount")
				.setPattern("^\\d{1,16}\\.\\d{2,4}$")
				.setOptional()
				.build());

		assertField(investmentFunds,
			new StringField
				.Builder("currency")
				.setPattern("^[A-Z]{3}$")
				.setOptional()
				.build());
	}

	private void assertGracePeriod(JsonObject gracePeriod) {
		assertField(gracePeriod,
			new NumberField
				.Builder("redemption")
				.setMaxValue(9999)
				.build());

		assertField(gracePeriod,
			new NumberField
				.Builder("betweenRedemptionRequests")
				.setMaxValue(9999)
				.build());

		assertField(gracePeriod,
			new NumberField
				.Builder("portability")
				.setMaxValue(9999)
				.build());

		assertField(gracePeriod,
			new NumberField
				.Builder("betweenPortabilityRequests")
				.setMaxValue(9999)
				.build());

	}
}
