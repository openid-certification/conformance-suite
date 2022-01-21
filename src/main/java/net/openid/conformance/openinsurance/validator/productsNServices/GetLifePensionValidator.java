package net.openid.conformance.openinsurance.validator.productsNServices;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api Swagger URL: https://gitlab.com/obb1/certification/-/blob/master/src/main/resources/swagger/openinsurance/swagger-productsnservices-lifepension.yaml
 * Api endpoint: /life-pension/
 * Api version: 1.0.0
 * Api Git Hash: 17d932e0fac28570a0bf2a8b8e292a65b816f278
 */

@ApiName("ProductsNServices Life Pension")
public class GetLifePensionValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> SEGMENT = Sets.newHashSet("SEGURO_PESSOAS", "PREVIDENCIA");
	public static final Set<String> TARGET_AUDIENCE = Sets.newHashSet("PESSOA_NATURAL", "PESSOA_JURIDICA");
	public static final Set<String> TYPE_PERFORMANCE_FEE = Sets.newHashSet("DIRETAMENTE", "INDIRETAMENTE", "NAO_APLICA");
	public static final Set<String> CONTRACT_TYPE = Sets.newHashSet("COLETIVO_AVERBADO", "COLETIVO_INSTITUIDO", "INDIVIDUAL");
	public static final Set<String> UPDATE_INDEX = Sets.newHashSet("IPCA", "INPC", "IGP-M");
	public static final Set<String> MODALITY = Sets.newHashSet("CONTRIBUICAO_VARIAVEL", "BENEFICIO_DEFINIDO");
	public static final Set<String> TYPES = Sets.newHashSet("PGBL", "PRGP", "PAGP", "PRSA", "PRI", "PDR", "VGBL", "VRGP", "VAGP", "VRSA", "VRI", "VDR", "DEMAIS_PRODUTOS_PREVIDENCIA");
	public static final Set<String> PREMIUM_PAYMENT_METHOD = Sets.newHashSet("CARTAO_CREDITO", "DEBITO_CONTA", "DEBITO_CONTA_POUPANCA", "BOLETO_BANCARIO", "PIX", "CARTAO_DEBITO", "REGRA_PARCEIRO", "CONSIGNACAO_FOLHA_PAGAMENTO", "PONTOS_PROGRAMA_BENEFICIO", "TED_DOC", "OUTROS");
	public static final Set<String> INCOME_MODALITY = Sets.newHashSet("PAGAMENTO_UNICO", "RENDA_PRAZO_CERTO", "RENDA_TEMPORARIA", "RENDA_TEMPORARIA_REVERSIVEL", "RENDA_TEMPORARIA_MINMO_GARANTIDO", "RENDA_TEMPORARIA_REVERSIVEL_MININO_GARANTIDO", "RENDA_VITALICIA", "RENDA_VITALICIA_REVERSIVEL_BENEFICIARIO_INDICADO", "RENDA_VITALICIA_CONJUGE_CONTINUIDADE_MENORES", "RENDA_VITALICIA_MINIMO_GARANTIDO", "RENDA_VITALICIA_PRAZO_MINIMO_GRANTIDO");
	public static final Set<String> BIOMETRIC_TABLE = Sets.newHashSet("AT_2000_MALE", "AT_2000_FEMALE", "AT_2000_MALE_FEMALE", "AT_2000_MALE_SUAVIZADA_10", "AT_2000_FEMALE_SUAVIZADA_10", "AT_2000_MALE_FEMALE_SUAVIZADA_10", "AT_2000_MALE_SUAVIZADA_15", "AT_2000_FEMALE_SUAVIZADA_15", "AT_2000_MALE_FEMALE_SUAVIZADA_15", "AT_83_MALE", "AT_83_FEMALE", "AT_83_MALE_FEMALE", "BR_EMSSB_MALE", "BR_EMSSB_FEMALE", "BR_EMSSB_MALE_FEMALE");

	private static class Fields extends ProductNServicesCommonFields { }

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = initBodyArray(environment);

		assertField(body,
			new ObjectArrayField.Builder("data")
				.setValidator(data->{
					assertField(data,
						new ObjectField
							.Builder("identification")
							.setValidator(this::assertIdentification)
							.build());

					assertField(data,
						new ObjectArrayField
							.Builder("products")
							.setValidator(this::assertProducts)
							.build());
				})
				.build());
		logFinalStatus();
		return environment;
	}


	private void assertIdentification(JsonObject identification){
		assertField(identification,
			new StringField
				.Builder("brand")
				.setMaxLength(80)
				.build());

		assertField(identification,
			new StringField
				.Builder("societyName")
				.setMaxLength(80)
				.build());

		assertField(identification, Fields.cnpjNumber().setMaxLength(14).build());
	}


	private void assertProducts(JsonObject products) {
		assertField(products,
			new StringField
				.Builder("name")
				.setMaxLength(80)
				.build());

		assertField(products,
			new StringField
				.Builder("code")
				.setMaxLength(80)
				.build());

		assertField(products,
			new StringField
				.Builder("segment")
				.setMaxLength(20)
				.setEnums(SEGMENT)
				.build());

		assertField(products,
			new StringField
				.Builder("type")
				.setMaxLength(20)
				.setEnums(TYPES)
				.setOptional()
				.build());

		assertField(products,
			new StringField
				.Builder("modality")
				.setMaxLength(25)
				.setEnums(MODALITY)
				.build());

		assertField(products,
			new StringField
				.Builder("optionalCoverage")
				.setMaxLength(1024)
				.setOptional()
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("productDetails")
				.setValidator(this::assertProductDetails)
				.setOptional()
				.build());

		assertField(products,
			new ObjectField
				.Builder("minimumRequirements")
				.setValidator(minimumRequirements -> {
					assertField(minimumRequirements,
						new StringField
							.Builder("contractType")
							.setMaxLength(15)
							.setEnums(CONTRACT_TYPE)
							.build());

					assertField(minimumRequirements,
						new BooleanField
							.Builder("participantQualified")
							.build());

					assertField(minimumRequirements,
						new StringField
							.Builder("minRequirementsContract")
							.setMaxLength(1024)
							.build());
				})
				.setOptional()
				.build());

		assertField(products,
			new StringField
				.Builder("targetAudience")
				.setEnums(TARGET_AUDIENCE)
				.build());
	}

	private void assertProductDetails(JsonObject productDetails) {
		assertField(productDetails,
			new StringField
				.Builder("susepProcessNumber")
				.setMaxLength(30)
				.build());

		assertField(productDetails,
			new StringField
				.Builder("contractTermsConditions")
				.setMaxLength(1024)
				.build());

		assertField(productDetails,
			new ObjectField
				.Builder("defferalPeriod")
				.setValidator(this::assertDefferalPeriod)
				.build());

		assertField(productDetails,
			new ObjectField
				.Builder("grantPeriodBenefit")
				.setValidator(this::assertGrantPeriodBenefit)
				.build());

		assertField(productDetails,
			new ObjectField
				.Builder("costs")
				.setValidator(this::assertCosts)
				.build());
	}

	private void assertCosts(JsonObject costs) {
		assertField(costs,
			new ObjectField
				.Builder("loadingAntecipated")
				.setValidator(this::assertMinAndMaxValues)
				.build());

		assertField(costs,
			new ObjectField
				.Builder("loadingLate")
				.setValidator(this::assertMinAndMaxValues)
				.build());
	}

	private void assertMinAndMaxValues(JsonObject values) {
		assertField(values,
			new NumberField
				.Builder("minValue")
				.setMaxLength(7)
				.build());

		assertField(values,
			new NumberField
				.Builder("maxValue")
				.setMaxLength(8)
				.build());
	}

	private void assertGrantPeriodBenefit(JsonObject grantPeriodBenefit) {
		assertField(grantPeriodBenefit,
			new StringArrayField
				.Builder("incomeModality")
				.setEnums(INCOME_MODALITY)
				.setMaxLength(100)
				.build());

		assertField(grantPeriodBenefit,
			new StringArrayField
				.Builder("biometricTable")
				.setMaxLength(100)
				.setEnums(BIOMETRIC_TABLE)
				.build());

		assertField(grantPeriodBenefit,
			new NumberField
				.Builder("interestRate")
				.setMaxLength(6)
				.build());

		assertField(grantPeriodBenefit,
			new StringField
				.Builder("updateIndex")
				.setEnums(UPDATE_INDEX)
				.setMaxLength(20)
				.build());

		assertField(grantPeriodBenefit,
			new NumberField
				.Builder("reversalResultsFinancial")
				.setMaxLength(8)
				.build());

		assertField(grantPeriodBenefit,
			new ObjectArrayField
				.Builder("investimentFunds")
				.setValidator(this::assertInvestimentFunds)
				.build());

	}

	private void assertDefferalPeriod(JsonObject defferalPeriod) {
		assertField(defferalPeriod,
			new NumberField
				.Builder("interestRate")
				.setMaxLength(10)
				.build());

		assertField(defferalPeriod,
			new StringField
				.Builder("updateIndex")
				.setEnums(UPDATE_INDEX)
				.setMaxLength(12)
				.build());

		assertField(defferalPeriod,
			new StringField
				.Builder("otherMinimumPerformanceGarantees")
				.setMaxLength(12)
				.build());

		assertField(defferalPeriod,
			new NumberField
				.Builder("reversalFinancialResults")
				.setMaxLength(5)
				.build());

		assertField(defferalPeriod,
			new ObjectArrayField
				.Builder("minimumPremiumAmount")
				.setValidator(minimumPremiumAmount -> {
					assertField(minimumPremiumAmount,
						new NumberField
							.Builder("minimumPremiumAmountValue")
							.setMaxLength(13)
							.setOptional()
							.build());

					assertField(minimumPremiumAmount,
						new StringField
							.Builder("minimumPremiumAmountDescription")
							.setMaxLength(15)
							.setOptional()
							.build());
				})
				.build());

		assertField(defferalPeriod,
			new StringArrayField
				.Builder("premiumPaymentMethod")
				.setEnums(PREMIUM_PAYMENT_METHOD)
				.setOptional()
				.build());

		assertField(defferalPeriod,
			new BooleanField
				.Builder("permissionExtraordinaryContributions")
				.setOptional()
				.build());

		assertField(defferalPeriod,
			new BooleanField
				.Builder("permissonScheduledFinancialPayments")
				.build());

		assertField(defferalPeriod,
			new IntField
				.Builder("gracePeriodRedemption")
				.setMaxLength(4)
				.build());

		assertField(defferalPeriod,
			new IntField
				.Builder("gracePeriodBetweenRedemptionRequests")
				.setMaxLength(4)
				.build());

		assertField(defferalPeriod,
			new IntField
				.Builder("redemptionPaymentTerm")
				.setMaxLength(4)
				.build());

		assertField(defferalPeriod,
			new IntField
				.Builder("gracePeriodPortability")
				.setMaxLength(4)
				.build());

		assertField(defferalPeriod,
			new IntField
				.Builder("gracePeriodBetweenPortabilityRequests")
				.setMaxLength(4)
				.build());

		assertField(defferalPeriod,
			new IntField
				.Builder("portabilityPaymentTerm")
				.setMaxLength(4)
				.build());

		assertField(defferalPeriod,
			new ObjectArrayField
				.Builder("investimentFunds")
				.setValidator(this::assertInvestimentFunds)
				.build());
	}

	private void assertInvestimentFunds(JsonObject investimentFunds) {
		assertField(investimentFunds,
			new StringField
				.Builder("cnpjNumber")
				.setMaxLength(18)
				.build());

		assertField(investimentFunds,
			new StringField
				.Builder("companyName")
				.setMaxLength(80)
				.build());

		assertField(investimentFunds,
			new NumberField
				.Builder("maximumAdministrationFee")
				.setMaxLength(5)
				.build());

		assertField(investimentFunds,
			new StringArrayField
				.Builder("typePerformanceFee")
				.setMaxLength(15)
				.setEnums(TYPE_PERFORMANCE_FEE)
				.build());

		assertField(investimentFunds,
			new NumberField
				.Builder("maximumPerformanceFee")
				.setMaxLength(5)
				.setOptional()
				.build());

		assertField(investimentFunds,
			new BooleanField
				.Builder("eligibilityRule")
				.build());

		assertField(investimentFunds,
			new NumberField
				.Builder("minimumContributionAmount")
				.setMaxLength(5)
				.build());

		assertField(investimentFunds,
			new NumberField
				.Builder("minimumMathematicalProvisionAmount")
				.setMaxLength(5)
				.build());
	}
}
