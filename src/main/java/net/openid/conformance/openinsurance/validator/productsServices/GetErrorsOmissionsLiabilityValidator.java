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
 * Api Source: swagger/openinsurance/productsServices/swagger-errors-omissions-liability.yaml
 * Api endpoint: /errors-omissions-liability/
 * Api version: 1.0.0
 * Git hash: 1f1b2984856259fb6f0097e7ddd94aae5fc089f3
 */

@ApiName("ProductsServices Errors Omissions Liability")
public class GetErrorsOmissionsLiabilityValidator extends AbstractJsonAssertingCondition {
	private static class Fields extends ProductNServicesCommonFields {
	}

	public static final Set<String> COVERAGE = Sets.newHashSet("RESPONSABILIZACAO_CIVIL_VINCULADA_A_PRESTACAO_DE_SERVICOS_PROFISSIONAIS_OBJETO_DA_ATIVIDADE_DO_SEGURADO", "OUTRAS");
	public static final Set<String> PARTICIPATION = Sets.newHashSet("FRANQUIA", "POS", "NAO_SE_APLICA");
	public static final Set<String> TERM = Sets.newHashSet("ANUAL", "ANUAL_INTERMITENTE", "PLURIANUAL", "PLURIANUAL_INTERMITENTE", "MENSAL", "MENSAL_INTERMITENTE", "DIARIO", "DIARIO_INTERMITENTE", "OUTROS");
	public static final Set<String> PAYMENT_METHOD = Sets.newHashSet("CARTAO_DE_CREDITO", "CARTAO_DE_DEBITO", "DEBITO_EM_CONTA_CORRENTE", "DEBITO_EM_CONTA_POUPANCA", "BOLETO_BANCARIO", "PIX", "CONSIGNACAO_EM_FOLHA_DE_PAGAMENTO", "PONTOS_DE_PROGRAMA_DE_BENEFICIO", "OUTROS");
	public static final Set<String> PAYMENT_TYPE = Sets.newHashSet("A_VISTA", "PARCELADO");
	public static final Set<String> CONTRACT_TYPE = Sets.newHashSet("COLETIVO", "INDIVIDUAL");
	public static final Set<String> TARGET_AUDIENCE = Sets.newHashSet("PESSOA_NATURAL", "PESSOA_JURIDICA");
	public static final Set<String> TYPE_SIGNALING = Sets.newHashSet("GRATUITO", "PAGO");
	public static final Set<String> CUSTOMER_SERVICES = Sets.newHashSet("REDE_REFERENCIADA", "LIVRE_ESCOLHA");
	public static final Set<String> PROFESSIONAL_CLASS = Sets.newHashSet("ADMINISTRADOR_IMOBILIARIO", "ESCRITORIOS_DE_ADVOCACIA", "CERTIFICACAO_DIGITAL", "CERTIFICACAO_DE_PRODUTOS_SISTEMAS_PROCESSOS_OU_SERVICOS", "DESPACHANTE_ADUANEIRO_AGENTE_EMBARCADOR_LICENCIADOR_E_SIMILARES", "CORRETORES_DE_RESSEGURO", "CORRETORES_DE_SEGUROS", "EMPRESAS_DE_TECNOLOGIA", "EMPRESAS_DE_ENGENHARIA_E_ARQUITETURA", "HOSPITAIS_CLINICAS_MEDICAS_ODONTOLOGICAS_LABORATORIOS_E_EMPRESAS_DE_DIAGNOSTICOS", "NOTARIOS_E_OU_REGISTRADORES", "INSTITUICOES_FINANCEIRAS", "HOSPITAIS_CLINICAS_LABORATORIOS_EMPRESAS_DE_DIAGNOSTICOS_VETERINARIOS", "MEDICOS_VETERINARIOS", "OUTROS");
	public static final Set<String> IDENIZATION_BASIS = Sets.newHashSet("POR_OCORRENCIA", "POR_RECLAMACAO", "OUTRAS");
	public static final Set<String> ASSISTANCE_SERVICES_PACKAGE = Sets.newHashSet("ATE_10_SERVICOS", "ATE_20_SERVICOS", "ACIMA_20_SERVICOS", "CUSTOMIZAVEL");

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
		assertField(products, Fields.code().setMaxLength(100).build());

		assertField(products,
			new StringField
				.Builder("professionalClass")
				.setMaxLength(80)
				.setEnums(PROFESSIONAL_CLASS)
				.build());

