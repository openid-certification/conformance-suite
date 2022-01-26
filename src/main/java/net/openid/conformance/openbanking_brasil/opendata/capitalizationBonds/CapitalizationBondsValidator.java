package net.openid.conformance.openbanking_brasil.opendata.capitalizationBonds;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;


/**
 * Api Swagger URL: https://sensedia.github.io/areadesenvolvedor/swagger/swagger_capitalization_bonds_apis.yaml
 * Api endpoint: /products
 * Api version: 1.0.0
 */

@ApiName("CapitalizationBonds")
public class CapitalizationBondsValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> TARGET_AUDIENCE = Sets.newHashSet("PESSOA_NATURAL", "PESSOA_JURIDICA");
	public static final Set<String> MODALITY = Sets.newHashSet("TRADICIONAL","INSTRUMENTO_GARANTIA","COMPRA_PROGRAMADA","POPULAR","INCENTIVO","FILANTROPIA_PREMIAVEL");
	public static final Set<String> COST_TYPE = Sets.newHashSet("PAGAMENTO_UNICO","PAGAMENTO_MENSAL","PAGAMENTO_PERIODICO ");
	public static final Set<String> INDEX = Sets.newHashSet("IPCA","IGPM","INPC","TR","INDICE_REMUNERACAO_DEPOSITOS_POUPANCA","OUTROS");
	public static final Set<String> TIME_INTERVAL_CONTIRBUTION_AMOUNT = Sets.newHashSet("PAGAMENTO_MENSAL", "PAGAMENTO_UNICO", "PERIODICO");
	public static final Set<String> TIME_INTERVAL = Sets.newHashSet("UNICO","DIÁRIO","SEMANAL","QUINZENAL","MENSAL","BIMESTRAL","TRIMESTRAL","QUADRIMESTRAL","SEMESTRAL","ANUAL","OUTROS");
	public static final Set<String> PAYMENT_METHOD = Sets.newHashSet("CARTAO_CREDITO","CARTAO_DEBITO","DEBITO_CONTA_CORRENTE","DEBITO_CONTA_POUPANCA","BOLETO_BANCARIO","PIX","CONSIGNACAO_FOLHA_PAGAMENTO","PAGAMENTO_COM_PONTOS","OUTROS");


	private static class Fields extends ProductNServicesCommonFields {
	}

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField.Builder("data")
				.setMinItems(1)
				.setValidator(data -> {
					assertField(data,
						new ObjectField
							.Builder("participantIdentification")
							.setValidator(this::assertParticipantIdentification)
							.build());

					assertField(data,
						new ObjectField
							.Builder("insuranceCompanyIdentification")
							.setValidator(insuranceCompanyIdentification -> {
								assertField(insuranceCompanyIdentification, Fields.name().setMaxLength(80).build());
								assertField(insuranceCompanyIdentification, Fields.cnpjNumber().setPattern("^\\d{14}$").setMaxLength(14).build());
							})
							.build());

					assertField(data,
						new ObjectField
							.Builder("product")
							.setValidator(this::assertProduct)
							.build());

					assertField(data,
						new ObjectField
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
										.Builder("termsRegulations")
										.setMaxLength(1024)
										.build());
							})
							.build());

					assertField(data,
						new ObjectArrayField
							.Builder("quotas")
							.setValidator(this::assertQuotas)
							.setMinItems(1)
							.build());

					assertField(data,
						new IntField
							.Builder("validity")
							.setMaxLength(3)
							.build());

					assertField(data,
						new IntField
							.Builder("serieSize")
							.setMaxLength(10)
							.setOptional()
							.build());

					assertField(data,
						new ObjectField
							.Builder("capitalizationPeriod")
							.setValidator(this::assertCapitalizationPeriod)
							.build());

					assertField(data,
						new ObjectField
							.Builder("latePayment")
							.setValidator(this::assertLatePayment)
							.build());

					assertField(data,
						new ObjectField
							.Builder("contributionPayment")
							.setValidator(this::assertContributionPayment)
							.build());

					assertField(data,
						new StringField
							.Builder("redemptionPercentageEndTerm")
							.setMaxLength(4)
							.setPattern("^[0-9]\\.\\d{2}$")
							.build());

					assertField(data,
						new ObjectArrayField
							.Builder("draws")
							.setValidator(this::assertDraws)
							.setMinItems(1)
							.build());

					assertField(data,
						new StringField
							.Builder("additionalInfo")
							.setMaxLength(1024)
							.build());

					assertField(data,
						new StringField
							.Builder("minimumRequirementDetails")
							.setMaxLength(1024)
							.setOptional()
							.build());

					assertField(data,
						new StringArrayField
							.Builder("targetAudiences")
							.setMinItems(1)
							.setMaxLength(15)
							.setEnums(TARGET_AUDIENCE)
							.build());
				})
				.build());

		logFinalStatus();
		return environment;
	}

	private void assertDraws(JsonObject draws) {
		assertField(draws,
			new StringField
				.Builder("timeInterval")
				.setMaxLength(13)
				.setEnums(TIME_INTERVAL)
				.build());

		assertField(draws,
			new StringField
				.Builder("periodicityAdditionalInfo")
				.setMaxLength(200)
				.setOptional()
				.build());

		assertField(draws,
			new NumberField
				.Builder("quantity")
				.setMaxLength(5)
				.build());

		assertField(draws,
			new NumberField
				.Builder("prizeMultiplier")
				.setMaxLength(6)
				.build());

		assertField(draws,
			new BooleanField
				.Builder("earlySettlementRaffle")
				.build());

		assertField(draws,
			new BooleanField
				.Builder("mandatoryContemplation")
				.setOptional()
				.build());

		assertField(draws,
			new StringField
				.Builder("ruleDescription")
				.setMaxLength(200)
				.setOptional()
				.build());

		assertField(draws,
			new StringField
				.Builder("minimumContemplationProbability")
				.setPattern("^[0-1]\\.\\d{6}$")
				.setMaxLength(8)
				.build());
	}

	private void assertQuotas(JsonObject quotas) {
		assertField(quotas,
			new NumberField
				.Builder("tranche")
				.setMaxLength(3)
				.build());

		assertField(quotas,
			new StringField
				.Builder("capitalizationQuota")
				.setMaxLength(8)
				.setPattern("^[0-1]\\.\\d{6}$")
				.build());

		assertField(quotas,
			new StringField
				.Builder("raffleQuota")
				.setMaxLength(8)
				.setPattern("^[0-1]\\.\\d{6}$")
				.build());

		assertField(quotas,
			new StringField
				.Builder("chargingQuota")
				.setMaxLength(8)
				.setPattern("^[0-1]\\.\\d{6}$")
				.build());
	}

	private void assertContributionPayment(JsonObject contributionPayment) {
		assertField(contributionPayment,
			new StringField
				.Builder("paymentMethod")
				.setMaxLength(27)
				.setEnums(PAYMENT_METHOD)
				.build());

		assertField(contributionPayment,
			new StringField
				.Builder("paymentMethodAdditionalInfo")
				.setMaxLength(200)
				.setOptional()
				.build());

		assertField(contributionPayment,
			new StringField
				.Builder("adjustmentIndex")
				.setMaxLength(37)
				.setEnums(INDEX)
				.build());

		assertField(contributionPayment,
			new StringField
				.Builder("adjustmentIndexAdditionalInfo")
				.setMaxLength(200)
				.setOptional()
				.build());
	}

	private void assertLatePayment(JsonObject latePayment) {
		assertField(latePayment,
			new NumberField
				.Builder("suspensionMonths")
				.setMaxLength(3)
				.build());

		assertField(latePayment,
			new BooleanField
				.Builder("periodExtensionOption")
				.build());
	}

	private void assertCapitalizationPeriod(JsonObject capitalizationPeriod) {
		assertField(capitalizationPeriod,
			new StringField
				.Builder("interestRate")
				.setPattern("^\\d{1}.\\d{5}$")
				.setMaxLength(7)
				.build());

		assertField(capitalizationPeriod,
			new StringField
				.Builder("adjustmentIndex")
				.setMaxLength(37)
				.setEnums(INDEX)
				.build());

		assertField(capitalizationPeriod,
			new StringField
				.Builder("adjustmentIndexAdditionalInfo")
				.setMaxLength(200)
				.setOptional()
				.build());

		assertField(capitalizationPeriod,
			new ObjectArrayField
				.Builder("contributionAmount")
				.setValidator(this::assertContributionAmount)
				.setMinItems(1)
				.build());

		assertField(capitalizationPeriod,
			new StringField
				.Builder("earlyRedemptions")
				.setPattern("^[0-1]\\.\\d{8}$")
				.setMaxLength(10)
				.build());

		assertField(capitalizationPeriod,
			new StringField
				.Builder("redemptionPercentageEndTerm")
				.setMaxLength(4)
				.setPattern("^[0-9]\\.\\d{2}$")
				.build());

		assertField(capitalizationPeriod,
			new NumberField
				.Builder("gracePeriodRedemption")
				.setMaxLength(3)
				.build());
	}

	private void assertContributionAmount(JsonObject contributionAmount) {
		assertField(contributionAmount,
			new StringField
				.Builder("timeInterval")
				.setMaxLength(16)
				.setOptional()
				.setEnums(TIME_INTERVAL_CONTIRBUTION_AMOUNT)
				.build());

		assertField(contributionAmount,
			new StringField
				.Builder("periodicityAdditionalInfo")
				.setMaxLength(200)
				.setOptional()
				.build());

		assertField(contributionAmount,
			new NumberField
				.Builder("minimum")
				.setMaxLength(8)
				.build());

		assertField(contributionAmount,
			new NumberField
				.Builder("maximum")
				.setMaxLength(8)
				.build());

		assertField(contributionAmount,
			new NumberField
				.Builder("allowed")
				.setMaxLength(8)
				.build());
	}

	private void assertProduct(JsonObject product) {
		assertField(product, Fields.name().setMaxLength(80).build());
		assertField(product, Fields.code().setMaxLength(100).build());

		assertField(product,
			new StringField
				.Builder("modality")
				.setMinLength(7)
				.setMaxLength(24)
				.setEnums(MODALITY)
				.build());

		assertField(product,
			new StringField
				.Builder("costType")
				.setMinLength(15)
				.setMaxLength(19)
				.setEnums(COST_TYPE)
				.build());
	}

	private void assertParticipantIdentification(JsonObject assertParticipantIdentification) {
		assertField(assertParticipantIdentification,
			new StringField
				.Builder("brand")
				.setMaxLength(80)
				.build());

		assertField(assertParticipantIdentification, Fields.name().setMaxLength(80).build());
		assertField(assertParticipantIdentification, Fields.cnpjNumber().setPattern("^\\d{14}$").build());
		assertField(assertParticipantIdentification,
			new StringField
				.Builder("urlComplementaryList")
				.setPattern("^(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$")
				.setMaxLength(1024)
				.setOptional()
				.build());

	}
}
