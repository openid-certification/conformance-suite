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
 * Api endpoint: /{policyId}/policy-info
 * Api version: 1.0.0
 */

@ApiName("Insurance Patrimonial PolicyInfo V1")
public class OpinInsurancePatrimonialPolicyInfoValidatorV1 extends AbstractJsonAssertingCondition {

	public static final Set<String> DOCUMENT_TYPE = SetUtils.createSet("APOLICE_INDIVIDUAL, BILHETE, CERTIFICADO, APOLICE_INDIVIDUAL_AUTOMOVEL, APOLICE_FROTA_AUTOMOVEL");
	public static final Set<String> ISSUANCE_TYPE = SetUtils.createSet("EMISSAO_PROPRIA, COSSEGURO_ACEITO");
	public static final Set<String> IDENTIFICATION_TYPE = SetUtils.createSet("CPF, CNPJ, OUTROS");
	public static final Set<String> INTERMEDIARIES_TYPE = SetUtils.createSet("CORRETOR, REPRESENTANTE, ESTIPULANTE, CORRESPONDENTE, AGENTE_DE_MICROSSEGUROS, OUTROS");
	public static final Set<String> INSURED_OBJECTS_TYPE = SetUtils.createSet("CONTRATO, PROCESSO_ADMINISTRATIVO, PROCESSO_JUDICIAL, AUTOMOVEL, CONDUTOR, OUTROS");
	public static final Set<String> GRACE_PERIODICITY = SetUtils.createSet("DIA, MES, ANO");
	public static final Set<String> GRACE_PERIOD_COUNTING_METHOD = SetUtils.createSet("DIAS_UTEIS, DIAS_CORRIDOS");
	public static final Set<String> CODE = SetUtils.createSet("LUCRO_BRUTO, LUCRO_LIQUIDO, DESPESAS_FIXAS, PERDA_DE_RECEITA_OU_INTERRUPCAO_DE_NEGOCIOS, OBRAS_CIVIS_CONSTRUCAO_E_INSTALACAO_E_MONTAGEM, AFRETAMENTOS_DE_AERONAVES, ARMAZENAGEM_FORA_DO_CANTEIRO_DE_OBRAS_OU_LOCAL_SEGURADO, DANOS_EM_CONSEQUENCIA_DE_ERRO_DE_PROJETO_RISCO_DO_FABRICANTE, DANOS_MORAIS, DESPESAS_COM_DESENTULHO_DO_LOCAL, DESPESAS_DE_SALVAMENTO_E_CONTENCAO_DE_SINISTROS, DESPESAS_EXTRAORDINARIAS, EQUIPAMENTOS_DE_ESCRITORIO_E_INFORMATICA, EQUIPAMENTOS_MOVEIS_OU_ESTACIONARIOS_UTILIZADOS_NA_OBRA, FERRAMENTAS_DE_PEQUENO_E_MEDIO_PORTE, HONORARIOS_DE_PERITO, INCENDIO_APOS_O_TERMINO_DE_OBRAS_ATE_TRINTA_DIAS_EXCETO_PARA_REFORMAS_OU_AMPLIACOES, LUCROS_CESSANTES, MANUTENCAO_AMPLA_ATE_VINTE_E_QUATRO_MESES, MANUTENCAO_SIMPLES_ATE_VINTE_E_QUATRO_MESES, OBRAS_CONCLUIDAS, OBRAS_TEMPORARIAS, OBRAS_INSTALACOES_CONTRATADAS, PROPRIEDADES_CIRCUNVIZINHAS, RECOMPOSICAO_DE_DOCUMENTOS, RESPONSABILIDADE_CIVIL_EMPREGADOR, FUNDACAO, STANDS_DE_VENDA, TRANSPORTE_TERRESTRE, TUMULTOS_GREVES_E_LOCKOUT, DANOS_MATERIAIS_CAUSADOS_AO_COFRE_FORTE, DANOS_MATERIAIS_CAUSADOS_AOS_CAIXAS_ELETRONICOS_ATM_INFIDELIDADE_DE_FUNCIONARIOS, VALORES_NO_INTERIOR_DO_ESTABELECIMENTO_DENTRO_E_OU_FORA_DE_COFRE_FORTE, VALORES_NO_INTERIOR_DE_CAIXAS_ELETRONICOS_ATM, VALORES_EM_MAOS_DE_PORTADORES_EM_TRANSITO, ALAGAMENTO_INUNDACAO, ALUGUEL_PERDA_OU_PAGAMENTO, ANUNCIOS_LUMINOSOS, BAGAGEM, BASICA_INCENDIO_RAIO_EXPLOSAO, BASICA_DANOS_MATERIAIS, BASICA_DE_OBRAS_CIVIS_EM_CONSTRUCAO_E_INSTALACOES_E_MONTAGENS, BENS_DE_TERCEIROS_EM_PODER_DO_SEGURADO, CARGA_DESCARGA_ICAMENTO_E_DESCIDA, DANOS_ELETRICOS, DANOS_NA_FABRICACAO, DERRAME_D_AGUA_OU_OUTRA_SUBSTANCIA_LIQUIDA_DE_INSTALACOES_DE_CHUVEIROS_AUTOMATICOS, DESMORONAMENTO, DESPESAS_ADICIONAIS_OUTRAS_DESPESAS, DESPESAS_EXTRAORDINARIAS, DESPESAS_FIXA, DETERIORACAO_DE_MERCADORIAS_EM_AMBIENTES, FRIGORIFICADOS, EQUIPAMENTOS_ARRENDADOS, EQUIPAMENTOS_CEDIDOS_A_TERCEIROS, EQUIPAMENTOS_CINEMATOGRAFICOS_FOTOGRAFICOS_DE_AUDIO_E_VIDEO, EQUIPAMENTOS_DIVERSOS_OUTRAS_MODALIDADES, EQUIPAMENTOS_ELETRONICOS, EQUIPAMENTOS_ESTACIONARIOS, EQUIPAMENTOS_MOVEIS, EQUIPAMENTOS_PORTATEIS, FIDELIDADE_DE_EMPREGADOS, HONORARIOS_DE_PERITOS, IMPACTO_DE_VEICULOS_E_QUEDA_DE_AERONAVES, IMPACTO_DE_VEICULOS_TERRESTRES, LINHAS_DE_TRANSMISSAO_E_DISTRIBUICAO, LUCROS_CESSANTES, MOVIMENTACAO_INTERNA_DE_MERCADORIAS, PATIOS, QUEBRA_DE_MAQUINAS, QUEBRA_DE_VIDROS_ESPELHOS_MARMORES_E_GRANITOS, RECOMPOSICAO_DE_REGISTROS_E_DOCUMENTOS, ROUBO_DE_BENS_DE_HOSPEDES, ROUBO_DE_VALORES_EM_TRANSITO_EM_MAOS_DE_PORTADOR, ROUBO_E_FURTO_MEDIANTE_ARROMBAMENTO, ROUBO_E_OU_FURTO_QUALIFICADO_DE_VALORES_NO_INTERIOR_DO_ESTABELECIMENTO_DENTRO_E_OU_FORA_DE_COFRES_FORTES_OU_CAIXAS_FORTES, TERRORISMO_E_SABOTAGEM, TUMULTOS_GREVES_LOCKOUT_E_ATOS_DOLOSOS, VAZAMENTO_DE_TUBULACOES_E_TANQUES, VAZAMENTO_DE_TUBULACOES_HIDRAULICAS, VENDAVAL_FURACAO_CICLONE_TORNADO_GRANIZO_QUEDA_DE_AERONAVES_OU_QUAISQUER_OUTROS_ENGENHOS_AEREOS_OU_ESPACIAIS_IMPACTO_DE_VEICULOS_TERRESTRES_E_FUMACA, OUTRAS");
	public static final Set<String> FEATURE = SetUtils.createSet("MASSIFICADOS, MASSIFICADOS_MICROSEGUROS, GRANDES_RISCOS");
	public static final Set<String> TYPE = SetUtils.createSet("PARAMETRICO, INTERMITENTE, REGULAR_COMUM, CAPITAL_GLOBAL, PARAMETRICO_E_INTERMITENTE");
	public static final Set<String> DEDUCTIBLE_TYPE = SetUtils.createSet("REDUZIDA, NORMAL, MAJORADA, DEDUTIVEL, OUTROS");
	public static final Set<String> APPLICATION_TYPE = SetUtils.createSet("VALOR, PERCENTUAL, OUTROS");
	public static final Set<String> BASIC_COVERAGE_INDEX = SetUtils.createSet("SIMPLES, AMPLA");
	public static final Set<String> PROPERTY_TYPE = SetUtils.createSet("CASA, APARTAMENTO, CONDOMINIO_RESIDENCIAL, CONDOMINIO_COMERCIAL, CONDOMINIO_MISTOS");
	public static final Set<String> STRUCTURING_TYPE = SetUtils.createSet("CONDOMINIO_VERTICAL, CONDOMINIO_HORIZONTAL, MISTO");
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
			new StringField
				.Builder("documentType")
				.setMaxLength(28)
				.setEnums(DOCUMENT_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("policyId")
				.setMaxLength(60)
				.build());

