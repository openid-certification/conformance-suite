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
 * Api Source: swagger/openinsurance/productsServices/swagger-transport.yaml
 * Api endpoint: /transport/
 * Git hash: c36cfcb7c62b3d011c7c8324ded1f75c806bc05d
 * Api version: 1.0.0
 */

@ApiName("ProductsServices Transport")
public class GetTransportValidator extends AbstractJsonAssertingCondition {

	private static class Fields extends ProductNServicesCommonFields {
	}

	public static final Set<String> TARGET_AUDIENCE = SetUtils.createSet("PESSOA_FISICA, PESSOA_JURIDICA");
	public static final Set<String> COVERAGE = SetUtils.createSet("ACIDENTES_PESSOAIS_COM_PASSAGEIROS, ACIDENTES_PESSOAIS_COM_TRIPULANTES," +
		" DANOS_CORPORAIS_PASSAGEIROS, DANOS_CORPORAIS_TERCEIROS, DANOS_CORPORAIS_TRIPULANTES, DANOS_ESTETICOS_PASSAGEIROS, DANOS_ESTETICOS_TERCEIROS," +
		" DANOS_ESTETICOS_TRIPULANTES, DANOS_MATERIAIS_PASSAGEIROS, DANOS_MATERIAIS_TERCEIROS, DANOS_MATERIAIS_TRIPULANTES, DANOS_MORAIS_PASSAGEIROS," +
		" DANOS_MORAIS_TERCEIROS, DANOS_MORAIS_TRIPULANTES, DESPESAS_COM_HONORARIOS," +
		" EMBARCADOR_AMPLA_A_RISCOS_DE_PERDA_OU_DANO_MATERIAL_SOFRIDOS_PELO_OBJETO_SEGURADO_EM_CONSEQUENCIA_DE_QUAISQUER_CAUSAS_EXTERNAS_EXCETO_AS_PREVISTAS_NA_CLAUSULA_DE_PREJUIZOS_NAO_INDENIZAVEIS," +
		" EMBARCADOR_RESTRITA_B_COBERTURAS_ELENCADAS_NA_EMBARCADOR_RESTRITA_C_E_INCLUI_INUNDACAO_TRANSBORDAMENTO_DE_CURSOS_DAGUA_REPRESAS_LAGOS_OU_LAGOAS_DURANTE_A_VIAGEM_TERRESTRE_DESMORONAMENTO_OU_QUEDA_DE_PEDRAS_TERRAS_OBRAS_DE_ARTE_DE_QUALQUER_NATUREZA_OU_OUTROS_OBJETOS_DURANTE_A_VIAGEM_TERRESTRE_TERREMOTO_OU_ERUPCAO_VULCANICA_E," +
		" ENTRADA_DE_AGUA_DO_MAR_LAGO_OU_RIO_NA_EMBARCACAO_OU_NO_NAVIO," +
		" VEICULO_CONTAINER_FURGAO_LIFTVAN_OU_LOCAL_DE_ARMAZENAGEM," +
		" EMBARCADOR_RESTRITA_C_PERDAS_E_DANOS_MATERIAIS_CAUSADOS_AO_OBJETO_SEGURADO_EXCLUSIVAMENTE_POR_INCENDIO_RAIO_OU_EXPLOSAO_ENCALHE," +
		" NAUFRAGIO_OU_SOCOBRAMENTO_DO_NAVIO_OU_EMBARCACAO_CAPOTAGEM_COLISAO_TOMBAMENTO_OU_DESCARRILAMENTO_DE_VEICULO_TERRESTRE_ABALROAMENTO_COLISAO_OU_CONTATO_DO_NAVIO_OU_EMBARCACAO_COM_QUALQUER_OBJETO_EXTERNO_QUE_NAO_SEJA_AGUA_COLISAO_QUEDA_E_OU_ATERRISSAGEM_FORCADA_DA_AERONAVE_DEVIDAMENTE_COMPROVADA_DESCARGA_DA_CARGA_EM_PORTO_DE_ARRIBADA_CARGA_LANCADA_AO_MAR_PERDA_TOTAL_DE_QUALQUER_VOLUME_DURANTE_AS_OPERACOES_DE_CARGA_E_DESCARGA_DO_NAVIO_E," +
		" PERDA_TOTAL_DECORRENTE_DE_FORTUNA_DO_MAR_E_OU_DE_ARREBATAMENTO_PELO_MAR," +
		" RESPONSABILIDADE_CIVIL_DO_OPERADOR_DE_TRANSPORTES_MULTIMODAL_CARGA_RCOTM_C, RESPONSABILIDADE_CIVIL_DO_TRANSPORTADOR_AEREO_CARGA_RCTA_C," +
		" RESPONSABILIDADE_CIVIL_DO_TRANSPORTADOR_AQUAVIARIO_CARGA_RCA_C, RESPONSABILIDADE_CIVIL_DO_TRANSPORTADOR_FERROVIARIO_CARGA_RCTF_C," +
		" RESPONSABILIDADE_CIVIL_DO_TRANSPORTADOR_RODOVIARIO_CARGA_RCTR," +
		" RESPONSABILIDADE_CIVIL_DO_TRANSPORTADOR_RODOVIARIO_EM_VIAGEM_INTERNACIONAL_DANOS_CAUSADOS_A_PESSOAS_OU_COISAS_TRANSPORTADAS_OU_NAO_A_EXCECAO_DA_CARGA_TRANSPORTADA_CARTA_AZUL," +
		" RESPONSABILIDADE_CIVIL_DO_TRANSPORTADOR_RODOVIARIO_EM_VIAGEM_INTERNACIONAL_DANOS_A_CARGA_TRANSPORTADA_RCTR_VI_C_," +
		" RESPONSABILIDADE_CIVIL_DO_TRANSPORTADOR_RODOVIARIO_POR_DESAPARECIMENTO_DE_CARGA_RCF_DC, OUTRAS");
	public static final Set<String> TERM = SetUtils.createSet("ANUAL, ANUAL_INTERMITENTE, PLURIANUAL, PLURIANUAL_INTERMITENTE, MENSAL, MENSAL_INTERMITENTE, DIARIO, DIARIO_INTERMITENTE, OUTROS");
	public static final Set<String> INSURED_PARTICIPATION = SetUtils.createSet("FRANQUIA, POS, NAO_SE_APLICA");
	public static final Set<String> SERVICE_PACKAGE = SetUtils.createSet("ATE_10_SERVICOS, ATE_20_SERVICOS, ACIMA_20_SERVICOS, CUSTOMIZAVEL");
	public static final Set<String> TYPE_SIGNALING = SetUtils.createSet("GRATUITO, PAGO");
	public static final Set<String> POLICY_TYPE = SetUtils.createSet("AVULSA, AVERBACAO, CERTIFICADO, AJUSTAVEL, SEM_AVERBACAO, NAO_SE_APLICA");

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
		assertField(products, Fields.name().setMaxLength(80).setOptional().build());
		assertField(products, Fields.code().setMaxLength(80).build());

