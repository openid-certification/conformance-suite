package net.openid.conformance.openbanking_brasil.productsNServices.creditCard;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductsNServicesCommonFields;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductsNServicesCommonValidatorParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * https://openbanking-brasil.github.io/areadesenvolvedor/swagger/swagger_products_services_apis.yaml
 *
 * URL: /business-credit-cards
 */

@ApiName("ProductsNServices BusinessCreditCard")
public class BusinessCreditCardValidator extends AbstractJsonAssertingCondition {

	private static class Fields extends ProductsNServicesCommonFields {}
	private final ProductsNServicesCommonValidatorParts parts;

	public BusinessCreditCardValidator() {
		parts = new ProductsNServicesCommonValidatorParts(this);
	}

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertJsonObject(body, ROOT_PATH,
			(data) -> assertField(data,
				new ObjectField.Builder("brand").setValidator(
					(brand) -> {
						assertField(brand, Fields.name().build());
						assertField(brand,
							new ObjectArrayField.Builder("companies")
								.setMinItems(1)
								.setValidator(this::assertCompanies)
								.build());
					}
				).build())
		);

		return environment;
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, Fields.cnpjNumber().build());
		assertField(companies, Fields.name().build());
		assertField(companies, Fields.urlComplementaryList().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("businessCreditCards")
				.setValidator(this::assertBusinessCreditCards)
				.setMinItems(1)
				.build());
	}

	private void assertBusinessCreditCards(JsonObject businessCreditCards) {
		Set<String> names = Sets.newHashSet("ANUIDADE_CARTAO_BASICO_NACIONAL",
			"ANUIDADE_CARTAO_BASICO_INTERNACIONAL", "ANUIDADE_DIFERENCIADA",
			"UTILIZACAO_CANAIS_ATENDIMENTO_RETIRADA_ESPECIE_BRASIL",
			"UTILIZACAO_CANAIS_ATENDIMENTO_RETIRADA_ESPECIE_EXTERIOR",
			"AVALIACAO_EMERGENCIAL_CREDITO", "FORNECIMENTO_SEGUNDA_VIA_FUNCAO_CREDITO",
			"PAGAMENTO_CONTAS_UTILIZANDO_FUNCAO_CREDITO", "SMS");
		Set<String> codes = Sets.newHashSet("ANUIDADE_NACIONAL", "ANUIDADE_INTERNACIONAL",
			"ANUIDADE_DIFERENCIADA", "SAQUE_CARTAO_BRASIL", "SAQUE_CARTAO_EXTERIOR",
			"AVALIACAO_EMERGENCIAL_CREDITO", "EMISSAO_SEGUNDA_VIA", "TARIFA_PAGAMENTO_CONTAS", "SMS");

		assertField(businessCreditCards, Fields.name().setMaxLength(50).build());

		assertField(businessCreditCards,
			new ObjectField
				.Builder("identification")
				.setValidator(this::assertInnerIdentification)
				.build());

		assertField(businessCreditCards,
			new ObjectField
				.Builder("rewardsProgram")
				.setValidator((rewards) -> {
					assertField(rewards,
						new BooleanField
							.Builder("hasRewardProgram")
							.build());

					assertField(rewards,
						new StringField
							.Builder("rewardProgramInfo")
							.setMaxLength(2000)
							.setPattern("[\\w\\W\\s]*")
							.setOptional()
							.build());
				}).build());

		assertField(businessCreditCards,
			new ObjectField.Builder("fees").setValidator(
				fees -> assertField(fees,
					new ObjectArrayField.Builder("services")
						.setMinItems(1).setMaxItems(9).setValidator(
						services -> {
							assertField(services, Fields.name().setEnums(names).build());
							assertField(services, Fields.code().setEnums(codes).build());
							assertField(services, Fields.chargingTriggerInfo().build());
							parts.assertPrices(services);
							parts.applyAssertingForCommonMinimumAndMaximum(services);
						}).build())
			).build());

		assertField(businessCreditCards, new ObjectField.Builder("interest").

			setValidator(interest -> {
				parts.applyAssertingForCommonRates(interest, "rates", false);
				parts.applyAssertingForCommonRates(interest, "instalmentRates", false);
				assertField(interest,
					new ObjectArrayField
						.Builder("otherCredits")
						.setMinItems(1)
						.setMaxItems(3)
						.setValidator(otherCredits -> {
							Set<String> types = Sets.newHashSet("SAQUE_A_CREDITO", "PAGAMENTOS_CONTAS", "OUTROS");
							assertField(otherCredits, Fields.code().setEnums(types).build());
							assertField(otherCredits,
								new StringField
									.Builder("additionalInfo")
									.setMaxLength(50)
									.setPattern("[\\w\\W\\s]*")
									.build());
						}).build());
			}).build());

		assertField(businessCreditCards,
			new ObjectField
				.Builder("termsConditions")
				.setValidator(this::assertTermsConditions)
				.build());
	}

	private void assertTermsConditions(JsonObject termsConditions) {
		assertField(termsConditions,
			new StringField
				.Builder("minimumFeeRate")
				.setMaxLength(4)
				.setPattern("(^[0-9](\\.[0-9]{2})$|^NA$)")
				.build());

		assertField(termsConditions,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(500)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(termsConditions,
			new StringField
				.Builder("elegibilityCriteriaInfo")
				.setMaxLength(2000)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(termsConditions,
			new StringField
				.Builder("closingProcessInfo")
				.setMaxLength(2000)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}

	private void assertInnerIdentification(JsonObject innerIdentification) {
		assertField(innerIdentification,
			new ObjectField
				.Builder("product")
				.setValidator(this::assertInterestProduct)
				.build());

		assertField(innerIdentification,
			new ObjectField
				.Builder("creditCard")
				.setValidator(this::assertInterestCreditCard)
				.build());
	}

	private void assertInterestCreditCard(JsonObject creditCard) {
		Set<String> types = Sets.newHashSet("VISA", "MASTERCARD", "AMERICAN_EXPRESS",
			"DINERS_CLUB", "HIPERCARD", "BANDEIRA_PROPRIA", "CHEQUE_ELETRONICO", "ELO", "OUTRAS");

		assertField(creditCard,
			new StringField
				.Builder("network")
				.setEnums(types)
				.build());

		assertField(creditCard,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}

	private void assertInterestProduct(JsonObject product) {
		Set<String> types = Sets.newHashSet("CLASSIC_NACIONAL", "CLASSIC_INTERNACIONAL",
			"GOLD", "PLATINUM", "INFINITE", "ELECTRON", "STANDARD_NACIONAL", "STANDARD_INTERNACIONAL",
			"ELETRONIC", "BLACK", "REDESHOP", "MAESTRO_MASTERCARD_MAESTRO", "GREEN", "BLUE", "BLUEBOX",
			"PROFISSIONAL_LIBERAL", "CHEQUE_ELETRONICO", "CORPORATIVO", "EMPRESARIAL", "COMPRAS", "OUTROS");

		assertField(product, Fields.type(types).build());

		assertField(product,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}
}
