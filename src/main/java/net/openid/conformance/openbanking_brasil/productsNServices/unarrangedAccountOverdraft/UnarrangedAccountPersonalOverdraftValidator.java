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
 *
 * URL: /personal-unarranged-account-overdraft
 */

@ApiName("ProductsNServices Unarranged Account Overdraft")
public class UnarrangedAccountPersonalOverdraftValidator extends AbstractJsonAssertingCondition {

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
				.build());
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, ProductsNServicesCommonFields.cnpjNumber().build());
		assertField(companies, ProductsNServicesCommonFields.name().build());
		assertField(companies, ProductsNServicesCommonFields.urlComplementaryList().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("personalUnarrangedAccountOverdraft")
				.setValidator(this::assertPersonalUnarrangedAccountOverdraft)
				.build());
	}

	private void assertPersonalUnarrangedAccountOverdraft(JsonObject personalUnarrangedAccountOverdraft) {
		assertField(personalUnarrangedAccountOverdraft,
			new ObjectField
				.Builder("fees")
				.setValidator(this::assertInnerFees)
				.build());

		new ProductsNServicesCommonValidatorParts(this)
			.applyAssertingForCommonRates(personalUnarrangedAccountOverdraft,
				"interestRates", true);

		assertField(personalUnarrangedAccountOverdraft,
			new StringField
				.Builder("termsConditions")
				.setMaxLength(2000)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}

	private void assertInnerFees(JsonObject innerFees) {
		assertField(innerFees,
			new ObjectArrayField
				.Builder("priorityServices")
				.setValidator(this::assertServices)
				.setMinItems(1)
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

		assertField(innerServices, ProductsNServicesCommonFields.chargingTriggerInfo().build());

		new ProductsNServicesCommonValidatorParts(this).assertPrices(innerServices);
		new ProductsNServicesCommonValidatorParts(this).applyAssertingForCommonMinimumAndMaximum(innerServices);
	}
}
