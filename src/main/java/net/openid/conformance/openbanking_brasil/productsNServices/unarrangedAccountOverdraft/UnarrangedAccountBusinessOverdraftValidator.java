package net.openid.conformance.openbanking_brasil.productsNServices.unarrangedAccountOverdraft;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductsNServicesCommonFields;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductsNServicesCommonValidatorParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * https://openbanking-brasil.github.io/areadesenvolvedor/swagger/swagger_products_services_apis.yaml
 *
 * URL: /business-unarranged-account-overdraft
 */

@ApiName("ProductsNServices Unarranged Account Business Overdraft")
public class UnarrangedAccountBusinessOverdraftValidator extends AbstractJsonAssertingCondition {

	private static class Fields extends ProductsNServicesCommonFields {}
	private final ProductsNServicesCommonValidatorParts parts;

	public UnarrangedAccountBusinessOverdraftValidator() {
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

		new ProductsNServicesCommonValidatorParts(this).assertPrices(innerServices);
		new ProductsNServicesCommonValidatorParts(this).applyAssertingForCommonMinimumAndMaximum(innerServices);
	}
}
