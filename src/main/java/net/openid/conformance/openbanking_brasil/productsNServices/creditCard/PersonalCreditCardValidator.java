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
 * URL: /personal-credit-cards
 */

@ApiName("ProductsNServices PersonalCreditCard")
public class PersonalCreditCardValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body,
			new ObjectField
				.Builder(ROOT_PATH)
				.setValidator(this::assertInnerFields)
				.build());
		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		assertField(body,
			new ObjectField
				.Builder("brand")
				.setValidator(this::assertBrandFields)
				.build());
	}

	private void assertBrandFields(JsonObject brand) {
		assertField(brand, ProductsNServicesCommonFields.name().build());

		assertField(brand,
			new ObjectArrayField
				.Builder("companies")
				.setValidator(this::assertCompanies)
				.setMinItems(1)
				.build());
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, ProductsNServicesCommonFields.cnpjNumber().build());
		assertField(companies, ProductsNServicesCommonFields.name().build());
		assertField(companies, ProductsNServicesCommonFields.urlComplementaryList().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("personalCreditCards")
				.setValidator(this::assertPersonalCreditCards)
				.setMinItems(1)
				.build());
	}

	private void assertPersonalCreditCards(JsonObject personalCreditCards) {
		assertField(personalCreditCards,
			ProductsNServicesCommonFields.name().setMaxLength(50).build());

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
		new ProductsNServicesCommonValidatorParts(this).applyAssertingForCommonRates(interest,
			"rates", false);
		new ProductsNServicesCommonValidatorParts(this).applyAssertingForCommonRates(interest,
			"instalmentRates", false);

		assertField(interest,
			new ObjectArrayField
				.Builder("otherCredits")
				.setValidator(this::assertOtherCredits)
				.setMinItems(1)
				.setMaxItems(3)
				.build());
	}

	private void assertOtherCredits(JsonObject otherCredits) {
		Set<String> types = Sets.newHashSet("SAQUE_A_CREDITO", "PAGAMENTOS_CONTAS", "OUTROS");

		assertField(otherCredits,
			new StringField
				.Builder("code")
				.setEnums(types)
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

		assertField(product, ProductsNServicesCommonFields.type(types).build());

		assertField(product,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}

	private void assertServices(JsonObject innerServices) {
		Set<String> names = Sets.newHashSet("ANUIDADE_CARTAO_BASICO_NACIONAL",
			"ANUIDADE_CARTAO_BASICO_INTERNACIONAL", "ANUIDADE_DIFERENCIADA",
			"UTILIZACAO_CANAIS_ATENDIMENTO_RETIRADA_ESPECIE_BRASIL",
			"UTILIZACAO_CANAIS_ATENDIMENTO_RETIRADA_ESPECIE_EXTERIOR",
			"AVALIACAO_EMERGENCIAL_CREDITO", "FORNECIMENTO_SEGUNDA_VIA_FUNCAO_CREDITO",
			"PAGAMENTO_CONTAS_UTILIZANDO_FUNCAO_CREDITO", "SMS");
		Set<String> codes = Sets.newHashSet("ANUIDADE_NACIONAL", "ANUIDADE_INTERNACIONAL",
			"ANUIDADE_DIFERENCIADA", "SAQUE_CARTAO_BRASIL", "SAQUE_CARTAO_EXTERIOR",
			"AVALIACAO_EMERGENCIAL_CREDITO", "EMISSAO_SEGUNDA_VIA", "TARIFA_PAGAMENTO_CONTAS", "SMS");
		assertField(innerServices,
			new StringField
				.Builder("name")
				.setEnums(names)
				.build());

		assertField(innerServices,
			new StringField
				.Builder("code")
				.setEnums(codes)
				.build());

		assertField(innerServices, ProductsNServicesCommonFields.chargingTriggerInfo().build());

		new ProductsNServicesCommonValidatorParts(this).assertPrices(innerServices);
		new ProductsNServicesCommonValidatorParts(this).applyAssertingForCommonMinimumAndMaximum(innerServices);
	}
}
