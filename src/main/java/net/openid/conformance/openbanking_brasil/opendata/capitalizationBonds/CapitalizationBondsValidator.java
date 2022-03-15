package net.openid.conformance.openbanking_brasil.opendata.capitalizationBonds;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.opendata.OpenDataLinksAndMetaValidator;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;


/**
 * Api Swagger: swagger/opendata/swagger-CapitalizationBonds.yaml
 * Api endpoint: /bonds
 * Api version: 1.0.0-rc1.0
 * Git hash: f3774e4268d7cd7c8a5977a31dae8f727cc9153d
 */

@ApiName("CapitalizationBonds")
public class CapitalizationBondsValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> TARGET_AUDIENCE = Sets.newHashSet("PESSOA_NATURAL", "PESSOA_JURIDICA", "PESSOA_NATURAL_JURIDICA");
	public static final Set<String> MODALITY = Sets.newHashSet("TRADICIONAL","INSTRUMENTO_GARANTIA","COMPRA_PROGRAMADA","POPULAR","INCENTIVO","FILANTROPIA_PREMIAVEL");
	public static final Set<String> COST_TYPE = Sets.newHashSet("PAGAMENTO_UNICO","PAGAMENTO_MENSAL","PAGAMENTO_PERIODICO");
	public static final Set<String> INDEX = Sets.newHashSet("IPCA","IGPM","INPC","TR","INDICE_REMUNERACAO_DEPOSITOS_POUPANCA","OUTROS");
	public static final Set<String> TIME_INTERVAL = Sets.newHashSet("UNICO","DIÁRIO","SEMANAL","QUINZENAL","MENSAL","BIMESTRAL","TRIMESTRAL","QUADRIMESTRAL","SEMESTRAL","ANUAL","OUTROS");
	public static final Set<String> PAYMENT_METHOD = Sets.newHashSet("CARTAO_CREDITO","CARTAO_DEBITO","DEBITO_CONTA_CORRENTE","DEBITO_CONTA_POUPANCA","BOLETO_BANCARIO","PIX","CONSIGNACAO_FOLHA_PAGAMENTO","PAGAMENTO_PONTOS","OUTROS");


	private static class Fields extends ProductNServicesCommonFields {
	}
	private final OpenDataLinksAndMetaValidator linksAndMetaValidator = new OpenDataLinksAndMetaValidator(this);

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField.Builder("data")
				.setMinItems(1)
				.mustNotBeEmpty()
				.setValidator(data -> {
					assertField(data,
						new ObjectField
							.Builder("participant")
							.setValidator(this::assertParticipant)
							.build());

					assertField(data,
						new ObjectField
							.Builder("society")
							.setValidator(insuranceCompanyIdentification -> {
								assertField(insuranceCompanyIdentification, Fields.name().setMaxLength(80).build());
								assertField(insuranceCompanyIdentification, Fields.cnpjNumber().setPattern("^\\d{14}$").build());
							})
							.build());


					assertField(data, Fields.name().setMaxLength(80).build());
					assertField(data, Fields.code().setMaxLength(100).build());

					assertField(data,
						new StringField
							.Builder("modality")
							.setMinLength(7)
							.setMaxLength(24)
							.setEnums(MODALITY)
							.build());

					assertField(data,
						new StringField
							.Builder("costType")
							.setMinLength(15)
							.setMaxLength(19)
							.setEnums(COST_TYPE)
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
										.Builder("detail")
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
							.setOptional()
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
							.setMaxLength(7)
							.setPattern("^[0-1]\\.\\d{5}$")
							.build());

					assertField(data,
						new StringField
							.Builder("finalRedemptionRate")
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
						new StringField
							.Builder("targetAudience")
							.setMaxLength(23)
							.setEnums(TARGET_AUDIENCE)
							.build());
				})
				.build());

		linksAndMetaValidator.assertMetaAndLinks(body);

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
				.Builder("timeIntervalAdditionalInfo")
				.setMaxLength(200)
				.setOptional()
				.build());

		assertField(draws,
			new IntField
				.Builder("quantity")
				.setMaxLength(5)
				.build());

		assertField(draws,
			new IntField
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
				.Builder("quota")
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
				.Builder("updateIndex")
				.setMaxLength(37)
				.setEnums(INDEX)
				.build());

		assertField(contributionPayment,
			new StringField
				.Builder("updateIndexAdditionalInfo")
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
				.setPattern("^[0-1]\\.[\\d]{6}$")
				.setMaxLength(8)
				.build());

		assertField(capitalizationPeriod,
			new StringField
				.Builder("updateIndex")
				.setMaxLength(37)
				.setEnums(INDEX)
				.build());

		assertField(capitalizationPeriod,
			new StringField
				.Builder("updateIndexAdditionalInfo")
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
			new ObjectArrayField
				.Builder("earlyRedemptions")
				.setValidator(earlyRedemptions -> {
					assertField(earlyRedemptions,
						new IntField
							.Builder("quota")
							.setMaxLength(3)
							.build());

					assertField(earlyRedemptions,
						new NumberField
							.Builder("rate")
							.setMaxLength(9)
							.build());
				})
				.setMinItems(1)
				.build());

		assertField(capitalizationPeriod,
			new StringField
				.Builder("redemptionPercentageEndTerm")
				.setMaxLength(7)
				.setPattern("^[0-1]\\.\\d{5}$")
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
				.Builder("periodicity")
				.setMaxLength(13)
				.setOptional()
				.setEnums(TIME_INTERVAL)
				.build());

		assertField(contributionAmount,
			new StringField
				.Builder("periodicityAdditionalInfo")
				.setMaxLength(200)
				.setOptional()
				.build());

		assertField(contributionAmount,
			new StringField
				.Builder("minimum")
				.setPattern("^\\d{1,16}\\.\\d{2,4}$")
				.setMaxLength(21)
				.build());

		assertField(contributionAmount,
			new StringField
				.Builder("maximum")
				.setPattern("^\\d{1,16}\\.\\d{2,4}$")
				.setMaxLength(21)
				.build());

		assertField(contributionAmount,
			new NumberField
				.Builder("allowedValue")
				.setMaxLength(8)
				.build());
	}

	private void assertParticipant(JsonObject assertParticipantIdentification) {
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