		assertField(data,
			new StringField
				.Builder("susepProcessNumber")
				.setMaxLength(60)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("groupCertificateId")
				.setMaxLength(60)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("issuanceType")
				.setMaxLength(16)
				.setEnums(ISSUANCE_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("issuanceDate")
				.setMaxLength(10)
				.build());

		assertField(data,
			new StringField
				.Builder("termStartDate")
				.setMaxLength(10)
				.build());

		assertField(data,
			new StringField
				.Builder("termEndDate")
				.setMaxLength(10)
				.build());

		assertField(data,
			new StringField
				.Builder("leadInsurerCode")
				.setMaxLength(1024)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("leadInsurerPolicyId")
				.setMaxLength(1024)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("maxLMG")
				.setValidator(this::assertAmount)
				.build());

		assertField(data,
			new StringField
				.Builder("proposalId")
				.setMaxLength(60)
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("insureds")
				.setValidator(this::assertPersonalInfo)
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("beneficiaries")
				.setValidator(this::assertPersonalInfo)
				.setOptional()
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("principals")
				.setValidator(this::assertPersonalInfo)
				.setOptional()
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("intermediaries")
				.setValidator(this::assertIntermediaries)
				.setOptional()
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("insuredObjects")
				.setValidator(this::assertInsuredObjects)
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("coverages")
				.setValidator(this::assertCoverages)
				.setMinProperties(3)
				.setOptional()
				.build());

		assertField(data,
			new DoubleField
				.Builder("coinsuranceRetainedPercentage")
				.setPattern("^\\d{1,3}\\.\\d{1,9}$")
				.setOptional()
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("coinsurers")
				.setValidator(coinsurers -> {
					assertField(coinsurers,
						new StringField
							.Builder("identification")
							.setMaxLength(60)
							.build());

					assertField(coinsurers,
						new DoubleField
							.Builder("cededPercentage")
							.setPattern("^\\d{1,3}\\.\\d{1,9}$")
							.build());
				})
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("branchInfo")
				.setValidator(branchInfo -> {
					assertField(branchInfo,
						new StringField
							.Builder("basicCoverageIndex")
							.setMaxLength(7)
							.setEnums(BASIC_COVERAGE_INDEX)
							.setOptional()
							.build());

					assertField(branchInfo,
						new ObjectArrayField
							.Builder("insuredObjects")
							.setValidator(this::assertSpecificInsuredObjects)
							.setOptional()
							.build());
				})
				.setOptional()
				.build());
	}

