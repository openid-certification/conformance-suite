package net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openinsurance.validator.OpenInsuranceLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api Source: swagger/openinsurance/insurancePatrimonial/v1/swagger-insurance-patrimonial.yaml
 * Api endpoint: /{policyId}/claim
 * Api version: 1.0.0
 */

@ApiName("Insurance Patrimonial Claim V1")
public class OpinInsurancePatrimonialClaimValidatorV1 extends AbstractJsonAssertingCondition {
	private final OpenInsuranceLinksAndMetaValidator linksAndMetaValidator = new OpenInsuranceLinksAndMetaValidator(this);
	public static final Set<String> STATUS = SetUtils.createSet("ABERTO, ENCERRADO_COM_INDENIZACAO, ENCERRADO_SEM_INDENIZACAO, REABERTO, CANCELADO_POR_ERRO_OPERACIONAL, AVALIACAO_INICIAL");
	public static final Set<String> JUSTIFICATION = SetUtils.createSet("RISCO_EXCLUIDO, RISCO_AGRAVADO, SEM_DOCUMENTACAO, DOCUMENTACAO_INCOMPLETA, PRESCRICAO, FORA_COBERTURA, OUTROS");
	public static final Set<String> CODE = SetUtils.createSet("LUCRO_BRUTO, LUCRO_LIQUIDO, DESPESAS_FIXAS, PERDA_DE_RECEITA_OU_INTERRUPCAO_DE_NEGOCIOS, OBRAS_CIVIS_CONSTRUCAO_E_INSTALACAO_E_MONTAGEM, AFRETAMENTOS_DE_AERONAVES, ARMAZENAGEM_FORA_DO_CANTEIRO_DE_OBRAS_OU_LOCAL_SEGURADO, DANOS_EM_CONSEQUENCIA_DE_ERRO_DE_PROJETO_RISCO_DO_FABRICANTE, DANOS_MORAIS, DESPESAS_COM_DESENTULHO_DO_LOCAL, DESPESAS_DE_SALVAMENTO_E_CONTENCAO_DE_SINISTROS, DESPESAS_EXTRAORDINARIAS, EQUIPAMENTOS_DE_ESCRITORIO_E_INFORMATICA, EQUIPAMENTOS_MOVEIS_OU_ESTACIONARIOS_UTILIZADOS_NA_OBRA, FERRAMENTAS_DE_PEQUENO_E_MEDIO_PORTE, HONORARIOS_DE_PERITO, INCENDIO_APOS_O_TERMINO_DE_OBRAS_ATE_TRINTA_DIAS_EXCETO_PARA_REFORMAS_OU_AMPLIACOES, LUCROS_CESSANTES, MANUTENCAO_AMPLA_ATE_VINTE_E_QUATRO_MESES, MANUTENCAO_SIMPLES_ATE_VINTE_E_QUATRO_MESES, OBRAS_CONCLUIDAS, OBRAS_TEMPORARIAS, OBRAS_INSTALACOES_CONTRATADAS, PROPRIEDADES_CIRCUNVIZINHAS, RECOMPOSICAO_DE_DOCUMENTOS, RESPONSABILIDADE_CIVIL_EMPREGADOR, FUNDACAO, STANDS_DE_VENDA, TRANSPORTE_TERRESTRE, TUMULTOS_GREVES_E_LOCKOUT, DANOS_MATERIAIS_CAUSADOS_AO_COFRE_FORTE, DANOS_MATERIAIS_CAUSADOS_AOS_CAIXAS_ELETRONICOS_ATM_INFIDELIDADE_DE_FUNCIONARIOS, VALORES_NO_INTERIOR_DO_ESTABELECIMENTO_DENTRO_E_OU_FORA_DE_COFRE_FORTE, VALORES_NO_INTERIOR_DE_CAIXAS_ELETRONICOS_ATM, VALORES_EM_MAOS_DE_PORTADORES_EM_TRANSITO, ALAGAMENTO_INUNDACAO, ALUGUEL_PERDA_OU_PAGAMENTO, ANUNCIOS_LUMINOSOS, BAGAGEM, BASICA_INCENDIO_RAIO_EXPLOSAO, BASICA_DANOS_MATERIAIS, BASICA_DE_OBRAS_CIVIS_EM_CONSTRUCAO_E_INSTALACOES_E_MONTAGENS, BENS_DE_TERCEIROS_EM_PODER_DO_SEGURADO, CARGA_DESCARGA_ICAMENTO_E_DESCIDA, DANOS_ELETRICOS, DANOS_NA_FABRICACAO, DERRAME_D_AGUA_OU_OUTRA_SUBSTANCIA_LIQUIDA_DE_INSTALACOES_DE_CHUVEIROS_AUTOMATICOS, DESMORONAMENTO, DESPESAS_ADICIONAIS_OUTRAS_DESPESAS, DESPESAS_EXTRAORDINARIAS, DESPESAS_FIXA, DETERIORACAO_DE_MERCADORIAS_EM_AMBIENTES, FRIGORIFICADOS, EQUIPAMENTOS_ARRENDADOS, EQUIPAMENTOS_CEDIDOS_A_TERCEIROS, EQUIPAMENTOS_CINEMATOGRAFICOS_FOTOGRAFICOS_DE_AUDIO_E_VIDEO, EQUIPAMENTOS_DIVERSOS_OUTRAS_MODALIDADES, EQUIPAMENTOS_ELETRONICOS, EQUIPAMENTOS_ESTACIONARIOS, EQUIPAMENTOS_MOVEIS, EQUIPAMENTOS_PORTATEIS, FIDELIDADE_DE_EMPREGADOS, HONORARIOS_DE_PERITOS, IMPACTO_DE_VEICULOS_E_QUEDA_DE_AERONAVES, IMPACTO_DE_VEICULOS_TERRESTRES, LINHAS_DE_TRANSMISSAO_E_DISTRIBUICAO, LUCROS_CESSANTES, MOVIMENTACAO_INTERNA_DE_MERCADORIAS, PATIOS, QUEBRA_DE_MAQUINAS, QUEBRA_DE_VIDROS_ESPELHOS_MARMORES_E_GRANITOS, RECOMPOSICAO_DE_REGISTROS_E_DOCUMENTOS, ROUBO_DE_BENS_DE_HOSPEDES, ROUBO_DE_VALORES_EM_TRANSITO_EM_MAOS_DE_PORTADOR, ROUBO_E_FURTO_MEDIANTE_ARROMBAMENTO, ROUBO_E_OU_FURTO_QUALIFICADO_DE_VALORES_NO_INTERIOR_DO_ESTABELECIMENTO_DENTRO_E_OU_FORA_DE_COFRES_FORTES_OU_CAIXAS_FORTES, TERRORISMO_E_SABOTAGEM, TUMULTOS_GREVES_LOCKOUT_E_ATOS_DOLOSOS, VAZAMENTO_DE_TUBULACOES_E_TANQUES, VAZAMENTO_DE_TUBULACOES_HIDRAULICAS, VENDAVAL_FURACAO_CICLONE_TORNADO_GRANIZO_QUEDA_DE_AERONAVES_OU_QUAISQUER_OUTROS_ENGENHOS_AEREOS_OU_ESPACIAIS_IMPACTO_DE_VEICULOS_TERRESTRES_E_FUMACA, OUTRAS");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertInnerFields)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertInnerFields(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("identification")
				.setMaxLength(50)
				.build());

		assertField(data,
			new StringField
				.Builder("documentationDeliveryDate")
				.setMaxLength(10)
				.build());

		assertField(data,
			new StringField
				.Builder("status")
				.setMaxLength(30)
				.setEnums(STATUS)
				.build());

		assertField(data,
			new StringField
				.Builder("statusAlterationDate")
				.setMaxLength(10)
				.build());

		assertField(data,
			new StringField
				.Builder("occurrenceDate")
				.setMaxLength(10)
				.build());

		assertField(data,
			new StringField
				.Builder("warningDate")
				.setMaxLength(10)
				.build());

		assertField(data,
			new StringField
				.Builder("thirdPartyClaimDate")
				.setMaxLength(10)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("amount")
				.setValidator(this::assertAmount)
				.build());

		assertField(data,
			new StringField
				.Builder("denialJustification")
				.setMaxLength(23)
				.setEnums(JUSTIFICATION)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("denialJustificationDescription")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("coverages")
				.setValidator(this::assertCoverages)
				.setOptional()
				.build());
	}

	private void assertCoverages(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("insuredObjectId")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("branch")
				.setMaxLength(4)
				.build());

		assertField(data,
			new StringField
				.Builder("code")
				.setMaxLength(148)
				.setEnums(CODE)
				.build());

		assertField(data,
			new StringField
				.Builder("description")
				.setMaxLength(500)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("warningDate")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("thirdPartyClaimDate")
				.setOptional()
				.build());
	}

	private void assertAmount(JsonObject data) {
		assertField(data,
			new DoubleField
				.Builder("amount")
				.setPattern("^\\d{1,16}\\.\\d{2}$")
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$")
				.build());
	}
}
