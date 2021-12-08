package net.openid.conformance.openinsurance.validator.productsNServices;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.CommonValidatorParts;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api endpoint: /auto-insurance/{commercializationArea}/{fipeCode}/{year}
 * Api version: 1.0.0
 */

@ApiName("ProductsNServices Auto Insurance")
public class GetAutoInsuranceValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> GEOGRAPHIC_SCOPE_COVERAGE = Sets.newHashSet("NACIONAL", "REGIONAL", "INTERNACIONAL", "OUTROS_PAISES");
	public static final Set<String> DEDUCTIBLE_TYPES = Sets.newHashSet("NORMAL", "REDUZIDA", "ISENTA", "MAJORADA", "FLEXIVEL");
	public static final Set<String> CONTRACT_BASE_TYPE = Sets.newHashSet("VALOR_DETERMINADO", "VALOR_MERCADO", "AMBOS");
	public static final Set<String> COVERAGE = Sets.newHashSet("CASCO_COMPREENSIVA_COLISAO_INCENDIO_ROUBO_FURTO", "CASCO_INCENDIO_ROUBO_FURTO", "CASCO_ROUBO_FURTO", "CASCO_INCENDIO", "CASCO_ALAGAMENTO", "CASCO_COLISAO_INDENIZACAO_PARCIAL", "CASCO_COLISAO_INDENIZACAO_INTEGRAL", "RESPONSABILIDADE_CIVIL_FACULTATIVA_VEICULOS_RCFV", "RESPONSABILIDADE_CIVIL_FACULTATIVA_CONDUTOR_RCFC", "ACIDENTE_PESSOAIS_PASSAGEIROS_VEICULO", "ACIDENTE_PESSOAIS_PASSAGEIROS_CONDUTOR", "VIDROS", "DIARIA_INDISPONIBILIDADE", "LFR_LANTERNAS_FAROIS_RETROVISORES", "ACESSORIOS_EQUIPAMENTOS", "CARRO_RESERVA", "PEQUENOS_REPAROS", "RESPONSABILIDADE_CIVIL_CARTA_VERDE", "OUTRAS");
	private final CommonValidatorParts parts;
	private static class Fields extends ProductNServicesCommonFields { }

	public GetAutoInsuranceValidator() {
		parts = new CommonValidatorParts(this);
	}

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body, new ObjectField
			.Builder("data")
			.setValidator(data -> {

				assertField(data,
					new DatetimeField
						.Builder("requestTime")
						.setPattern("[\\w\\W\\s]*")
						.setMaxLength(2048)
						.setOptional()
						.build());

				assertField(data, new ObjectField
					.Builder("brand")
					.setValidator(brand -> {
						assertField(brand, Fields.name().build());
						assertField(brand, new ObjectField
							.Builder("company")
							.setValidator(this::assertCompany)
							.build());
					})
					.setOptional().build());
			}).build());

		logFinalStatus();
		return environment;
	}

	private void assertCompany(JsonObject companies) {
		assertField(companies, Fields.name().build());
		assertField(companies, Fields.cnpjNumber().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("products")
				.setValidator(this::assertProducts)
				.setOptional()
				.build());
	}

	private void assertProducts(JsonObject products) {
		assertField(products, Fields.name().build());
		assertField(products, Fields.code().build());

		assertField(products,
			new ObjectArrayField
				.Builder("coverages")
				.setValidator(this::assertCoverages)
				.setOptional()
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("carParts")
				.setValidator(carParts -> {
					assertField(carParts,
						new StringField
							.Builder("carPartCondition")
							.build());

					assertField(carParts,
						new StringField
							.Builder("carPartType")
							.build());
				})
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("carModels")
				.setValidator(carModels -> {
					assertField(carModels,
						new StringField
							.Builder("manufacturer")
							.build());

					assertField(carModels,
						new StringField
							.Builder("model")
							.build());

					assertField(carModels,
						new IntField
							.Builder("year")
							.build());

					assertField(carModels,
						new StringField
							.Builder("fipeCode")
							.build());
				})
				.build());

		assertField(products,
			new StringField
				.Builder("vehicleOvernightZipCode")
				.build());

		assertField(products,
			new StringField
				.Builder("additional")
				.build());

		assertField(products,
			new StringField
				.Builder("additionalOthers")
				.build());

		assertField(products,
			new ObjectField
				.Builder("assistanceServices")
				.setValidator(assistanceServices -> {
					assertField(assistanceServices,
						new StringArrayField
							.Builder("assistanceServicesPackage")
							.build());

					assertField(assistanceServices,
						new StringField
							.Builder("assistanceServicesDetail")
							.build());

					assertField(assistanceServices,
						new StringField
							.Builder("chargeTypeSignaling")
							.build());
				})
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("termAndCondition")
				.setValidator(termAndCondition -> {
					assertField(termAndCondition,
						new StringField
							.Builder("susepProcessNumber")
							.build());

					assertField(termAndCondition,
						new StringField
							.Builder("definition")
							.build());
				})
				.build());

		assertField(products,
			new StringArrayField
				.Builder("terms")
				.build());

		assertField(products,
			new StringArrayField
				.Builder("customerService")
				.build());

		assertField(products,
			new ObjectField
				.Builder("premiumPayments")
				.setValidator(premiumPayments -> {
					assertField(premiumPayments,
						new StringArrayField
							.Builder("paymentMethod")
							.build());

					assertField(premiumPayments,
						new StringArrayField
							.Builder("paymentType")
							.build());

					assertField(premiumPayments,
						new StringField
							.Builder("paymentDetail")
							.build());
				})
				.build());

		assertField(products,
			new ObjectField
				.Builder("minimumRequirements")
				.setValidator(minimumRequirements -> {
					assertField(minimumRequirements,
						new StringArrayField
							.Builder("contractingType")
							.build());

					assertField(minimumRequirements,
						new StringField
							.Builder("contractingMinRequirement")
							.build());
				})
				.build());

		assertField(products,
			new StringArrayField
				.Builder("targetAudiences")
				.build());
	}

	private void assertCoverages(JsonObject coverages) {
		assertField(coverages,
			new StringField
				.Builder("coverage")
				.setEnums(COVERAGE)
				.build());

		assertField(coverages,
			new StringField
				.Builder("coverageDetail")
				.setMaxLength(1000)
				.build());

		assertField(coverages,
			new BooleanField
				.Builder("coveragePermissionSeparteAcquisition")
				.setOptional()
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
				.Builder("minLMI")
				.setValidator(parts::assertValue)
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("maxLMI")
				.setValidator(parts::assertValue)
				.build());

		assertField(coverageAttributes,
			new ObjectArrayField
				.Builder("contractBase")
				.setValidator(this::assertContractBase)
				.build());

		assertField(coverageAttributes,
			new IntField
				.Builder("newCarMaximumCalculatingPeriod")
				.setMaxLength(3)
				.build());

		assertField(coverageAttributes,
			new ObjectArrayField
				.Builder("newCardContractBase")
				.setValidator(this::assertContractBase)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("fullIndemnityPercentage")
				.setValidator(parts::assertValue)
				.build());

		assertField(coverageAttributes,
			new StringArrayField
				.Builder("deductibleType")
				.setEnums(DEDUCTIBLE_TYPES)
				.build());

		assertField(coverageAttributes,
			new BooleanField
				.Builder("fullIndemnityDeductible")
				.build());

		assertField(coverageAttributes,
			new BooleanField
				.Builder("deductibleExists")
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new BooleanField
				.Builder("deductiblePaymentByCoverage")
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new ObjectField
				.Builder("deductiblePercentage")
				.setValidator(parts::assertValue)
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("mandatoryParticipation")
				.setMaxLength(300)
				.build());

		assertField(coverageAttributes,
			new StringArrayField
				.Builder("geographicScopeCoverage")
				.setEnums(GEOGRAPHIC_SCOPE_COVERAGE)
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("geographicScopeCoverageOthers")
				.setOptional()
				.build());
		}

	private void assertContractBase(JsonObject contractBase) {
		assertField(contractBase,
			new StringField
				.Builder("contractBaseType")
				.setEnums(CONTRACT_BASE_TYPE)
				.build());

		assertField(contractBase,
			new ObjectField
				.Builder("contractBaseMinValue")
				.setValidator(parts::assertValue)
				.setOptional()
				.build());

		assertField(contractBase,
			new ObjectField
				.Builder("contractBaseMaxValue")
				.setValidator(parts::assertValue)
				.setOptional()
				.build());
		}
}
