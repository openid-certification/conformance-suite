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
 * Api Source: swagger/openinsurance/productsServices/swagger-named-operational-risks.yaml
 * Api endpoint: /named-operational-risks/
 * Api version: 1.0.0
 */

@ApiName("ProductsServices Named Operational Risks")
public class GetNamedOperationalRisksValidator extends AbstractJsonAssertingCondition {
	private static class Fields extends ProductNServicesCommonFields {
	}

	public static final Set<String> COVERAGE = Sets.newHashSet("ALAGAMENTO_INUNDACAO", "ALUGUEL_PERDA_OU_PAGAMENTO", "ANUNCIOS_LUMINOSOS", "BAGAGEM", "BASICA_INCENDIO_RAIO_EXPLOSAO", "BASICA_DANOS_MATERIAIS", "BASICA_DE_OBRAS_CIVIS_EM_CONSTRUCAO_E_INSTALACOES_E_MONTAGENS", "BENS_DE_TERCEIROS_EM_PODER_DO_SEGURADO", "CARGA_DESCARGA_ICAMENTO_E_DESCIDA", "DANOS_ELETRICOS", "DANOS_NA_FABRICACAO", "DERRAME_D’AGUA_OU_OUTRA_SUBSTANCIA_LIQUIDA_DE_INSTALACOES_DE_CHUVEIROS_AUTOMATICOS_SPRINKLERS", "DESMORONAMENTO", "DESPESAS_ADICIONAIS_OUTRAS_DESPESAS", "DESPESAS_EXTRAORDINARIAS", "DESPESAS_FIXA", "DETERIORACAO_DE_MERCADORIAS_EM_AMBIENTES_FRIGORIFICADOS", "EQUIPAMENTOS_ARRENDADOS", "EQUIPAMENTOS_CEDIDOS_A_TERCEIROS", "EQUIPAMENTOS_CINEMATOGRAFICOS_FOTOGRAFICOS_DE_AUDIO_E_VIDEO", "EQUIPAMENTOS_ELETRONICOS", "EQUIPAMENTOS_DIVERSOS_OUTRAS_MODALIDADES", "EQUIPAMENTOS_ESTACIONARIOS", "EQUIPAMENTOS_MOVEIS", "EQUIPAMENTOS_PORTATEIS_", "FIDELIDADE_DE_EMPREGADOS", "HONORARIOS_DE_PERITOS", "IMPACTO_DE_VEICULOS_E_QUEDA_DE_AERONAVES", "IMPACTO_DE_VEICULOS_TERRESTRES", "LINHAS_DE_TRANSMISSAO_E_DISTRIBUICAO", "LUCROS_CESSANTES", "MOVIMENTACAO_INTERNA_DE_MERCADORIAS", "PATIOS", "QUEBRA_DE_MAQUINAS", "QUEBRA_DE_VIDROS_ESPELHOS_MARMORES_E_GRANITOS", "RECOMPOSICAO_DE_REGISTROS_E_DOCUMENTOS", "ROUBO_DE_BENS_DE_HOSPEDES", "ROUBO_DE_VALORES_EM_TRANSITO_EM_MAOS_DE_PORTADOR", "ROUBO_E_FURTO_MEDIANTE_ARROMBAMENTO", "ROUBO_E_OU_FURTO_QUALIFICADO_DE_VALORES_NO_INTERIOR_DO_ESTABELECIMENTO_DENTRO_E_OU_FORA_DE_COFRES_FORTES_OU_CAIXAS_FORTES", "TERRORISMO_E_SABOTAGEM", "TUMULTOS_GREVES_LOCKOUT_E_ATOS_DOLOSOS", "VAZAMENTO_DE_TUBULACOES_E_TANQUES", "VAZAMENTO_DE_TUBULACOES_HIDRAULICAS", "VENDAVAL_FURACAO_CICLONE_TORNADO_GRANIZO_QUEDA_DE_AERONAVES_OU_QUAISQUER_OUTROS_ENGENHOS_AEREOS_OU_ESPACIAIS_IMPACTO_DE_VEICULOS_TERRESTRES_E_FUMACA", "OUTRAS");
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
		assertField(products, Fields.code().setMaxLength(100).build());

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
				.setMaxLength(148)
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
				.setMaxLength(2)
				.build());

		assertField(unit,
			new StringField
				.Builder("description")
				.setMaxLength(5)
				.build());
	}
}
