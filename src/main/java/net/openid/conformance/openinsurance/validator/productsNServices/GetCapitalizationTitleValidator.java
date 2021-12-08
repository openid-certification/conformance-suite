package net.openid.conformance.openinsurance.validator.productsNServices;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.CommonValidatorParts;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api endpoint: /capitalization-title/
 * Api version: 1.0.0
 */

@ApiName("ProductsNServices Capitalization Title")
public class GetCapitalizationTitleValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> MODALITY = Sets.newHashSet("TRADICIONAL", "INSTRUMENTO_GARANTIA", "COMPRA_PROGRAMADA", "POPULAR", "INCENTIVO", "FILANTROPIA_PREMIAVEL");
	public static final Set<String> UPDATE_INDEX = Sets.newHashSet("IPCA", "IGPM", "INPC", "TR", "INDICE_REMUNERACAO_DEPOSITOS_POUPANCA", "OUTROS");
	public static final Set<String> COST_TYPE = Sets.newHashSet("PAGMENTO_UNICO", "PAGAMENTO_MENSAL", "PAGAMENTO_PERIODICO");
	public static final Set<String> FREQUENCY = Sets.newHashSet("MENSAL", "UNICO", "PERIODICO");
	public static final Set<String> PAYMENT_METHOD = Sets.newHashSet("CARTAO_CREDITO", "CARTAO_DEBITO", "DEBITO_CONTA_CORRENTE", "DEBITO_CONTA_POUPANCA", "BOLETO_BANCARIO", "PIX", "CONSIGNACAO_FOLHA_PAGAMENTO", "PAGAMENTO_COM_PONTOS", "OUTROS");
	public static final Set<String> TIME_INTERVAL = Sets.newHashSet("UNICO", "DIARIO", "SEMANAL", "QUINZENAL", "MENSAL", "BIMESTRAL", "TRIMESTRAL", "QUADRIMESTRAL", "SEMESTRAL", "ANUAL", "OUTROS");
	public static final Set<String> TARGET_AUDIENCE = Sets.newHashSet("PESSOAL_NATURAL", "PESSOA_JURIDICA");

	private static class Fields extends ProductNServicesCommonFields { }

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body,
			new DatetimeField
				.Builder("requestTime")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(2048)
				.setOptional()
				.build());

		assertField(body, new ObjectField
			.Builder("brand")
			.setValidator(brand -> {
				assertField(brand, Fields.name().setMaxLength(80).build());
				assertField(brand, new ObjectField
					.Builder("companies")
					.setValidator(this::assertCompany)
					.build());
			})
			.build());

		logFinalStatus();
		return environment;
	}

	private void assertCompany(JsonObject companies) {
		assertField(companies, Fields.name().setMaxLength(80).build());
		assertField(companies, Fields.cnpjNumber().setMaxLength(14).build());

		assertField(companies,
			new ObjectArrayField
				.Builder("product")
				.setValidator(this::assertProduct)
				.build());
	}

	private void assertProduct(JsonObject product) {
		assertField(product, Fields.name().setMaxLength(80).build());

		assertField(product, Fields.code().setMaxLength(100).build());

		assertField(product,
			new StringArrayField
				.Builder("modality")
				.setEnums(MODALITY)
				.build());

		assertField(product,
			new StringArrayField
				.Builder("costType")
				.setEnums(COST_TYPE)
				.build());

		assertField(product,
			new ObjectField
				.Builder("termsAndConditions")
				.setValidator(termsAndConditions -> {
					assertField(termsAndConditions,
						new StringField
							.Builder("susepProcessNumber")
							.setMaxLength(20)
							.build());

					assertField(termsAndConditions,
						new StringField
							.Builder("termsRegulations")
							.setMaxLength(1024)
							.build());
				})
				.setOptional()
				.build());

		assertField(product,
			new ObjectField
				.Builder("quotas")
				.setValidator(quotas -> {
					assertField(quotas,
						new StringField
							.Builder("capitalizationQuota")
							.setMaxLength(9)
							.build());

					assertField(quotas,
						new StringField
							.Builder("raffleQuota")
							.setMaxLength(9)
							.build());

					assertField(quotas,
						new StringField
							.Builder("chargingQuota")
							.setMaxLength(9)
							.build());
				})
				.setOptional()
				.build());

		assertField(product,
			new IntField
				.Builder("validity")
				.setMaxLength(3)
				.setOptional()
				.build());

		assertField(product,
			new IntField
				.Builder("serieSize")
				.setMaxLength(10)
				.setOptional()
				.build());

		assertField(product,
			new ObjectField
				.Builder("capitalizationPeriod")
				.setValidator(this::assertCapitalizationPeriod)
				.setOptional()
				.build());

		assertField(product,
			new ObjectField
				.Builder("latePayment")
				.setValidator(latePayment -> {
					assertField(latePayment,
						new IntField
							.Builder("suspensionPeriod")
							.setMaxLength(3)
							.build());

					assertField(latePayment,
						new BooleanField
							.Builder("termExtensionOption")
							.build());
				})
				.setOptional()
				.build());

		assertField(product,
			new ObjectField
				.Builder("contributionPayment")
				.setValidator(contributionPayment -> {
					assertField(contributionPayment,
						new StringArrayField
							.Builder("paymentMethod")
							.setEnums(PAYMENT_METHOD)
							.build());

					assertField(contributionPayment,
						new StringArrayField
							.Builder("updateIndex")
							.setEnums(UPDATE_INDEX)
							.build());

					assertField(contributionPayment,
						new StringArrayField
							.Builder("others")
							.setOptional()
							.build());
				})
				.build());

		assertField(product,
			new ObjectField
				.Builder("redemption")
				.setValidator(redemption -> {
					assertField(redemption,
						new StringField
							.Builder("redemption")
							.setMaxLength(6)
							.build());
				})
				.setOptional()
				.build());

		assertField(product,
			new ObjectField
				.Builder("raffle")
				.setValidator(this::assertRaffle)
				.setOptional()
				.build());

		assertField(product,
			new ObjectField
				.Builder("additionalDetails")
				.setValidator(additionalDetails -> {
					assertField(additionalDetails,
						new StringField
							.Builder("additionalDetails")
							.setMaxLength(1024)
							.build());
				})
				.setOptional()
				.build());

		assertField(product,
			new ObjectField
				.Builder("minimumRequirements")
				.setValidator(minimumRequirements -> {
					assertField(minimumRequirements,
						new StringField
							.Builder("minimumRequirementDetails")
							.setMaxLength(1024)
							.build());

					assertField(minimumRequirements,
						new StringArrayField
							.Builder("targetAudience")
							.setEnums(TARGET_AUDIENCE)
							.build());
				})
				.setOptional()
				.build());
	}

	private void assertRaffle(JsonObject raffle) {
		assertField(raffle,
			new IntField
				.Builder("raffleQty")
				.setMaxLength(5)
				.build());

		assertField(raffle,
			new StringArrayField
				.Builder("timeInterval")
				.setEnums(TIME_INTERVAL)
				.build());

		assertField(raffle,
			new IntField
				.Builder("raffleValue")
				.setMaxLength(6)
				.build());

		assertField(raffle,
			new BooleanField
				.Builder("earlySettlementRaffle")
				.build());

		assertField(raffle,
			new BooleanField
				.Builder("mandatoryContemplation")
				.build());

		assertField(raffle,
			new StringField
				.Builder("ruleDescription")
				.setMaxLength(200)
				.setOptional()
				.build());

		assertField(raffle,
			new StringField
				.Builder("minimumContemplationProbability")
				.setMaxLength(8)
				.build());
	}

	private void assertCapitalizationPeriod(JsonObject capitalizationPeriod) {
		assertField(capitalizationPeriod,
			new StringField
				.Builder("interestRate")
				.setMaxLength(7)
				.build());

		assertField(capitalizationPeriod,
			new StringArrayField
				.Builder("updateIndex")
				.setEnums(UPDATE_INDEX)
				.build());

		assertField(capitalizationPeriod,
			new StringArrayField
				.Builder("others")
				.setOptional()
				.build());

		assertField(capitalizationPeriod,
			new ObjectField
				.Builder("contributionAmount")
				.setValidator(this::assertContributionAmount)
				.build());

		assertField(capitalizationPeriod,
			new StringField
				.Builder("earlyRedemption")
				.setMaxLength(9)
				.build());

		assertField(capitalizationPeriod,
			new StringField
				.Builder("redemptionPercentageEndTerm")
				.setMaxLength(7)
				.build());

		assertField(capitalizationPeriod,
			new IntField
				.Builder("gracePeriodRedemption")
				.setMaxLength(3)
				.build());
	}

		private void assertContributionAmount(JsonObject contributionAmount) {
			assertField(contributionAmount,
				new IntField
					.Builder("minValue")
					.setOptional()
					.build());

			assertField(contributionAmount,
				new IntField
					.Builder("maxValue")
					.setOptional()
					.build());

			assertField(contributionAmount,
				new StringField
					.Builder("frequency")
					.setEnums(FREQUENCY)
					.setOptional()
					.build());

			assertField(contributionAmount,
				new IntField
					.Builder("value")
					.setOptional()
					.build());
	}
}