		assertField(products,
			new StringField
				.Builder("professionalClassOthers")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("coverages")
				.setValidator(this::assertCoverages)
				.build());

		assertField(products,
			new BooleanField
				.Builder("traits")
				.build());

		assertField(products,
			new NumberField
				.Builder("maxLMG")
				.setMaxLength(36)
				.build());

		assertField(products,
			new StringField
				.Builder("maxLMGDescription")
				.setMaxLength(1024)
				.build());

		assertField(products,
			new StringArrayField
				.Builder("customerServices")
				.setEnums(CUSTOMER_SERVICES)
				.setMaxLength(17)
				.setOptional()
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("assistanceServices")
				.setValidator(this::assertAssistanceServices)
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("premiumPayment")
				.setValidator(this::assertPremiumPayment)
				.build());

		assertField(products,
			new ObjectArrayField.Builder("validity")
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
			new ObjectField.Builder("termsAndConditions")
				.setValidator(termsAndConditions -> {
					assertField(termsAndConditions,
						new StringField
							.Builder("susepProcessNumber")
							.setMaxLength(20)
							.setOptional()
							.build());

					assertField(termsAndConditions,
						new StringField
							.Builder("definition")
							.setMaxLength(1024)
							.build());
				}).build());

		assertField(products,
			new ObjectField.Builder("minimumRequirements")
				.setValidator(minimumRequirements -> {
					assertField(minimumRequirements,
						new StringArrayField
							.Builder("contractType")
							.setEnums(CONTRACT_TYPE)
							.setMaxLength(10)
							.build());

					assertField(minimumRequirements,
						new StringField
							.Builder("minimumRequirementDetails")
							.setMaxLength(1024)
							.build());

					assertField(minimumRequirements,
						new StringArrayField
							.Builder("targetAudiences")
							.setEnums(TARGET_AUDIENCE)
							.setMaxLength(15)
							.build());
				}).build());
	}

	private void assertAssistanceServices(JsonObject assistanceServices) {
		assertField(assistanceServices,
			new BooleanField
				.Builder("assistanceServices")
				.build());

		assertField(assistanceServices,
			new StringArrayField
				.Builder("assistanceServicesPackage")
				.setEnums(ASSISTANCE_SERVICES_PACKAGE)
				.setMaxLength(17)
				.setOptional()
				.build());

		assertField(assistanceServices,
			new StringField
				.Builder("complementaryAssistanceServicesDetail")
				.setMaxLength(1000)
				.setOptional()
				.build());

		assertField(assistanceServices,
			new StringField
				.Builder("chargeTypeSignaling")
				.setMaxLength(8)
				.setEnums(TYPE_SIGNALING)
				.setOptional()
				.build());
	}

	private void assertPremiumPayment(JsonObject premiumPayment) {
		assertField(premiumPayment,
			new StringField
				.Builder("paymentMethod")
				.setEnums(PAYMENT_METHOD)
				.setMaxLength(33)
				.build());

		assertField(premiumPayment,
			new StringField
				.Builder("paymentDetail")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(premiumPayment,
			new StringArrayField
				.Builder("paymentType")
				.setEnums(PAYMENT_TYPE)
				.setMaxLength(9)
				.build());

		assertField(premiumPayment,
			new StringField
				.Builder("premiumRates")
				.setMaxLength(1024)
				.setOptional()
				.build());
	}

	private void assertCoverages(JsonObject coverages) {
		assertField(coverages,
			new StringField
				.Builder("coverage")
				.setEnums(COVERAGE)
				.setMaxLength(103)
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

		assertField(coverages,
			new BooleanField
				.Builder("allowApartPurchase")
				.build());
	}

	private void assertCoverageAttributes(JsonObject coverageAttributes) {
		assertField(coverageAttributes,
			new ObjectField
				.Builder("maxLMI")
				.setValidator(this::assertValue)
				.build());

		assertField(coverageAttributes,
			new StringArrayField
				.Builder("insuredParticipation")
				.setEnums(PARTICIPATION)
				.setMaxLength(13)
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("insuredParticipationDescription")
				.setMaxLength(1024)
				.setOptional()
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("idenizationBasis")
				.setEnums(IDENIZATION_BASIS)
				.setMaxLength(14)
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("idenizationBasisOthers")
				.setMaxLength(3000)
				.setOptional()
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
				.setMaxLength(2)
				.build());

		assertField(unit,
			new StringField
				.Builder("description")
				.setMaxLength(5)
				.build());
	}
}
