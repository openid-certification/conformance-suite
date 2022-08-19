package net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openinsurance.validator.OpenInsuranceLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api Source: swagger/openinsurance/insurancePatrimonial/v1/swagger-insurance-patrimonial.yaml
 * Api endpoint: /{policyId}/premium
 * Api version: 1.0.0
 */

@ApiName("Insurance Patrimonial Premium V1")
public class OpinInsurancePatrimonialPremiumValidatorV1 extends AbstractJsonAssertingCondition {

	public static final Set<String> CODE = SetUtils.createSet("LUCRO_BRUTO, LUCRO_LIQUIDO, DESPESAS_FIXAS, PERDA_DE_RECEITA_OU_INTERRUPCAO_DE_NEGOCIOS, OBRAS_CIVIS_CONSTRUCAO_E_INSTALACAO_E_MONTAGEM, AFRETAMENTOS_DE_AERONAVES, ARMAZENAGEM_FORA_DO_CANTEIRO_DE_OBRAS_OU_LOCAL_SEGURADO, DANOS_EM_CONSEQUENCIA_DE_ERRO_DE_PROJETO_RISCO_DO_FABRICANTE, DANOS_MORAIS, DESPESAS_COM_DESENTULHO_DO_LOCAL, DESPESAS_DE_SALVAMENTO_E_CONTENCAO_DE_SINISTROS, DESPESAS_EXTRAORDINARIAS, EQUIPAMENTOS_DE_ESCRITORIO_E_INFORMATICA, EQUIPAMENTOS_MOVEIS_OU_ESTACIONARIOS_UTILIZADOS_NA_OBRA, FERRAMENTAS_DE_PEQUENO_E_MEDIO_PORTE, HONORARIOS_DE_PERITO, INCENDIO_APOS_O_TERMINO_DE_OBRAS_ATE_TRINTA_DIAS_EXCETO_PARA_REFORMAS_OU_AMPLIACOES, LUCROS_CESSANTES, MANUTENCAO_AMPLA_ATE_VINTE_E_QUATRO_MESES, MANUTENCAO_SIMPLES_ATE_VINTE_E_QUATRO_MESES, OBRAS_CONCLUIDAS, OBRAS_TEMPORARIAS, OBRAS_INSTALACOES_CONTRATADAS, PROPRIEDADES_CIRCUNVIZINHAS, RECOMPOSICAO_DE_DOCUMENTOS, RESPONSABILIDADE_CIVIL_EMPREGADOR, FUNDACAO, STANDS_DE_VENDA, TRANSPORTE_TERRESTRE, TUMULTOS_GREVES_E_LOCKOUT, DANOS_MATERIAIS_CAUSADOS_AO_COFRE_FORTE, DANOS_MATERIAIS_CAUSADOS_AOS_CAIXAS_ELETRONICOS_ATM_INFIDELIDADE_DE_FUNCIONARIOS, VALORES_NO_INTERIOR_DO_ESTABELECIMENTO_DENTRO_E_OU_FORA_DE_COFRE_FORTE, VALORES_NO_INTERIOR_DE_CAIXAS_ELETRONICOS_ATM, VALORES_EM_MAOS_DE_PORTADORES_EM_TRANSITO, ALAGAMENTO_INUNDACAO, ALUGUEL_PERDA_OU_PAGAMENTO, ANUNCIOS_LUMINOSOS, BAGAGEM, BASICA_INCENDIO_RAIO_EXPLOSAO, BASICA_DANOS_MATERIAIS, BASICA_DE_OBRAS_CIVIS_EM_CONSTRUCAO_E_INSTALACOES_E_MONTAGENS, BENS_DE_TERCEIROS_EM_PODER_DO_SEGURADO, CARGA_DESCARGA_ICAMENTO_E_DESCIDA, DANOS_ELETRICOS, DANOS_NA_FABRICACAO, DERRAME_D_AGUA_OU_OUTRA_SUBSTANCIA_LIQUIDA_DE_INSTALACOES_DE_CHUVEIROS_AUTOMATICOS, DESMORONAMENTO, DESPESAS_ADICIONAIS_OUTRAS_DESPESAS, DESPESAS_EXTRAORDINARIAS, DESPESAS_FIXA, DETERIORACAO_DE_MERCADORIAS_EM_AMBIENTES, FRIGORIFICADOS, EQUIPAMENTOS_ARRENDADOS, EQUIPAMENTOS_CEDIDOS_A_TERCEIROS, EQUIPAMENTOS_CINEMATOGRAFICOS_FOTOGRAFICOS_DE_AUDIO_E_VIDEO, EQUIPAMENTOS_DIVERSOS_OUTRAS_MODALIDADES, EQUIPAMENTOS_ELETRONICOS, EQUIPAMENTOS_ESTACIONARIOS, EQUIPAMENTOS_MOVEIS, EQUIPAMENTOS_PORTATEIS, FIDELIDADE_DE_EMPREGADOS, HONORARIOS_DE_PERITOS, IMPACTO_DE_VEICULOS_E_QUEDA_DE_AERONAVES, IMPACTO_DE_VEICULOS_TERRESTRES, LINHAS_DE_TRANSMISSAO_E_DISTRIBUICAO, LUCROS_CESSANTES, MOVIMENTACAO_INTERNA_DE_MERCADORIAS, PATIOS, QUEBRA_DE_MAQUINAS, QUEBRA_DE_VIDROS_ESPELHOS_MARMORES_E_GRANITOS, RECOMPOSICAO_DE_REGISTROS_E_DOCUMENTOS, ROUBO_DE_BENS_DE_HOSPEDES, ROUBO_DE_VALORES_EM_TRANSITO_EM_MAOS_DE_PORTADOR, ROUBO_E_FURTO_MEDIANTE_ARROMBAMENTO, ROUBO_E_OU_FURTO_QUALIFICADO_DE_VALORES_NO_INTERIOR_DO_ESTABELECIMENTO_DENTRO_E_OU_FORA_DE_COFRES_FORTES_OU_CAIXAS_FORTES, TERRORISMO_E_SABOTAGEM, TUMULTOS_GREVES_LOCKOUT_E_ATOS_DOLOSOS, VAZAMENTO_DE_TUBULACOES_E_TANQUES, VAZAMENTO_DE_TUBULACOES_HIDRAULICAS, VENDAVAL_FURACAO_CICLONE_TORNADO_GRANIZO_QUEDA_DE_AERONAVES_OU_QUAISQUER_OUTROS_ENGENHOS_AEREOS_OU_ESPACIAIS_IMPACTO_DE_VEICULOS_TERRESTRES_E_FUMACA, OUTRAS");
	public static final Set<String> MOVEMENT_TYPE = SetUtils.createSet("LIQUIDACAO_DE_PREMIO, LIQUIDACAO_DE_RESTITUICAO_DE_PREMIO, LIQUIDACAO_DE_CUSTO_DE_AQUISICAO, LIQUIDACAO_DE_RESTITUICAO_DE_CUSTO_DE_AQUISICAO, ESTORNO_DE_PREMIO, ESTORNO_DE_RESTITUICAO_DE_PREMIO, ESTORNO_DE_CUSTO_DE_AQUISICAO, EMISSAO_DE_PREMIO, CANCELAMENTO_DE_PARCELA, EMISSAO_DE_RESTITUICAO_DE_PREMIO, REABERTURA_DE_PARCELA, BAIXA_POR_PERDA");
	public static final Set<String> MOVEMENT_ORIGIN = SetUtils.createSet("EMISSAO_DIRETA, EMISSAO_ACEITA_DE_COSSEGURO, EMISSAO_CEDIDA_DE_COSSEGURO");
	public static final Set<String> TELLERID_TYPE = SetUtils.createSet("CPF, CNPJ, OUTROS");
	public static final Set<String> PAYMENT_TYPE = SetUtils.createSet("BOLETO, TED, TEF, CARTAO, DOC, CHEQUE, DESCONTO_EM_FOLHA, PIX, DINHEIRO_EM_ESPECIE, OUTROS");
	private final OpenInsuranceLinksAndMetaValidator linksAndMetaValidator = new OpenInsuranceLinksAndMetaValidator(this);

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectField
				.Builder(ROOT_PATH)
				.setValidator(this::assertData)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		return environment;
	}

	private void assertData(JsonObject data) {
		assertField(data,
			new NumberField
				.Builder("paymentsQuantity")
				.setMaxLength(3)
				.build());

		assertField(data,
			new ObjectField
				.Builder("amount")
				.setValidator(this::assertAmount)
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("coverages")
				.setValidator(this::assertCoverages)
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("payments")
				.setValidator(this::assertPayments)
				.build());
	}

	private void assertPayments(JsonObject payments) {
		assertField(payments,
			new StringField
				.Builder("movementDate")
				.build());

		assertField(payments,
			new StringField
				.Builder("movementType")
				.setMaxLength(47)
				.setEnums(MOVEMENT_TYPE)
				.build());

		assertField(payments,
			new StringField
				.Builder("movementOrigin")
				.setMaxLength(27)
				.setEnums(MOVEMENT_ORIGIN)
				.setOptional()
				.build());

		assertField(payments,
			new NumberField
				.Builder("movementPaymentsNumber")
				.setMaxLength(3)
				.build());

		assertField(payments,
			new ObjectField
				.Builder("amount")
				.setValidator(this::assertAmount)
				.build());

		assertField(payments,
			new StringField
				.Builder("maturityDate")
				.build());

		assertField(payments,
			new StringField
				.Builder("tellerId")
				.setMaxLength(1024)
				.setOptional()
				.build());

		assertField(payments,
			new StringField
				.Builder("tellerIdType")
				.setMaxLength(6)
				.setEnums(TELLERID_TYPE)
				.setOptional()
				.build());

		assertField(payments,
			new StringField
				.Builder("tellerName")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(payments,
			new StringField
				.Builder("financialInstitutionCode")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(payments,
			new StringField
				.Builder("paymentType")
				.setMaxLength(19)
				.setEnums(PAYMENT_TYPE)
				.setOptional()
				.build());
	}

	private void assertCoverages(JsonObject coverages) {
		assertField(coverages,
			new StringField
				.Builder("branch")
				.setMaxLength(4)
				.build());

		assertField(coverages,
			new StringField
				.Builder("code")
				.setMaxLength(148)
				.setEnums(CODE)
				.build());

		assertField(coverages,
			new StringField
				.Builder("description")
				.setMaxLength(500)
				.setOptional()
				.build());

		assertField(coverages,
			new ObjectField
				.Builder("premiumAmount")
				.setValidator(this::assertAmount)
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
