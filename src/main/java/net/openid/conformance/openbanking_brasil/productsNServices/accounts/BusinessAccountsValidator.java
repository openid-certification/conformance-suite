package net.openid.conformance.openbanking_brasil.productsNServices.accounts;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.CommonFields;
import net.openid.conformance.openbanking_brasil.productsNServices.CommonValidatorParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/91e2ff8327cb35eb1ae571c7b2264e6173b34eeb/swagger/swagger_products_services_apis.yaml
 * Api endpoint: /business-accounts
 * Api version: 1.0.2
 * Api git hash: 1ecdb0cc1e9dbe85f3dd1df8b870f2a4b927837d
 *
 */

@ApiName("ProductsNServices Business Accounts")
public class BusinessAccountsValidator extends AbstractJsonAssertingCondition {
	private static class Fields extends CommonFields {}
	private final CommonValidatorParts parts;

	public BusinessAccountsValidator() {
		parts = new CommonValidatorParts(this);
	}

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		setLogOnlyFailure();
		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertJsonObject(body, ROOT_PATH,
			data -> assertField(data, new ObjectField.Builder("brand").setValidator(
				brand -> {
					assertField(brand, Fields.name().build());
					assertField(brand,
						new ObjectArrayField.Builder("companies")
							.setMinItems(1)
							.setValidator(this::assertCompanies)
							.build());
				}
			).build())
		);
		logFinalStatus();
		return environment;
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, Fields.cnpjNumber().build());
		assertField(companies, Fields.name().build());
		assertField(companies, Fields.urlComplementaryList().setOptional().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("businessAccounts")
				.setValidator(this::assertBusinessAccounts)
				.setMinItems(1)
				.setMaxItems(3)
				.setOptional()
				.build());
	}

	private void assertBusinessAccounts(JsonObject businessAccounts) {
		Set<String> accountTypes = Sets.newHashSet("CONTA_DEPOSITO_A_VISTA", "CONTA_POUPANCA", "CONTA_PAGAMENTO_PRE_PAGA");
		Set<String> OpeningClosingChannels = Sets.newHashSet("DEPENDENCIAS_PROPRIAS", "CORRESPONDENTES_BANCARIOS", "INTERNET_BANKING", "MOBILE_BANKING", "CENTRAL_TELEFONICA", "CHAT", "OUTROS");
		Set<String> transactionMethods = Sets.newHashSet("MOVIMENTACAO_ELETRONICA", "MOVIMENTACAO_CHEQUE", "MOVIMENTACAO_CARTAO", "MOVIMENTACAO_PRESENCIAL");

		assertField(businessAccounts, Fields.type(accountTypes).build());

		assertField(businessAccounts,
			new ObjectField.Builder("fees").setValidator(
				fees -> assertField(fees,
					new ObjectArrayField.Builder("services").setMinItems(1).setValidator(
						services -> {
							assertField(services, Fields.name().setMaxLength(250).build());
							assertField(services, Fields.code().build());
							assertField(services, Fields.chargingTriggerInfo().build());
							parts.assertPrices(services);
							parts.applyAssertingForCommonMinimumAndMaximum(services);
						}).build())
				).build());

		assertField(businessAccounts,
			new ObjectArrayField
				.Builder("serviceBundles")
				.setValidator(this::assertServiceBundles)
				.setMinItems(1)
				.build());

		assertField(businessAccounts,
			new StringArrayField
				.Builder("openingClosingChannels")
				.setEnums(OpeningClosingChannels)
				.setMinItems(1)
				.setMaxItems(7)
				.build());

		assertField(businessAccounts,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(100)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(businessAccounts,
			new StringArrayField
				.Builder("transactionMethods")
				.setEnums(transactionMethods)
				.setMinItems(1)
				.setMaxItems(4)
				.build());

		assertField(businessAccounts,
			new ObjectField
				.Builder("termsConditions")
				.setValidator(this::assertTermsConditions)
				.build());

		assertField(businessAccounts,
			new ObjectField
				.Builder("incomeRate")
				.setValidator(this::assertIncomeRate)
				.build());
	}

	private void assertTermsConditions(JsonObject termsConditions) {
		assertField(termsConditions,
			new ObjectField
				.Builder("minimumBalance")
				.setValidator(this::assertMinimumBalance)
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

	private void assertIncomeRate(JsonObject incomeRate) {
		assertField(incomeRate,
			new StringField
				.Builder("savingAccount")
				.setMaxLength(2000)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(incomeRate,
			new StringField
				.Builder("prepaidPaymentAccount")
				.setMaxLength(2000)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());
	}

	private void assertMinimumBalance(JsonObject minimumBalance) {
		assertField(minimumBalance, Fields.value().build());
		assertField(minimumBalance, Fields.currency().build());
	}

	private void assertServiceBundles(JsonObject servicesBundles) {
		assertField(servicesBundles, Fields.name().setMaxLength(250).build());

		assertField(servicesBundles,
			new ObjectArrayField
				.Builder("services")
				.setValidator(this::assertInnerServices)
				.setMinItems(1)
				.build());

		parts.assertMonthlyPrices(servicesBundles);
		parts.applyAssertingForCommonMinimumAndMaximum(servicesBundles);
	}

	private void assertInnerServices(JsonObject services) {
		assertField(services,
			new StringField
				.Builder("code")
				.setMaxLength(100)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(services, Fields.chargingTriggerInfo().build());

		assertField(services,
			new StringField
				.Builder("eventLimitQuantity")
				.setMaxLength(6)
				.setPattern("^(\\d{1,6}){1}$")
				.build());

		assertField(services,
			new StringField
				.Builder("freeEventQuantity")
				.setMaxLength(6)
				.setPattern("^(\\d{1,6}){1}$")
				.build());
	}

}
