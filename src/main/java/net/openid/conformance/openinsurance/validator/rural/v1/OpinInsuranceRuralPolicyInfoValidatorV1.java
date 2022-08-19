package net.openid.conformance.openinsurance.validator.rural.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openinsurance.validator.OpenInsuranceLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.NumberField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api Source: swagger/openinsurance/rural/v1/swagger-insurance-rural-api.yaml
 * Api endpoint: /{policyId}/policy-info
 * Api version: 1.0.0
 */

@ApiName("Insurance Rural Policy Info V1")
public class OpinInsuranceRuralPolicyInfoValidatorV1 extends AbstractJsonAssertingCondition {
	private final OpenInsuranceLinksAndMetaValidator linksAndMetaValidator = new OpenInsuranceLinksAndMetaValidator(this);
	public static final Set<String> DOCUMENT_TYPE = SetUtils.createSet("APOLICE_INDIVIDUAL, BILHETE, CERTIFICADO, APOLICE_INDIVIDUAL_AUTOMOVEL, APOLICE_FROTA_AUTOMOVEL");
	public static final Set<String> ISSUANCE_TYPE = SetUtils.createSet("EMISSAO_PROPRIA, COSSEGURO_ACEITO");
	public static final Set<String> IDENTIFICATION_TYPE = SetUtils.createSet("CPF, CNPJ, OUTROS");
	public static final Set<String> TYPE = SetUtils.createSet("CORRETOR, REPRESENTANTE, ESTIPULANTE, CORRESPONDENTE, AGENTE_DE_MICROSSEGUROS, OUTROS");
	public static final Set<String> INSURED_TYPE = SetUtils.createSet("CONTRATO, PROCESSO_ADMINISTRATIVO, PROCESSO_JUDICIAL, AUTOMOVEL, CONDUTOR, OUTROS");
	public static final Set<String> CODE = SetUtils.createSet("GRANIZO, GEADA, GRANIZO_GEADA, GRANIZO_GEADA_CHUVA_EXCESSIVA, COMPREENSIVA, COMPREENSIVA_COM_DOENCAS_E_PRAGAS, CANCRO_CITRICO, COMPREENSIVA_PARA_A_MODALIDADE_BENFEITORIAS_E_PRODUTOS_AGROPECUARIO, COMPREENSIVA_PARA_A_MODALIDADE_PENHOR_RURAL, MORTE_DE_ANIMAIS, CONFINAMENTO_SEMI_CONFINAMENTO_BOVINOS_DE_CORTE, CONFINAMENTO_BOVINOS_DE_LEITE, VIAGEM, EXPOSICAO_MOSTRA_E_LEILAO, CARREIRA, SALTO_E_ADESTRAMENTO, PROVAS_FUNCIONAIS, HIPISMO_RURAL, POLO, TROTE, VAQUEJADA, EXTENSAO_DE_COBERTURA_EM_TERRITORIO_ESTRANGEIRO, TRANSPORTE, RESPONSABILIDADE_CIVIL, PERDA_DE_FERTILIDADE_DE_GARANHAO, REEMBOLSO_CIRURGICO, COLETA_DE_SEMEN, PREMUNICAO, COMPREENSIVA_PARA_A_MODALIDADE_FLORESTAS, VIDA_DO_PRODUTOR_RURAL, BASICA_DE_FATURAMENTO_PECUARIO, OUTRAS");
	public static final Set<String> FEATURE = SetUtils.createSet("MASSIFICADOS, MASSIFICADOS_MICROSEGUROS, GRANDES_RISCOS");
	public static final Set<String> COVERAGE_TYPE = SetUtils.createSet("PARAMETRICO, INTERMITENTE, REGULAR_COMUM, CAPITAL_GLOBAL, PARAMETRICO_E_INTERMITENTE");
	public static final Set<String> GRACE_PERIODICITY = SetUtils.createSet("DIA, MES, ANO");
	public static final Set<String> GRACE_PERIOD_COUNTING_METHOD = SetUtils.createSet("DIAS_UTEIS, DIAS_CORRIDOS");
	public static final Set<String> DEDUCTIBLE_TYPE = SetUtils.createSet("REDUZIDA, NORMAL, MAJORADA, DEDUTIVEL, OUTROS");
	public static final Set<String> APPLICATION_TYPE = SetUtils.createSet("VALOR, PERCENTUAL, OUTROS");
	public static final Set<String> SUBVENTION_TYPE = SetUtils.createSet("AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA, PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO, BR, XX");
	public static final Set<String> UNIT_MEASURE = SetUtils.createSet("HECTAR, METRO_QUADRADO, OUTROS");
	public static final Set<String> FLOCK_CODE = SetUtils.createSet("BOVINOS, EQUINOS, OVINOS, SUINOS, CAPRINOS, AVES, BUBALINOS, OUTROS");
	public static final Set<String> FOREST_CODE = SetUtils.createSet("PINUS, EUCALIPTO, TECA, SERINGUEIRA, OUTROS");
	public static final Set<String> COUNTRY_SUB_DEVISION = SetUtils.createSet("AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA, PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO");
	public static final Set<String> MODEL_TYPE = SetUtils.createSet("CLIMATICOS, OUTROS");
	public static final Set<String> ANIMAL_DESTINATION = SetUtils.createSet("CONSUMO, PRODUCAO, REPRODUCAO");
	public static final Set<String> ANIMAL_TYPE = SetUtils.createSet("ELITE, DOMESTICO, SEGURANCA");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectField
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
				.setValidator(this::assertDataCoverages)
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
			new ObjectArrayField.Builder("coinsurers")
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
			new ObjectField.Builder("branchInfo")
				.setValidator(branchInfo -> assertField(branchInfo,
					new ObjectArrayField
						.Builder("insuredObjects")
						.setValidator(this::assertInsuranceRuralSpecificInsuredObject)
						.setOptional()
						.build()))
				.setOptional()
				.build());

	}

	private void assertInsuranceRuralSpecificInsuredObject(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("identification")
				.setMaxLength(100)
				.build());

		assertField(data,
			new BooleanField
				.Builder("isFESRParticipant")
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("subventionAmount")
				.setValidator(this::assertAmount)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("subventionType")
				.setEnums(SUBVENTION_TYPE)
				.setOptional()
				.setMaxLength(2)
				.build());

		assertField(data,
			new DoubleField
				.Builder("safeArea")
				.setPattern("^\\d{1,18}\\.\\d{2}$")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("unitMeasure")
				.setEnums(UNIT_MEASURE)
				.setMaxLength(6)
				.setOptional()
				.build());

		assertField(data,
			new NumberField
				.Builder("cultureCode")
				.setMaxLength(8)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("flockCode")
				.setEnums(FLOCK_CODE)
				.setMaxLength(9)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("forestCode")
				.setEnums(FOREST_CODE)
				.setMaxLength(11)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("surveyDate")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("surveyAddress")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("surveyCountrySubDivision")
				.setMaxLength(2)
				.setEnums(COUNTRY_SUB_DEVISION)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("surveyPostCode")
				.setMaxLength(30)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("surveyCountryCode")
				.setMaxLength(3)
				.setPattern("^(\\w{3}){1}$")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("surveyorIdType")
				.setMaxLength(6)
				.setEnums(IDENTIFICATION_TYPE)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("surveyorId")
				.setMaxLength(60)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("surveyorName")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("modelType")
				.setMaxLength(10)
				.setEnums(MODEL_TYPE)
				.setOptional()
				.build());

		assertField(data,
			new BooleanField
				.Builder("areAssetsCovered")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("coveredAnimalDestination")
				.setMaxLength(10)
				.setEnums(ANIMAL_DESTINATION)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("animalType")
				.setMaxLength(9)
				.setEnums(ANIMAL_TYPE)
				.setOptional()
				.build());
	}

	private void assertDataCoverages(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("branch")
				.setMaxLength(4)
				.build());

		assertField(data,
			new StringField
				.Builder("code")
				.setMaxLength(67)
				.setEnums(CODE)
				.build());

		assertField(data,
			new StringField
				.Builder("description")
				.setMaxLength(500)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("deductible")
				.setValidator(this::assertDeductible)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField.Builder("POS")
				.setValidator(pos -> {
					assertField(pos,
						new StringField
							.Builder("applicationType")
							.setEnums(APPLICATION_TYPE)
							.setMaxLength(10)
							.build());

					assertField(pos,
						new StringField
							.Builder("description")
							.setMaxLength(60)
							.build());

					assertField(pos,
						new ObjectField
							.Builder("minValue")
							.setValidator(this::assertAmount)
							.setOptional()
							.build());

					assertField(pos,
						new ObjectField
							.Builder("maxValue")
							.setValidator(this::assertAmount)
							.setOptional()
							.build());

					assertField(pos,
						new DoubleField
							.Builder("percentage")
							.setPattern("^\\d{1,3}\\.\\d{1,9}$")
							.setOptional()
							.build());
				})
				.setOptional()
				.build());
	}

	private void assertDeductible(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("type")
				.setMaxLength(9)
				.setEnums(DEDUCTIBLE_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("typeAdditionalInfo")
				.setMaxLength(500)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("amount")
				.setValidator(this::assertAmount)
				.build());

		assertField(data,
			new IntField
				.Builder("period")
				.setMaxLength(5)
				.build());

		assertField(data,
			new StringField
				.Builder("periodicity")
				.setMaxLength(3)
				.setEnums(GRACE_PERIODICITY)
				.build());

		assertField(data,
			new StringField
				.Builder("periodCountingMethod")
				.setMaxLength(13)
				.setEnums(GRACE_PERIOD_COUNTING_METHOD)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("periodStartDate")
				.setMaxLength(10)
				.build());

		assertField(data,
			new StringField
				.Builder("periodEndDate")
				.setMaxLength(10)
				.build());

		assertField(data,
			new StringField
				.Builder("description")
				.setMaxLength(60)
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

	private void assertInsuredObjects(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("identification")
				.setMaxLength(100)
				.build());

		assertField(data,
			new StringField
				.Builder("type")
				.setMaxLength(23)
				.setEnums(INSURED_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("typeAdditionalInfo")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("description")
				.setMaxLength(100)
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
	}

	private void assertCoverages(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("branch")
				.setMaxLength(4)
				.build());

		assertField(data,
			new StringField
				.Builder("code")
				.setMaxLength(67)
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
				.Builder("internalCode")
				.setMaxLength(500)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("susepProcessNumber")
				.setMaxLength(50)
				.build());

		assertField(data,
			new ObjectField
				.Builder("LMI")
				.setValidator(this::assertAmount)
				.build());

		assertField(data,
			new BooleanField
				.Builder("isLMISublimit")
				.setOptional()
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
			new BooleanField
				.Builder("isMainCoverage")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("feature")
				.setMaxLength(25)
				.setEnums(FEATURE)
				.build());

		assertField(data,
			new StringField
				.Builder("type")
				.setMaxLength(26)
				.setEnums(COVERAGE_TYPE)
				.build());

		assertField(data,
			new IntField
				.Builder("gracePeriod")
				.setMaxLength(5)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("gracePeriodicity")
				.setMaxLength(3)
				.setEnums(GRACE_PERIODICITY)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("gracePeriodCountingMethod")
				.setMaxLength(13)
				.setEnums(GRACE_PERIOD_COUNTING_METHOD)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("gracePeriodStartDate")
				.setMaxLength(10)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("gracePeriodEndDate")
				.setMaxLength(10)
				.setOptional()
				.build());
	}

	private void assertIntermediaries(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("type")
				.setMaxLength(23)
				.setEnums(TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("identification")
				.setMaxLength(60)
				.setPattern("^\\d{1,60}$")
				.build());

		assertField(data,
			new StringField
				.Builder("brokerId")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("identificationType")
				.setMaxLength(6)
				.setEnums(IDENTIFICATION_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("name")
				.setMaxLength(60)
				.build());

		assertField(data,
			new StringField
				.Builder("postCode")
				.setMaxLength(60)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("city")
				.setMaxLength(60)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("state")
				.setMaxLength(60)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("country")
				.setMaxLength(3)
				.setPattern("^(\\w{3}){1}$")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("address")
				.setMaxLength(60)
				.build());
	}

	private void assertPersonalInfo(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("identification")
				.setMaxLength(60)
				.build());

		assertField(data,
			new StringField
				.Builder("identificationType")
				.setMaxLength(6)
				.setEnums(IDENTIFICATION_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("name")
				.setMaxLength(60)
				.build());

		assertField(data,
			new StringField
				.Builder("postCode")
				.setMaxLength(60)
				.build());

		assertField(data,
			new StringField
				.Builder("email")
				.setMaxLength(256)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("city")
				.setMaxLength(60)
				.build());

		assertField(data,
			new StringField
				.Builder("state")
				.setMaxLength(60)
				.build());

		assertField(data,
			new StringField
				.Builder("country")
				.setMaxLength(3)
				.setPattern("^(\\w{3}){1}$")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("address")
				.setMaxLength(60)
				.build());
	}
}
