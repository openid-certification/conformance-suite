package net.openid.conformance.openinsurance.validator.productsServices;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.openinsurance.validator.OpenInsuranceLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api Source: swagger/openinsurance/productsServices/swagger-housing.yaml
 * Api endpoint: /housing/
 * Api version: 1.0.0
 */

@ApiName("ProductsServices Housing")
public class GetHousingValidator extends AbstractJsonAssertingCondition {

	private static class Fields extends ProductNServicesCommonFields {}

	public static final Set<String> COVERAGE = SetUtils.createSet("DANOS_ELETRICOS, DANOS_FISICOS_AO_CONTEUDO, DANOS_FISICOS_AO_IMOVEL, " +
		"MORTE_OU_INVALIDEZ_TOTAL_PERMANENTE, PAGAMENTO_DE_ALUGUEL, RESPONSABILIDADE_CIVIL_DO_CONSTRUTOR_RCC, ROUBO_E_FURTO_AO_CONTEUDO, OUTRAS");
	public static final Set<String> INSURED_PARTICIPATION = SetUtils.createSet("FRANQUIA, POS, NAO_SE_APLICA");
	public static final Set<String> ADDITIONAL = SetUtils.createSet("SORTEIO_GRATUITO, CLUBE_DE_BENEFICIOS, CASHBACK, DESCONTOS, OUTROS");
	public static final Set<String> TARGET_AUDIENCE = SetUtils.createSet("PESSOA_FISICA, PESSOA_JURIDICA");

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
		assertField(companies, Fields.cnpjNumber().build());

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
			new StringArrayField
				.Builder("additional")
				.setMaxLength(19)
				.setEnums(ADDITIONAL)
				.build());

		assertField(products,
			new StringField
				.Builder("additionalOthers")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(products,
			new StringField
				.Builder("premiumRates")
				.setMaxLength(1024)
				.build());

		assertField(products,
			new ObjectField
				.Builder("termsAndConditions")
				.setValidator(assertTermsAndConditions -> {
					assertField(assertTermsAndConditions,
						new StringField
							.Builder("susepProcessNumber")
							.setMaxLength(20)
							.build());

					assertField(assertTermsAndConditions,
						new StringField
							.Builder("definition")
							.setMaxLength(1024)
							.build());
				})
				.build());

		assertField(products,
			new ObjectField.Builder("minimumRequirements")
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
						.setMaxLength(30)
						.build());
				}).build());
	}

	private void assertCoverages(JsonObject coverages) {
		assertField(coverages,
			new StringField
				.Builder("coverage")
				.setMaxLength(40)
				.setEnums(COVERAGE)
				.build());

		assertField(coverages,
			new StringField
				.Builder("coverageDescription")
				.setMaxLength(3000)
				.build());

		assertField(coverages,
			new BooleanField
				.Builder("allowApartPurchase")
				.build());

		assertField(coverages,
			new ObjectField
				.Builder("coverageAttributes")
				.setValidator(this::assertCoverageAttributes)
				.build());
	}

	private void assertCoverageAttributes(JsonObject coverageAttributes) {
		assertField(coverageAttributes,
			new ObjectField
				.Builder("maxLMI")
				.setValidator(assertValue -> {
					assertField(assertValue,
						new NumberField
							.Builder("amount")
							.build());

					assertField(assertValue,
						new ObjectField
							.Builder("unit")
							.setValidator(this::assertUnit)
							.build());
				})
				.build());

		assertField(coverageAttributes,
			new StringArrayField
				.Builder("insuredParticipation")
				.setMaxLength(13)
				.setEnums(INSURED_PARTICIPATION)
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("insuredParticipationDescription")
				.setMaxLength(1024)
				.build());
	}

	private void assertUnit(JsonObject unit) {
		assertField(unit,
			new StringField
				.Builder("code")
				.setMaxLength(2)
				.build());

		assertField(unit,
			new StringField
				.Builder("description")
				.setMaxLength(5)
				.build());
	}
}
