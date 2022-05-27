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
 * Api Source: swagger/openinsurance/productsServices/swagger-rural.yaml
 * Api endpoint: /rural/
 * Api version: 1.0.0
 */

@ApiName("ProductsServices Rural")
public class GetRuralValidator extends AbstractJsonAssertingCondition {

	private static class Fields extends ProductNServicesCommonFields {}

	public static final Set<String> TARGET_AUDIENCE = SetUtils.createSet("PESSOA_FISICA, PESSOA_JURIDICA");
	public static final Set<String> COVERAGE = SetUtils.createSet("GRANIZO, GEADA, GRANIZO_GEADA, GRANIZO_GEADA_CHUVA_EXCESSIVA, " +
		"COMPREENSIVA_INCENDIO_E_RAIO_TROMBA_D_AGUA_VENTOS_FORTES_E_VENTOS_FRIOS_CHUVAS_EXCESSIVAS_SECA_VARIACAO_EXCESSIVA_DE_TEMPERATURA_GRANIZO_GEADA, " +
		"COMPREENSIVA_INCENDIO_E_RAIO_TROMBA_D_AGUA_VENTOS_FORTES_E_VENTOS_FRIOS_CHUVAS_EXCESSIVAS_SECA_VARIACAO_EXCESSIVA_DE_TEMPERATURA_GRANIZO_GEADA_COM_DOENCAS_E_PRAGAS, " +
		"CANCRO_CITRICO, COMPREENSIVA_PARA_A_MODALIDADE_BENFEITORIAS_E_PRODUTOS_AGROPECUARIOS_INCENDIO_RAIO_EXPLOSAO_VENDAVAL_GRANIZO_TREMORES_DE_TERRA_IMPACTO_DE_VEICULOS_DESMORONAMENTO_TOTAL_OU_PARCIAL_DANOS_AS_MERCADORIAS_DO_SEGURADO_EXCLUSIVAMENTE_PARA_OS_PRODUTOS_AGROPECUARIOS_DECORRENTES_DE_ACIDENTES_COM_O_VEICULO_TRANSPORTADOR_DANOS_AS_MAQUINAS_AGRICOLAS_E_SEUS_IMPLEMENTOS, DECORRENTES_DE_COLISAO_ABALROAMENTO_E_OU_CAPOTAGEM_QUEDA_DE, " +
		"PONTES_VIADUTOS_OU_EM_PRECIPICIOS_ROUBO_OU_FURTO_TOTAL_CASO, FORTUITO_OU_FORCA_MAIOR_OCORRIDOS_DURANTE_O_TRANSPORTE, COMPREENSIVA_PARA_A_MODALIDADE_PENHOR_RURAL, MORTE_DE_ANIMAIS, " +
		"CONFINAMENTO_SEMI_CONFINAMENTO_BOVINOS_DE_CORTE, CONFINAMENTO_BOVINOS_DE_LEITE, VIAGEM, EXPOSICAO_MOSTRA_E_LEILAO, CARREIRA, SALTO_E_ADESTRAMENTO, PROVAS_FUNCIONAIS, HIPISMO_RURAL, " +
		"POLO, TROTE, VAQUEJADA, EXTENSAO_DE_COBERTURA_EM_TERRITORIO_ESTRANGEIRO, TRANSPORTE, RESPONSABILIDADE_CIVIL, PERDA_DE_FERTILIDADE_DE_GARANHAO, REEMBOLSO_CIRURGICO, COLETA_DE_SEMEN, PREMUNICAO, COMPREENSIVA_PARA_A_MODALIDADE_FLORESTAS, VIDA_DO_PRODUTOR_RURAL, BASICA_DE_FATURAMENTO_PARA_O_PECUARIO, OUTRAS");
	public static final Set<String> TERMS = SetUtils.createSet("ANUAL, ANUAL_INTERMITENTE, PLURIANUAL, PLURIANUAL_INTERMITENTE, MENSAL, MENSAL_INTERMITENTE, DIARIO, DIARIO_INTERMITENTE, OUTROS");
	public static final Set<String> MODALITY = SetUtils.createSet("AGRICOLA, PECUARIO, AQUICOLA, FLORESTAS, BENFEITORIAS_E_PRODUTOS_AGROPECUARIOS, PENHOR_RURAL, ANIMAIS, VIDA_DO_PRODUTOR_RURAL");
	public static final Set<String> INSURED_PARTICIPATION = SetUtils.createSet("FRANQUIA, POS, NAO_SE_APLICA");
	public static final Set<String> CROPS = SetUtils.createSet("FRUTAS, GRAOS_DE_VERAO, GRAOS_DE_INVERNO, OLERICOLAS, OUTROS");
	public static final Set<String> FOREST_CODE = SetUtils.createSet("PINUS, EUCALIPITO, TECA, SERINGUEIRA, OUTROS");
	public static final Set<String> FLOCK_CODE = SetUtils.createSet("BOVINOS, EQUINOS, OVINOS, SUINOS, CAPRINOS, AVES, BUBALINOS, OUTROS");
	public static final Set<String> ANIMAL_DESTINATION = SetUtils.createSet("PRODUCAO, TRABALHO_DESTINADOS_A_SELA, TRABALHO_POR_TRACAO, TRANSPORTE_NO_MANEJO_DA_FAZENDA, ATIVIDADE_REPRODUTIVA");
	public static final Set<String> ANIMALS_CLASSIFICATION = SetUtils.createSet("ANIMAIS_DESTINADOS_A_COMPANHIA, CONVIVIO_FAMILIAR, SEGURANCA, LAZER, EXPOSICAO, ATIVIDADES_ESPORTIVAS, ATIVIDADE_REPRODUTIVA");
	public static final Set<String> PAYMENT_METHOD = SetUtils.createSet("CARTAO_DE_CREDITO, CARTAO_DE_DEBITO, DEBITO_EM_CONTA_CORRENTE, DEBITO_EM_CONTA_POUPANCA, BOLETO_BANCARIO, PIX, CONSIGNACAO_EM_FOLHA_DE_PAGAMENTO, PONTOS_DE_PROGRAMA_DE_BENEFICIO, OUTROS");
	public static final Set<String> PAYMENT_TYPE = SetUtils.createSet("A_VISTA, PARCELADO");
	public static final Set<String> CONTRACT_TYPE = SetUtils.createSet("COLETIVO, INDIVIDUAL");

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
			new StringField
				.Builder("modality")
				.setMaxLength(37)
				.setEnums(MODALITY)
				.build());

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
			new BooleanField
				.Builder("traits")
				.build());

		assertField(products,
			new StringArrayField
				.Builder("crops")
				.setEnums(CROPS)
				.setMaxLength(16)
				.setOptional()
				.build());

		assertField(products,
			new StringField
				.Builder("cropsOthers")
				.setMaxLength(1024)
				.setOptional()
				.build());

		assertField(products,
			new StringArrayField
				.Builder("forestCode")
				.setEnums(FOREST_CODE)
				.setMaxLength(11)
				.setOptional()
				.build());

		assertField(products,
			new StringField
				.Builder("forestCodeOthers")
				.setMaxLength(1024)
				.setOptional()
				.build());

		assertField(products,
			new StringArrayField
				.Builder("flockCode")
				.setEnums(FLOCK_CODE)
				.setMaxLength(9)
				.setOptional()
				.build());

		assertField(products,
			new StringField
				.Builder("flockCodeOthers")
				.setMaxLength(1024)
				.setOptional()
				.build());

		assertField(products,
			new StringArrayField
				.Builder("animalDestination")
				.setEnums(ANIMAL_DESTINATION)
				.setMaxLength(31)
				.setOptional()
				.build());

		assertField(products,
			new StringArrayField
				.Builder("animalsClassification")
				.setEnums(ANIMALS_CLASSIFICATION)
				.setMaxLength(30)
				.setOptional()
				.build());

		assertField(products,
			new BooleanField
				.Builder("subvention")
				.setOptional()
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
				.Builder("validity")
				.setValidator(validity -> {
					assertField(validity,
						new StringArrayField
							.Builder("term")
							.setMaxLength(23)
							.setEnums(TERMS)
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
			new ObjectArrayField
				.Builder("premiumPayment")
				.setValidator(this::assertPremiumPayment)
				.build());

		assertField(products,
			new ObjectField
				.Builder("minimumRequirements")
				.setValidator(minimumRequirements -> {
					assertField(minimumRequirements,
						new StringField
							.Builder("contractType")
							.setMaxLength(10)
							.setEnums(CONTRACT_TYPE)
							.build());

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
				.setMaxLength(357)
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