	private void assertPersonalInfo(JsonObject insureds) {
		assertField(insureds,
			new StringField
				.Builder("identification")
				.setMaxLength(60)
				.build());

		assertField(insureds,
			new StringField
				.Builder("identificationType")
				.setMaxLength(6)
				.setEnums(IDENTIFICATION_TYPE)
				.build());

		assertField(insureds,
			new StringField
				.Builder("name")
				.setMaxLength(60)
				.build());

		assertField(insureds,
			new StringField
				.Builder("postCode")
				.setMaxLength(60)
				.build());

		assertField(insureds,
			new StringField
				.Builder("email")
				.setMaxLength(256)
				.setOptional()
				.build());

		assertField(insureds,
			new StringField
				.Builder("city")
				.setMaxLength(60)
				.build());

		assertField(insureds,
			new StringField
				.Builder("state")
				.setMaxLength(60)
				.build());

		assertField(insureds,
			new StringField
				.Builder("country")
				.setMaxLength(3)
				.setPattern("^(\\w{3}){1}$")
				.build());

		assertField(insureds,
			new StringField
				.Builder("address")
				.setMaxLength(60)
				.build());
	}

	private void assertIntermediaries(JsonObject intermediaries) {
		assertField(intermediaries,
			new StringField
				.Builder("type")
				.setMaxLength(23)
				.setEnums(INTERMEDIARIES_TYPE)
				.setOptional()
				.build());

		assertField(intermediaries,
			new StringField
				.Builder("identification")
				.setMaxLength(60)
				.setPattern("\\d{1,60}$")
				.build());

		assertField(intermediaries,
			new StringField
				.Builder("brokerId")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(intermediaries,
			new StringField
				.Builder("identificationType")
				.setMaxLength(6)
				.setEnums(IDENTIFICATION_TYPE)
				.build());

		assertField(intermediaries,
			new StringField
				.Builder("name")
				.setMaxLength(60)
				.build());

		assertField(intermediaries,
			new StringField
				.Builder("postCode")
				.setMaxLength(60)
				.setOptional()
				.build());

		assertField(intermediaries,
			new StringField
				.Builder("city")
				.setMaxLength(60)
				.setOptional()
				.build());

		assertField(intermediaries,
			new StringField
				.Builder("state")
				.setMaxLength(60)
				.setOptional()
				.build());

		assertField(intermediaries,
			new StringField
				.Builder("country")
				.setMaxLength(3)
				.setPattern("^(\\w{3}){1}$")
				.setOptional()
				.build());

		assertField(intermediaries,
			new StringField
				.Builder("address")
				.setMaxLength(60)
				.setOptional()
				.build());
	}

	private void assertInsuredObjects(JsonObject insuredObjects) {
		assertField(insuredObjects,
			new StringField
				.Builder("identification")
				.setMaxLength(100)
				.build());

		assertField(insuredObjects,
			new StringField
				.Builder("type")
				.setMaxLength(23)
				.setEnums(INSURED_OBJECTS_TYPE)
				.build());

		assertField(insuredObjects,
			new StringField
				.Builder("typeAdditionalInfo")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(insuredObjects,
			new StringField
				.Builder("description")
				.setMaxLength(1024)
				.build());

		assertField(insuredObjects,
			new ObjectField
				.Builder("amount")
				.setValidator(this::assertAmount)
				.build());

		assertField(insuredObjects,
			new ObjectArrayField
				.Builder("coverages")
				.setValidator(this::assertInsuredObjectsCoverages)
				.build());
	}

	private void assertSpecificInsuredObjects(JsonObject insuredObjects) {
		assertField(insuredObjects,
			new StringField
				.Builder("identification")
				.setMaxLength(100)
				.build());

		assertField(insuredObjects,
			new StringField
				.Builder("propertyType")
				.setMaxLength(22)
				.setEnums(PROPERTY_TYPE)
				.setOptional()
				.build());

		assertField(insuredObjects,
			new StringField
				.Builder("structuringType")
				.setMaxLength(21)
				.setEnums(STRUCTURING_TYPE)
				.setOptional()
				.build());

		assertField(insuredObjects,
			new StringField
				.Builder("postCode")
				.setMaxLength(60)
				.setOptional()
				.build());

		assertField(insuredObjects,
			new StringField
				.Builder("businessActivity")
				.setMaxLength(7)
				.setPattern("^\\d{7}$")
				.setOptional()
				.build());
	}

	private void assertInsuredObjectsCoverages(JsonObject coverages) {
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
			new StringField
				.Builder("internalCode")
				.setMaxLength(500)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("susepProcessNumber")
				.setMaxLength(50)
				.build());

		assertField(coverages,
			new ObjectField
				.Builder("LMI")
				.setValidator(this::assertAmount)
				.build());

		assertField(coverages,
			new BooleanField
				.Builder("isLMISublimit")
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("termStartDate")
				.setMaxLength(10)
				.build());

		assertField(coverages,
			new StringField
				.Builder("termEndDate")
				.setMaxLength(10)
				.build());

		assertField(coverages,
			new BooleanField
				.Builder("isMainCoverage")
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("feature")
				.setMaxLength(25)
				.setEnums(FEATURE)
				.build());

		assertField(coverages,
			new StringField
				.Builder("type")
				.setMaxLength(26)
				.setEnums(TYPE)
				.build());

		assertField(coverages,
			new IntField
				.Builder("gracePeriod")
				.setMaxLength(5)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("gracePeriodicity")
				.setMaxLength(3)
				.setEnums(GRACE_PERIODICITY)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("gracePeriodCountingMethod")
				.setMaxLength(13)
				.setEnums(GRACE_PERIOD_COUNTING_METHOD)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("gracePeriodStartDate")
				.setMaxLength(10)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("gracePeriodEndDate")
				.setMaxLength(10)
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
				.Builder("deductible")
				.setValidator(this::assertDeductible)
				.setOptional()
				.build());

		assertField(coverages,
			new ObjectField
				.Builder("POS")
				.setValidator(this::assertPOS)
				.setOptional()
				.build());
	}

	private void assertDeductible(JsonObject deductible) {
		assertField(deductible,
			new StringField
				.Builder("type")
				.setMaxLength(9)
				.setEnums(DEDUCTIBLE_TYPE)
				.build());

		assertField(deductible,
			new StringField
				.Builder("typeAdditionalInfo")
				.setMaxLength(500)
				.setOptional()
				.build());

		assertField(deductible,
			new ObjectField
				.Builder("amount")
				.setValidator(this::assertAmount)
				.build());

		assertField(deductible,
			new IntField
				.Builder("period")
				.setMaxLength(5)
				.build());

		assertField(deductible,
			new StringField
				.Builder("periodicity")
				.setMaxLength(3)
				.setEnums(GRACE_PERIODICITY)
				.build());

		assertField(deductible,
			new StringField
				.Builder("periodCountingMethod")
				.setMaxLength(13)
				.setOptional()
				.setEnums(GRACE_PERIOD_COUNTING_METHOD)
				.build());

		assertField(deductible,
			new StringField
				.Builder("periodStartDate")
				.build());

		assertField(deductible,
			new StringField
				.Builder("periodEndDate")
				.build());

		assertField(deductible,
			new StringField
				.Builder("description")
				.setMaxLength(60)
				.build());
	}

	private void assertPOS(JsonObject pOS) {
		assertField(pOS,
			new StringField
				.Builder("applicationType")
				.setMaxLength(10)
				.setEnums(APPLICATION_TYPE)
				.build());

		assertField(pOS,
			new StringField
				.Builder("description")
				.setMaxLength(60)
				.build());

		assertField(pOS,
			new ObjectField
				.Builder("minValue")
				.setValidator(this::assertAmount)
				.setOptional()
				.build());

		assertField(pOS,
			new ObjectField
				.Builder("maxValue")
				.setValidator(this::assertAmount)
				.setOptional()
				.build());

		assertField(pOS,
			new DoubleField
				.Builder("percentage")
				.setPattern("^\\d{1,3}\\.\\d{1,9}$")
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