		assertField(products,
			new ObjectArrayField
				.Builder("coverages")
				.setValidator(this::assertCoverages)
				.build());

		assertField(products,
			new ObjectField
				.Builder("maxLMG")
				.setValidator(this::assertValue)
				.build());

		assertField(products,
			new ObjectArrayField
				.Builder("assistanceServices")
				.setValidator(assistanceServices -> {
					assertField(assistanceServices,
						new BooleanField
							.Builder("assistanceServices")
							.build());

					assertField(assistanceServices,
						new StringArrayField
							.Builder("assistanceServicesPackage")
							.setMaxLength(17)
							.setEnums(SERVICE_PACKAGE)
							.setOptional()
							.build());

					assertField(assistanceServices,
						new StringField
							.Builder("assistanceServicesDetail")
							.setMaxLength(1000)
							.setOptional()
							.build());

					assertField(assistanceServices,
						new StringField
							.Builder("chargeTypeSignaling")
							.setEnums(TYPE_SIGNALING)
							.setMaxLength(8)
							.setOptional()
							.build());
				})
				.build());

		assertField(products,
			new BooleanField
				.Builder("traits")
				.build());

		assertField(products,
			new ObjectField
				.Builder("validity")
				.setValidator(validity -> {
					assertField(validity,
						new StringArrayField
							.Builder("term")
							.setMaxLength(23)
							.setEnums(TERM)
							.build());

					assertField(validity,
						new StringField
							.Builder("termOthers")
							.setMaxLength(100)
							.setOptional()
							.build());
				})
				.build());

		assertField(products,
			new StringField
				.Builder("premiumRates")
				.setOptional()
				.setMaxLength(1024)
				.build());

		assertField(products,
			new StringArrayField
				.Builder("policyType")
				.setEnums(POLICY_TYPE)
				.setMaxLength(13)
				.build());

		assertField(products,
			new ObjectField
				.Builder("termsAndConditions")
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
							.setOptional()
							.build());
				})
				.build());

		assertField(products,
			new ObjectField
				.Builder("minimumRequirements")
				.setValidator(minimumRequirements -> {
					assertField(minimumRequirements,
						new StringField
							.Builder("minimumRequirementDetails")
							.setMaxLength(1024)
							.build());

					assertField(minimumRequirements,
						new StringArrayField
							.Builder("targetAudiences")
							.setMaxLength(15)
							.setEnums(TARGET_AUDIENCE)
							.build());
				})
				.build());
	}

	private void assertCoverages(JsonObject coverages) {
		assertField(coverages,
			new StringField
				.Builder("coverage")
				.setMaxLength(436)
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
			new StringArrayField
				.Builder("insuredParticipation")
				.setMaxLength(13)
				.setEnums(INSURED_PARTICIPATION)
				.build());

		assertField(coverageAttributes,
			new StringField
				.Builder("insuredParticipationDescription")
				.setMaxLength(1024)
				.setOptional()
				.build());
	}

	public void assertValue(JsonObject minValue) {
		assertField(minValue,
			new NumberField
				.Builder("amount")
				.setMaxLength(36)
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
