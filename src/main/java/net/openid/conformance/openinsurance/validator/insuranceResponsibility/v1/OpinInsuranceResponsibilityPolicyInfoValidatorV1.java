package net.openid.conformance.openinsurance.validator.insuranceResponsibility.v1;

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
 * Api Source: swagger/openinsurance/insuranceResponsibility/v1/swagger-insurance-responsibility.yaml
 * Api endpoint: /{policyId}/policy-info
 * Api version: 1.0.0
 */

@ApiName("Insurance Responsibility PolicyInfo V1")
public class OpinInsuranceResponsibilityPolicyInfoValidatorV1 extends AbstractJsonAssertingCondition {

	public static final Set<String> DOCUMENT_TYPE = SetUtils.createSet("APOLICE_INDIVIDUAL, BILHETE, CERTIFICADO, APOLICE_INDIVIDUAL_AUTOMOVEL, APOLICE_FROTA_AUTOMOVEL");
	public static final Set<String> ISSUANCE_TYPE = SetUtils.createSet("EMISSAO_PROPRIA, COSSEGURO_ACEITO");
	public static final Set<String> IDENTIFICATION_TYPE = SetUtils.createSet("CPF, CNPJ, OUTROS");
	public static final Set<String> INTERMEDIARIES_TYPE = SetUtils.createSet("CORRETOR, REPRESENTANTE, ESTIPULANTE, CORRESPONDENTE, AGENTE_DE_MICROSSEGUROS, OUTROS");
	public static final Set<String> INSURED_OBJECTS_TYPE = SetUtils.createSet("CONTRATO, PROCESSO_ADMINISTRATIVO, PROCESSO_JUDICIAL, AUTOMOVEL, CONDUTOR, OUTROS");
	public static final Set<String> GRACE_PERIODICITY = SetUtils.createSet("DIA, MES, ANO");
	public static final Set<String> GRACE_PERIOD_COUNTING_METHOD = SetUtils.createSet("DIAS_UTEIS, DIAS_CORRIDOS");
	public static final Set<String> CODE = SetUtils.createSet("DANOS_CAUSADOS_A_TERCEIROS, INSTALACOES_FIXAS, TRANSPORTE_AMBIENTAL, OBRAS_E_PRESTACAO_DE_SERVICO, ALAGAMENTO_E_OU_INUNDACAO, ANUNCIOS_E_ANTENAS, ASSISTENCIAS_TECNICAS_E_MECANICAS, CONDOMINIOS_PROPRIETARIOS_E_LOCATARIOS_DE_IMOVEIS, CUSTOS_DE_DEFESA_DO_SEGURADO, DANOS_CAUSADOS_POR_FALHAS_DE_PROFISSIONAL_DA_AREA_MEDICA, DANOS_CAUSADOS_POR_FOGOS_DE_ARTIFICIO, DANOS_ESTETICOS, DANOS_MORAIS, DESPESAS_EMERGENCIAIS_DESPESAS_DE_CONTENCAO_E_DESPESAS_DE_SALVAMENTO_DE_SINISTRO, EMPREGADOR_EMPREGADOS, EMPRESAS_DE_SERVICOS, EQUIPAMENTOS_DE_TERCEIROS_OPERADOS_PELO_SEGURADO, ERRO_DE_PROJETO, EXCURSOES_EVENTOS_EXPOSICOES_E_ATIVIDADES, FAMILIAR, FINANCEIRO, FORO, INDUSTRIA_E_COMERCIO, LOCAIS_E_OU_ESTABELECIMENTOS_DE_QUALQUER_NATUREZA, OBRAS, OPERACOES_DE_QUALQUER_NATUREZA, POLUICAO, PRESTACAO_DE_SERVICOS, PRODUTOS, RECALL, RECLAMACOES_DECORRENTES_DO_FORNECIMENTO_DE_COMESTIVEIS_OU_BEBIDAS, SINDICO, TELEFERICOS_E_SIMILARES, TRANSPORTE_DE_BENS_OU_PESSOAS, VEICULOS_EMBARCACOES_BENS_E_MERCADORIAS, RESPONSABILIZACAO_CIVIL_VINCULADA_A_PRESTACAO_DE_SERVICOS_PROFISSIONAIS_OBJETO_DA_ATIVIDADE_DO_SEGURADO, RESPONSABILIDADE_CIVIL_PERANTE_TERCEIROS, PERDAS_DIRETAS_AO_SEGURADO, GERENCIAMENTO_DE_CRISE, OUTRAS");
	public static final Set<String> FEATURE = SetUtils.createSet("MASSIFICADOS, MASSIFICADOS_MICROSEGUROS, GRANDES_RISCOS");
	public static final Set<String> TYPE = SetUtils.createSet("PARAMETRICO, INTERMITENTE, REGULAR_COMUM, CAPITAL_GLOBAL, PARAMETRICO_E_INTERMITENTE");
	public static final Set<String> DEDUCTIBLE_TYPE = SetUtils.createSet("REDUZIDA, NORMAL, MAJORADA, DEDUTIVEL, OUTROS");
	public static final Set<String> APPLICATION_TYPE = SetUtils.createSet("VALOR, PERCENTUAL, OUTROS");
	public static final Set<String> COVERAGE_TYPE = SetUtils.createSet("POR_OCORRENCIA, POR_RECLAMACAO, OUTRA");
	public static final Set<String> WORKING_DAYS_INDEX = SetUtils.createSet("DIAS_UTEIS, DIAS_CORRIDOS");
	public static final Set<String> LAWYER_RECOMMENDATION = SetUtils.createSet("ESCOLHIDO_PELO_SEGURADO, OUTROS");
	public static final Set<String> PROFESSIONAL_CLASS = SetUtils.createSet("ADMINISTRADOR_IMOBILIARIO, ESCRITORIOS_DE_ADVOCACIA, CERTIFICACAO_DIGITAL, CERTIFICACAO_DE_PRODUTOS_SISTEMAS_PROCESSOS_OU_SERVICOS, DESPACHANTE_ADUANEIRO_AGENTE_EMBARCADOR_LICENCIADOR_E_SIMILARES, CORRETORES_DE_RESSEGURO, CORRETORES_DE_SEGUROS, EMPRESAS_DE_TECNOLOGIA, EMPRESAS_DE_ENGENHARIA_E_ARQUITETURA, HOSPITAIS_CLINICAS_MEDICAS_ODONTOLOGICAS_LABORATORIOS_E_EMPRESAS_DE_DIAGNOSTICOS, NOTARIOS_E_OU_REGISTRADORES, INSTITUICOES_FINANCEIRAS, HOSPITAIS_CLINICAS_LABORATORIOS_EMPRESAS_DE_DIAGNOSTICOS_VETERINARIOS, MEDICOS_VETERINARIOS, OUTROS");
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
						new ObjectArrayField
							.Builder("coverages")
							.setValidator(this::assertSpecificCoverages)
							.build());

					assertField(branchInfo,
						new ObjectArrayField
							.Builder("insuredObjects")
							.setValidator(this::assertSpecificInsuredObjects)
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

	private void assertInsuredObjectsCoverages(JsonObject coverages) {
		assertField(coverages,
			new StringField
				.Builder("branch")
				.setMaxLength(4)
				.build());

		assertField(coverages,
			new StringField
				.Builder("code")
				.setMaxLength(103)
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
				.setMaxLength(103)
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

	private void assertSpecificCoverages(JsonObject coverages) {
		assertField(coverages,
			new StringField
				.Builder("branch")
				.setMaxLength(4)
				.build());

		assertField(coverages,
			new StringField
				.Builder("code")
				.setMaxLength(103)
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
				.Builder("type")
				.setMaxLength(14)
				.setEnums(COVERAGE_TYPE)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("typeDescription")
				.setMaxLength(500)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("retroactivityDate")
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("retroactivityPeriod")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("retroactivityTimeUnit")
				.setMaxLength(3)
				.setEnums(GRACE_PERIODICITY)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("retroactivityWorkingDaysIndex")
				.setMaxLength(13)
				.setEnums(WORKING_DAYS_INDEX)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("complementaryTermStartDate")
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("complementaryTermEndDate")
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("complementaryTerm")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("complementaryTermTimeUnit")
				.setMaxLength(3)
				.setEnums(GRACE_PERIODICITY)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("complementaryTermWorkingDaysIndex")
				.setMaxLength(13)
				.setEnums(WORKING_DAYS_INDEX)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("supplementaryTermStartDate")
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("supplementaryTermEndDate")
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("supplementaryTerm")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("supplementaryTermTimeUnit")
				.setMaxLength(3)
				.setEnums(GRACE_PERIODICITY)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("supplementaryTermWorkingDaysIndex")
				.setMaxLength(13)
				.setEnums(WORKING_DAYS_INDEX)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("lawyerRecommendation")
				.setMaxLength(23)
				.setEnums(LAWYER_RECOMMENDATION)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("lawyerRecommendationDescription")
				.setMaxLength(500)
				.setOptional()
				.build());
	}

	private void assertSpecificInsuredObjects(JsonObject insured) {
		assertField(insured,
			new StringField
				.Builder("identification")
				.setMaxLength(100)
				.build());

		assertField(insured,
			new BooleanField
				.Builder("hasComplementaryContract")
				.setOptional()
				.build());

		assertField(insured,
			new ObjectField
				.Builder("complementaryContractAmount")
				.setValidator(this::assertAmount)
				.setOptional()
				.build());

		assertField(insured,
			new ObjectField
				.Builder("coveragesMaxAmount")
				.setValidator(this::assertAmount)
				.setOptional()
				.build());

		assertField(insured,
			new StringField
				.Builder("coveragesTermStartDate")
				.setOptional()
				.build());

		assertField(insured,
			new StringField
				.Builder("coveragesTermEndDate")
				.setOptional()
				.build());

		assertField(insured,
			new IntField
				.Builder("coveragesTerm")
				.setMaxLength(5)
				.setOptional()
				.build());

		assertField(insured,
			new StringField
				.Builder("coveragesUnit")
				.setMaxLength(3)
				.setEnums(GRACE_PERIODICITY)
				.setOptional()
				.build());

		assertField(insured,
			new StringField
				.Builder("coveragesWorkingDaysIndex")
				.setMaxLength(13)
				.setEnums(WORKING_DAYS_INDEX)
				.setOptional()
				.build());

		assertField(insured,
			new BooleanField
				.Builder("hasTransportationPollutionDamage")
				.setOptional()
				.build());

		assertField(insured,
			new BooleanField
				.Builder("hasThirdPatyDamage")
				.setOptional()
				.build());

		assertField(insured,
			new StringField
				.Builder("professionalClass")
				.setMaxLength(80)
				.setEnums(PROFESSIONAL_CLASS)
				.setOptional()
				.build());

		assertField(insured,
			new BooleanField
				.Builder("hasRetroactivityApplication")
				.setOptional()
				.build());
	}
}
