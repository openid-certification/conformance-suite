package net.openid.conformance.openbanking_brasil.productsNServices.unarrangedAccountOverdraft;

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
import net.openid.conformance.util.field.StringField;

/**
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/91e2ff8327cb35eb1ae571c7b2264e6173b34eeb/swagger/swagger_products_services_apis.yaml
 * Api endpoint: /business-unarranged-account-overdraft
 * Api version: 1.0.2
 * Api git hash: 1ecdb0cc1e9dbe85f3dd1df8b870f2a4b927837d
 *
 */
@ApiName("ProductsNServices Unarranged Account Business Overdraft")
public class UnarrangedAccountBusinessOverdraftValidator extends AbstractJsonAssertingCondition {

	private static class Fields extends CommonFields {}
	private final CommonValidatorParts parts;

	public UnarrangedAccountBusinessOverdraftValidator() {
		parts = new CommonValidatorParts(this);
	}

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		setLogOnlyFailure();
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
		logFinalStatus();
		return environment;
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, Fields.cnpjNumber().build());
		assertField(companies, Fields.name().build());
		assertField(companies, Fields.urlComplementaryList().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("businessUnarrangedAccountOverdraft")
				.setValidator(this::assertBusinessUnarrangedAccountOverdraft)
				.build());
	}

	private void assertBusinessUnarrangedAccountOverdraft(JsonObject businessUnarrangedAccountOverdraft) {
		assertField(businessUnarrangedAccountOverdraft,
			new ObjectField
				.Builder("fees")
				.setValidator(this::assertInnerFees)
				.build());

		parts.applyAssertingForCommonRates(businessUnarrangedAccountOverdraft,
				"interestRates", true);

		assertField(businessUnarrangedAccountOverdraft,
			new StringField
				.Builder("termsConditions")
				.setMaxLength(2000)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}

	private void assertInnerFees(JsonObject innerFees) {
		assertField(innerFees,
			new ObjectArrayField
				.Builder("services")
				.setValidator(this::assertServices)
				.setMinItems(1)
				.setMaxItems(31)
				.build());
	}

	private void assertServices(JsonObject innerServices) {
		assertField(innerServices,
			new StringField
				.Builder("name")
				.setEnums(Sets.newHashSet("CONCESSAO_ADIANTAMENTO_DEPOSITANTE"))
				.build());

		assertField(innerServices,
			new StringField
				.Builder("code")
				.setEnums(Sets.newHashSet("ADIANT_DEPOSITANTE"))
				.build());

		assertField(innerServices, Fields.chargingTriggerInfo().build());

		parts.assertPrices(innerServices);
		parts.applyAssertingForCommonMinimumAndMaximum(innerServices);
	}
}
