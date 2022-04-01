package net.openid.conformance.openinsurance.validator.productsServices;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.openinsurance.validator.OpenInsuranceLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api Source: swagger/openinsurance/productsServices/swagger-export-credit.yaml
 * Api endpoint: /export-credit/
 * Api version: 1.0.0
 */

@ApiName("ProductsServices Export Credit")
public class GetExportCreditValidator extends AbstractJsonAssertingCondition {
	private static class Fields extends ProductNServicesCommonFields {
	}

	public static final Set<String> COVERAGE = Sets.newHashSet("NAO_PAGAMENTO_DA_CARTEIRA_DE_CLIENTES_DO_SEGURADO_INADIMPLENCIA_POR_QUESTAO_COMERCIAL", "NAO_PAGAMENTO_DA_CARTEIRA_DE_CLIENTES_DO_SEGURADO_INADIMPLENCIA_POR_QUESTAO_POLITICA_EXTRAORDINARIO", "OUTRAS");
	public static final Set<String> TERM = Sets.newHashSet("ANUAL", "ANUAL_INTERMITENTE", "PLURIANUAL", "PLURIANUAL_INTERMITENTE", "MENSAL", "MENSAL_INTERMITENTE", "DIARIO", "DIARIO_INTERMITENTE", "OUTROS");
	public static final Set<String> TARGET_AUDIENCE = Sets.newHashSet("PESSOA_NATURAL", "PESSOA_JURIDICA");

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body, new ObjectField
			.Builder("data")
			.setValidator(data -> assertField(data, new ObjectField
				.Builder("brand")
				.setValidator(brand -> {
					assertField(brand, Fields.name().setMaxLength(80).build());
					assertField(brand,
						new ObjectArrayField
							.Builder("companies")
							.setValidator(this::assertCompanies)
							.build());
				})
				.build())).build());
		new OpenInsuranceLinksAndMetaValidator(this).assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, Fields.name().setMaxLength(80).build());
		assertField(companies, Fields.cnpjNumber().setMaxLength(14).build());

		assertField(companies,
			new ObjectArrayField
				.Builder("products")
				.setValidator(this::assertProducts)
				.build());
	}

	private void assertProducts(JsonObject products) {
		assertField(products, Fields.name().setMaxLength(80).build());
		assertField(products, Fields.code().setMaxLength(80).build());

		assertField(products,
			new ObjectArrayField
				.Builder("coverages")
				.setValidator(this::assertCoverages)
				.build());

		assertField(products,
			new BooleanField
				.Builder("allowApartPurchase")
				.build());

		assertField(products,
			new BooleanField
				.Builder("traits")
				.build());

		assertField(products,
			new ObjectField.Builder("validity")
				.setValidator(validity -> {
					assertField(validity,
						new StringArrayField
							.Builder("term")
							.setEnums(TERM)
							.setMaxLength(23)
							.build());

					assertField(validity,
						new StringField
							.Builder("termOthers")
							.setMaxLength(100)
							.setOptional()
							.build());
				}).build());

		assertField(products,
			new StringArrayField
				.Builder("premiumRates")
				.setMaxLength(1024)
				.setOptional()
				.build());

		assertField(products,
			new ObjectField.Builder("termsAndConditions")
				.setValidator(termsAndConditions -> {
					assertField(termsAndConditions,
						new StringField
							.Builder("susepProcessNumber")
							.setOptional()
							.setMaxLength(20)
							.build());

					assertField(termsAndConditions,
						new StringField
							.Builder("definition")
							.setMaxLength(1024)
							.build());
				}).build());

		assertField(products,
			new ObjectField.Builder("minimumRequirements")
				.setValidator(minimumRequirements -> assertField(minimumRequirements,
					new StringArrayField
						.Builder("targetAudiences")
						.setEnums(TARGET_AUDIENCE)
						.setMaxLength(15)
						.build())).build());
	}

	private void assertCoverages(JsonObject coverages) {
		assertField(coverages,
			new StringField
				.Builder("coverage")
				.setEnums(COVERAGE)
				.setMaxLength(99)
				.build());

		assertField(coverages,
			new StringField
				.Builder("coverageDescription")
				.setMaxLength(3000)
				.build());

		assertField(coverages,
			new ObjectField
				.Builder("coverageAttributes")
				.setValidator(this::assertCoverageAttributes)
				.setOptional()
				.build());
	}

	private void assertCoverageAttributes(JsonObject coverageAttributes) {
		assertField(coverageAttributes,
			new ObjectField
				.Builder("maxLMI")
				.setValidator(this::assertValue)
				.build());
	}

	public void assertValue(JsonObject minValue) {
		assertField(minValue,
			new NumberField
				.Builder("amount")
				.build());

		assertField(minValue,
			new ObjectField
				.Builder("unit")
				.setValidator(this::assertUnit)
				.build());
	}

	public void assertUnit(JsonObject unit) {
		assertField(unit,
			new StringField
				.Builder("code")
				.build());

		assertField(unit,
			new StringField
				.Builder("description")
				.build());
	}
}
