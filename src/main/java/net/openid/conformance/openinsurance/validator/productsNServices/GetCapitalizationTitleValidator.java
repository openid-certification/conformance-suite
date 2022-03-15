package net.openid.conformance.openinsurance.validator.productsNServices;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.openinsurance.validator.OpenInsuranceLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.NumberArrayField;
import net.openid.conformance.util.field.NumberField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api source: swagger/openinsurance/swagger-productsnservices-capitalizationtitle.yaml
 * Api endpoint: /capitalization-title/
 * Api version: 1.0.2
 * Git hash: b5dcb30363a2103b9d412bc3c79040696d2947d2
 */

@ApiName("ProductsNServices Capitalization Title")
public class GetCapitalizationTitleValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> MODALITY = Sets.newHashSet("TRADICIONAL", "INSTRUMENTO_GARANTIA", "COMPRA_PROGRAMADA", "POPULAR", "INCENTIVO", "FILANTROPIA_PREMIAVEL");
	public static final Set<String> UPDATE_INDEX = Sets.newHashSet("IPCA", "IGPM", "INPC", "TR", "INDICE_REMUNERACAO_DEPOSITOS_POUPANCA", "OUTROS");
	public static final Set<String> COST_TYPE = Sets.newHashSet("PAGAMENTO_UNICO", "PAGAMENTO_MENSAL", "PAGAMENTO_PERIODICO");
	public static final Set<String> FREQUENCY = Sets.newHashSet("MENSAL", "UNICO", "PERIODICO");
	public static final Set<String> PAYMENT_METHOD = Sets.newHashSet("CARTAO_CREDITO", "CARTAO_DEBITO", "DEBITO_CONTA_CORRENTE", "DEBITO_CONTA_POUPANCA", "BOLETO_BANCARIO", "PIX", "CONSIGNACAO_FOLHA_PAGAMENTO", "PAGAMENTO_COM_PONTOS", "OUTROS");
	public static final Set<String> TIME_INTERVAL = Sets.newHashSet("UNICO", "DIARIO", "SEMANAL", "QUINZENAL", "MENSAL", "BIMESTRAL", "TRIMESTRAL", "QUADRIMESTRAL", "SEMESTRAL", "ANUAL", "OUTROS");
	public static final Set<String> TARGET_AUDIENCE = Sets.newHashSet("PESSOAL_NATURAL", "PESSOA_JURIDICA");

	private static class Fields extends ProductNServicesCommonFields { }

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

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
				assertField(brand, new ObjectArrayField
					.Builder("companies")
					.setValidator(this::assertCompany)
					.build());
			})
			.build());
		new OpenInsuranceLinksAndMetaValidator(this).assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertCompany(JsonObject companies) {
		assertField(companies, Fields.name().setMaxLength(80).build());
		assertField(companies, Fields.cnpjNumber().setMaxLength(14).build());

		assertField(companies,
			new ObjectArrayField
				.Builder("products")
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
						new NumberField
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
			new ObjectArrayField
				.Builder("quotas")
				.setValidator(quotas -> {
					assertField(quotas,
						new IntField
							.Builder("quota")
							.build());

					assertField(quotas,
						new NumberArrayField
							.Builder("capitalizationQuota")
							.setMaxLength(9)
							.build());

					assertField(quotas,
						new NumberArrayField
							.Builder("raffleQuota")
							.setMaxLength(9)
							.build());

					assertField(quotas,
						new NumberArrayField
							.Builder("chargingQuota")
							.setMaxLength(9)
							.build());
				})
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
							.Builder("updateIndexOthers")
							.setOptional()
							.build());
				})
				.setOptional()
				.build());

		assertField(product,
			new ObjectField
				.Builder("redemption")
				.setValidator(redemption -> assertField(redemption,
					new NumberField
						.Builder("redemption")
						.setMaxLength(6)
						.build()))
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
				.setValidator(additionalDetails -> assertField(additionalDetails,
					new StringField
						.Builder("additionalDetails")
						.setMaxLength(1024)
						.build()))
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
							.Builder("targetAudiences")
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
			new NumberField
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
			new NumberField
				.Builder("minimumContemplationProbability")
				.setMaxLength(8)
				.build());
	}

	private void assertCapitalizationPeriod(JsonObject capitalizationPeriod) {
		assertField(capitalizationPeriod,
			new NumberField
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
			new ObjectArrayField
				.Builder("earlyRedemption")
				.setValidator(earlyRedemption -> {
					assertField(earlyRedemption,
						new IntField
							.Builder("quota")
							.build());

					assertField(earlyRedemption,
						new NumberField
							.Builder("percentage")
							.build());
				})
				.setOptional()
				.build());

		assertField(capitalizationPeriod,
			new NumberField
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
				new NumberField
					.Builder("minValue")
					.setOptional()
					.build());

			assertField(contributionAmount,
				new NumberField
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
				new NumberField
					.Builder("value")
					.setOptional()
					.build());
	}
}
