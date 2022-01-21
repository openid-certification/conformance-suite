package net.openid.conformance.openbanking_brasil.productsNServices.creditCard;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.CommonFields;
import net.openid.conformance.openbanking_brasil.productsNServices.CommonValidatorParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/91e2ff8327cb35eb1ae571c7b2264e6173b34eeb/swagger/swagger_products_services_apis.yaml
 * Api endpoint: /personal-credit-cards
 * Api version: 1.0.2
 * Api git hash: ba747ce30bdf7208a246ebf1e8a2313f85263d91
 *
 */
@ApiName("ProductsNServices PersonalCreditCard")
public class PersonalCreditCardValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> TYPES = Sets.newHashSet("SAQUE_A_CREDITO", "PAGAMENTOS_CONTAS", "OUTROS");
	public static final Set<String> CARD_TYPES = Sets.newHashSet("VISA", "MASTERCARD", "AMERICAN_EXPRESS",
		"DINERS_CLUB", "HIPERCARD", "BANDEIRA_PROPRIA", "CHEQUE_ELETRONICO", "ELO", "OUTRAS");
	public static final Set<String> CARD_PRODUCT_TYPES = Sets.newHashSet("CLASSIC_NACIONAL", "CLASSIC_INTERNACIONAL",
		"GOLD", "PLATINUM", "INFINITE", "ELECTRON", "STANDARD_NACIONAL", "STANDARD_INTERNACIONAL",
		"ELETRONIC", "BLACK", "REDESHOP", "MAESTRO_MASTERCARD_MAESTRO", "GREEN", "BLUE", "BLUEBOX",
		"PROFISSIONAL_LIBERAL", "CHEQUE_ELETRONICO", "CORPORATIVO", "EMPRESARIAL", "COMPRAS", "OUTROS");
	public static final Set<String> NAMES = Sets.newHashSet("ANUIDADE_CARTAO_BASICO_NACIONAL",
		"ANUIDADE_CARTAO_BASICO_INTERNACIONAL", "ANUIDADE_DIFERENCIADA",
		"UTILIZACAO_CANAIS_ATENDIMENTO_RETIRADA_ESPECIE_BRASIL",
		"UTILIZACAO_CANAIS_ATENDIMENTO_RETIRADA_ESPECIE_EXTERIOR",
		"AVALIACAO_EMERGENCIAL_CREDITO", "FORNECIMENTO_SEGUNDA_VIA_FUNCAO_CREDITO",
		"PAGAMENTO_CONTAS_UTILIZANDO_FUNCAO_CREDITO", "SMS");
	public static final Set<String> CODES = Sets.newHashSet("ANUIDADE_NACIONAL", "ANUIDADE_INTERNACIONAL",
		"ANUIDADE_DIFERENCIADA", "SAQUE_CARTAO_BRASIL", "SAQUE_CARTAO_EXTERIOR",
		"AVALIACAO_EMERGENCIAL_CREDITO", "EMISSAO_SEGUNDA_VIA", "TARIFA_PAGAMENTO_CONTAS", "SMS");
	private final CommonValidatorParts parts;

	public PersonalCreditCardValidator() {
		parts = new CommonValidatorParts(this);
	}

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		setLogOnlyFailure();
		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertField(body, new ObjectField.Builder(ROOT_PATH).setValidator(
			data -> assertField(data, new ObjectField.Builder("brand").setValidator(
				brand -> {
					assertField(brand, CommonFields.name().build());
					assertField(brand,
						new ObjectArrayField.Builder("companies")
							.setMinItems(1)
							.setValidator(this::assertCompanies)
							.build());}
			).build())
		).build());
		logFinalStatus();
		return environment;
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, CommonFields.cnpjNumber().build());
		assertField(companies, CommonFields.name().build());
		assertField(companies, CommonFields.urlComplementaryList().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("personalCreditCards")
				.setValidator(this::assertPersonalCreditCards)
				.setMinItems(1)
				.build());
	}

	private void assertPersonalCreditCards(JsonObject personalCreditCards) {
		assertField(personalCreditCards,
			CommonFields.name().setMaxLength(50).build());

		assertField(personalCreditCards,
			new ObjectField
				.Builder("identification")
				.setValidator(this::assertInnerIdentification)
				.build());

		assertField(personalCreditCards,
			new ObjectField
				.Builder("rewardsProgram")
				.setValidator(this::assertRewardsProgram)
				.build());

		assertField(personalCreditCards,
			new ObjectField
				.Builder("fees")
				.setValidator(this::assertInnerFees)
				.build());

		assertField(personalCreditCards,
			new ObjectField
				.Builder("interest")
				.setValidator(this::assertInterest)
				.build());

		assertField(personalCreditCards,
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

	private void assertInterest(JsonObject interest) {
		parts.applyAssertingForCommonRates(interest, "rates", false);
		parts.applyAssertingForCommonRates(interest, "instalmentRates", false);

		assertField(interest,
			new ObjectArrayField
				.Builder("otherCredits")
				.setValidator(this::assertOtherCredits)
				.setMinItems(1)
				.setMaxItems(3)
				.build());
	}

	private void assertOtherCredits(JsonObject otherCredits) {

		assertField(otherCredits,
			new StringField
				.Builder("code")
				.setEnums(TYPES)
				.build());

		assertField(otherCredits,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}

	private void assertInnerFees(JsonObject innerFees) {
		assertField(innerFees,
			new ObjectArrayField
				.Builder("services")
				.setValidator(this::assertServices)
				.setMinItems(1)
				.setMaxItems(9)
				.build());
	}

	private void assertRewardsProgram(JsonObject rewardsProgram) {
		assertField(rewardsProgram,
			new BooleanField
				.Builder("hasRewardProgram")
				.build());

		assertField(rewardsProgram,
			new StringField
				.Builder("rewardProgramInfo")
				.setMaxLength(2000)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
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

		assertField(creditCard,
			new StringField
				.Builder("network")
				.setEnums(CARD_TYPES)
				.build());

		assertField(creditCard,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}

	private void assertInterestProduct(JsonObject product) {

		assertField(product, CommonFields.type(CARD_PRODUCT_TYPES).build());

		assertField(product,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}

	private void assertServices(JsonObject innerServices) {
		assertField(innerServices,
			new StringField
				.Builder("name")
				.setEnums(NAMES)
				.build());

		assertField(innerServices,
			new StringField
				.Builder("code")
				.setEnums(CODES)
				.build());

		assertField(innerServices, CommonFields.chargingTriggerInfo().build());

		parts.assertPrices(innerServices);
		parts.applyAssertingForCommonMinimumAndMaximum(innerServices);
	}
}
