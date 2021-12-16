package net.openid.conformance.openbanking_brasil.investments;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;


/**
 * Api Swagger URL: https://sensedia.github.io/areadesenvolvedor/swagger/swagger_capitalization_bonds_apis.yaml
 * Api endpoint: /products
 * Api version: 1.0.0
 */

public class CapitalizationBonds extends AbstractJsonAssertingCondition {

	public static final Set<String> TARGET_AUDIENCE = Sets.newHashSet("PESSOA_NATURAL","PESSOA_JURIDICA ");
	public static final Set<String> MODALITY = Sets.newHashSet("TRADICIONAL", "INSTRUMENTO_DE_GARANTIA", "COMPRA_PROGRAMADA",
		"POPULAR", "INCENTIVO", "FILANTROPIA_PREMIAVEL");
	public static final Set<String> PAYMENT_TERM = Sets.newHashSet("PAGAMENTO_UNICO", "PAGAMENTO_MENSAL", "PAGAMENTO_PERIODICO");
	public static final Set<String> INDEX = Sets.newHashSet( "IPCA_IBGE", "IGPM_FGV", "INPC_IBGE", "TR_BC", "OUTROS");
	public static final Set<String> CONTRIBUTION_PERIODICITY = Sets.newHashSet( "PAGAMENTO_MENSAL", "PAGAMENTO_UNICO", "PERIODICO");
	public static final Set<String> PERIODICITY = Sets.newHashSet( "UNICO", "DIÁRIO", "SEMANAL", "QUINZENAL", "MENSAL", "BIMESTRAL", "TRIMESTRAL", "QUADRIMESTRAL", "SEMESTRAL", "ANUAL", "OUTROS" );
	public static final Set<String> YES_NO = Sets.newHashSet( "SIM", "NAO");
	public static final Set<String> PAYMENT_METHOD = Sets.newHashSet("CARTAO_DE_CREDITO",
		"CARTAO_DE_DEBITO",
		"DEBITO_EM_CONTA_CORRENTE",
		"DEBITO_EM_CONTA_POUPANCA",
		"BOLETO_BANCARIO",
		"PIX",
		"CONSIGNACAO_EM_FOLHA_DE_PAGAMENTO",
		"PONTOS_DE_PROGRAMAS_DE_BENEFICIO",
		"OUTROS");

	private static class Fields extends ProductNServicesCommonFields {}

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField.Builder("data")
				.setValidator(data->{

					assertField(data,
						new ObjectField
							.Builder("participantIdentification")
							.setValidator(participantIdentification->
								assertField(participantIdentification,
									new ObjectField
										.Builder("brand")
										.setValidator(this::assertBrand)
										.build()))
							.build());

					assertField(data,
						new ObjectField
							.Builder("insuranceCompanyIdentification")
							.setValidator(insuranceCompanyIdentification->{
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
							.setValidator(termsAndConditions->{
								assertField(termsAndConditions,
									new StringField
										.Builder("susepNumber")
										.setPattern("(\\d{5}\\.\\d{6}\\/\\d{4}-\\d{2})")
										.setMaxLength(20)
										.build());

								assertField(termsAndConditions,
									new StringField
										.Builder("generalConditions")
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
						new NumberField
							.Builder("periodMonths")
							.setMaxLength(3)
							.build());

					assertField(data,
						new NumberField
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
						new NumberField
							.Builder("finalRedemptionRate")
							.setMaxLength(7)
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
							.Builder("minimumRequirements")
							.setMaxLength(1024)
							.setOptional()
							.build());

					assertField(data,
						new StringArrayField
							.Builder("targetAudience")
							.setMinItems(1)
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
				.Builder("periodicity")
				.setMaxLength(13)
				.setEnums(PERIODICITY)
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
			new StringField
				.Builder("liquidityEarlyRedemption")
				.setMaxLength(3)
				.setEnums(YES_NO)
				.build());

		assertField(draws,
			new StringField
				.Builder("mandatoryDraw")
				.setMaxLength(3)
				.setEnums(YES_NO)
				.build());

		assertField(draws,
			new StringField
				.Builder("additionalConditions")
				.setMaxLength(200)
				.setOptional()
				.build());

		assertField(draws,
			new NumberField
				.Builder("minimumProbability")
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
			new NumberField
				.Builder("capitalization")
				.setMaxLength(9)
				.build());

		assertField(quotas,
			new NumberField
				.Builder("drawing")
				.setMaxLength(9)
				.build());

		assertField(quotas,
			new NumberField
				.Builder("loadingRate")
				.setMaxLength(9)
				.build());
	}

	private void assertContributionPayment(JsonObject contributionPayment) {
		assertField(contributionPayment,
			new StringField
				.Builder("paymentMethod")
				.setMaxLength(33)
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
				.setMaxLength(9)
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
			new StringField
				.Builder("periodExtensionOption")
				.setMaxLength(3)
				.setEnums(YES_NO)
				.build());
	}

	private void assertCapitalizationPeriod(JsonObject capitalizationPeriod) {
		assertField(capitalizationPeriod,
			new NumberField
				.Builder("interestRate")
				.setMaxLength(7)
				.build());

		assertField(capitalizationPeriod,
			new StringField
				.Builder("adjustmentIndex")
				.setMaxLength(9)
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
			new ObjectArrayField
				.Builder("earlyRedemptions")
				.setValidator(
					earlyRedemptions->{
						assertField(earlyRedemptions,
							new NumberField
								.Builder("tranche")
								.setMaxLength(3)
								.build());

						assertField(earlyRedemptions,
							new NumberField
								.Builder("rate")
								.setMaxLength(3)
								.build());
					})
				.setMinItems(1)
				.build());

		assertField(capitalizationPeriod,
			new NumberField
				.Builder("finalRedemptionRate")
				.setMaxLength(7)
				.build());

		assertField(capitalizationPeriod,
			new DoubleField
				.Builder("redemptionGraceMonths")
				.setMaxLength(3)
				.build());
	}

	private void assertContributionAmount(JsonObject contributionAmount) {
		assertField(contributionAmount,
			new StringField
				.Builder("periodicity")
				.setMaxLength(16)
				.setOptional()
				.setEnums(CONTRIBUTION_PERIODICITY)
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

		assertField(product,
			new StringField
				.Builder("productId")
				.setMaxLength(80)
				.build());

		assertField(product,
			new StringField
				.Builder("modality")
				.setMinLength(7)
				.setMaxLength(24)
				.setEnums(MODALITY)
				.build());

		assertField(product,
			new StringField
				.Builder("paymentTerm")
				.setMinLength(15)
				.setMaxLength(19)
				.setEnums(PAYMENT_TERM)
				.build());
	}

	private void assertBrand(JsonObject brand) {
		assertField(brand, Fields.name().setMaxLength(120).build());

		assertField(brand,
			new ObjectArrayField
				.Builder("companies")
				.setValidator(companies->{
						assertField(companies, Fields.name().setMaxLength(120).build());
						assertField(companies, Fields.cnpjNumber().setPattern("^\\d{14}$").setMaxLength(14).build());
					assertField(companies,
						new StringField
							.Builder("urlComplementaryList")
							.setPattern("^(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$")
							.setMaxLength(1024)
							.setOptional()
							.build());
				})
				.setOptional()
				.build());
	}
}
