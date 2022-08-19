package net.openid.conformance.openinsurance.validator.insuranceAuto.v1;

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
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api Source: swagger/openinsurance/insuranceAuto/v1/swagger-insurance-auto-api.yaml
 * Api endpoint: /{policyId}/policy-info
 * Api version: 1.0.0
 */

@ApiName("Insurance Auto PolicyInfo V1")
public class OpinInsuranceAutoPolicyInfoValidatorV1 extends AbstractJsonAssertingCondition {

	public static final Set<String> DOCUMENT_TYPE = SetUtils.createSet("APOLICE_INDIVIDUAL, BILHETE, CERTIFICADO, APOLICE_INDIVIDUAL_AUTOMOVEL, APOLICE_FROTA_AUTOMOVEL");
	public static final Set<String> ISSUANCE_TYPE = SetUtils.createSet("EMISSAO_PROPRIA, COSSEGURO_ACEITO");
	public static final Set<String> IDENTIFICATION_TYPE = SetUtils.createSet("CPF, CNPJ, OUTROS");
	public static final Set<String> INTERMEDIARIES_TYPE = SetUtils.createSet("CORRETOR, REPRESENTANTE, ESTIPULANTE, CORRESPONDENTE, AGENTE_DE_MICROSSEGUROS, OUTROS");
	public static final Set<String> INSURED_OBJECTS_TYPE = SetUtils.createSet("CONTRATO, PROCESSO_ADMINISTRATIVO, PROCESSO_JUDICIAL, AUTOMOVEL, CONDUTOR, OUTROS");
	public static final Set<String> MODALITY = SetUtils.createSet("VALOR_DE_MERCADO_REFERENCIADO, VALOR_DETERMINADO, CRITERIO_DIVERSO, OUTROS");
	public static final Set<String> AMOUNT_REFERENCE_TABLE = SetUtils.createSet("MOLICAR, FIPE, JORNAL_DO_CARRO, VD, OUTRAS");
	public static final Set<String> FARE_CATEGORY = SetUtils.createSet("10, 11, 14A, 14B, 14C, 15, 16, 17, 18, 19, 20, 21, 22, 23, 30, 31, 40, 41, 42, 43, 50, 51, 52, 53, 58, 59, 60, 61, 62, 63, 68, 69, 70, 71, 72, 73, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97");
	public static final Set<String> VEHICLE_USAGE = SetUtils.createSet("LAZER, LOCOMOCAO_DIARIA, EXERCICIO_DO_TRABALHO, OUTROS");
	public static final Set<String> GRACE_PERIODICITY = SetUtils.createSet("DIA, MES, ANO");
	public static final Set<String> GRACE_PERIOD_COUNTING_METHOD = SetUtils.createSet("DIAS_UTEIS, DIAS_CORRIDOS");
	public static final Set<String> CODE = SetUtils.createSet("CASCO_COMPREENSIVA, CASCO_INCENDIO_ROUBO_E_FURTO, CASCO_ROUBO_E_FURTO, CASCO_INCENDIO, CASCO_ALAGAMENTO, CASCO_COLISAO_INDENIZACAO_PARCIAL, CASCO_COLISAO_INDENIZACAO_INTEGRAL, RESPONSABILIDADE_CIVIL_FACULTATIVA_DE_VEICULOS_RCFV, RESPONSABILIDADE_CIVIL_FACULTATIVA_DO_CONDUTOR_RCFC, ACIDENTE_PESSOAIS_DE_PASSAGEIROS_APP_VEICULO, ACIDENTE_PESSOAIS_DE_PASSAGEIROS_APP_CONDUTOR, VIDROS, DIARIA_POR_INDISPONIBILIDADE, LFR_LANTERNAS_FAROIS_E_RETROVISORES, ACESSORIOS_E_EQUIPAMENTOS, CARRO_RESERVA, PEQUENOS_REPAROS, RESPONSABILIDADE_CIVIL_CARTA_VERDE, OUTRAS");
	public static final Set<String> FEATURE = SetUtils.createSet("MASSIFICADOS, MASSIFICADOS_MICROSEGUROS, GRANDES_RISCOS");
	public static final Set<String> TYPE = SetUtils.createSet("PARAMETRICO, INTERMITENTE, REGULAR_COMUM, CAPITAL_GLOBAL, PARAMETRICO_E_INTERMITENTE");
	public static final Set<String> DEDUCTIBLE_TYPE = SetUtils.createSet("REDUZIDA, NORMAL, MAJORADA, DEDUTIVEL, OUTROS");
	public static final Set<String> COMPENSATION_TYPE = SetUtils.createSet("INTEGRAL, PARCIAL, OUTROS");
	public static final Set<String> BOUND_COVERAGE = SetUtils.createSet("VEICULO, CONDUTOR, OUTROS");
	public static final Set<String> APPLICATION_TYPE = SetUtils.createSet("VALOR, PERCENTUAL, OUTROS");
	public static final Set<String> REPAIR_NETWORK = SetUtils.createSet("LIVRE_ESCOLHA, REDE_REFERENCIADA, AMBAS, OUTROS");
	public static final Set<String> REPAIRED_PARTS_USAGE_TYPE = SetUtils.createSet("NOVA, USADA, NOVA_E_USADA");
	public static final Set<String> REPAIRED_PARTS_CLASSIFICATION = SetUtils.createSet("ORIGINAL, COMPATIVEL, ORIGINAL_E_COMPATIVEL");
	public static final Set<String> REPAIRED_PARTS_NATIONALITY = SetUtils.createSet("NACIONAL, IMPORTADA, NACIONAL_E_IMPORTADA");
	public static final Set<String> VALIDITY_TYPE = SetUtils.createSet("ANUAL, ANUAL_INTERMITENTE, PLURIANUAL, PLURIANUAL_INTERMITENTE, SEMESTRAL, SEMESTRAL_INTERMITENTE, MENSAL, MENSAL_INTERMITENTE, DIARIO, DIARIO_INTERMITENTE, OUTROS");
	public static final Set<String> OTHER_BENEFITS = SetUtils.createSet("SORTEIO_GRATUITO, CLUBE_DE_BENEFICIOS, CASH_BACK, DESCONTOS, CUSTOMIZAVEL");
	public static final Set<String> ASSISTANCE_PACKAGES = SetUtils.createSet("ATE_DEZ_SERVICOS, ATE_VINTE_SERVICOS, ACIMA_DE_VINTE_SERVICOS, CUSTOMIZAVEL");
	public static final Set<String> SEX = SetUtils.createSet("MASCULINO, FEMININO, NAO_DECLARADO, OUTROS");
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
				.setValidator(maxLMG -> {
					assertField(maxLMG,
						new DoubleField
							.Builder("amount")
							.setPattern("^\\d{1,16}\\.\\d{2}$")
							.build());

					assertField(maxLMG,
						new StringField
							.Builder("currency")
							.setPattern("^(\\w{3}){1}$")
							.build());
				})
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
			new StringField
				.Builder("repairNetwork")
				.setMaxLength(17)
				.setEnums(REPAIR_NETWORK)
				.build());

		assertField(data,
			new StringField
				.Builder("repairedPartsUsageType")
				.setMaxLength(12)
				.setEnums(REPAIRED_PARTS_USAGE_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("repairedPartsClassification")
				.setMaxLength(21)
				.setEnums(REPAIRED_PARTS_CLASSIFICATION)
				.build());

		assertField(data,
			new StringField
				.Builder("repairedPartsNationality")
				.setMaxLength(20)
				.setEnums(REPAIRED_PARTS_NATIONALITY)
				.build());

		assertField(data,
			new StringField
				.Builder("validityType")
				.setMaxLength(23)
				.setEnums(VALIDITY_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("otherCompensations")
				.setMaxLength(500)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("otherBenefits")
				.setMaxLength(19)
				.setEnums(OTHER_BENEFITS)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("assistancePackages")
				.setMaxLength(23)
				.setEnums(ASSISTANCE_PACKAGES)
				.setOptional()
				.build());

		assertField(data,
			new BooleanField
				.Builder("isExpiredRiskPolicy")
				.setOptional()
				.build());

		assertField(data,
			new DoubleField
				.Builder("bonusDiscountRate")
				.setPattern("^\\d{1,16}\\.\\d{2}$")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("bonusClass")
				.setMaxLength(500)
				.setOptional()
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("drivers")
				.setValidator(drivers -> {
					assertField(drivers,
						new StringField
							.Builder("identification")
							.setMaxLength(60)
							.build());

					assertField(drivers,
						new StringField
							.Builder("sex")
							.setMaxLength(13)
							.setEnums(SEX)
							.setOptional()
							.build());

					assertField(drivers,
						new StringField
							.Builder("birthDate")
							.setOptional()
							.build());

					assertField(drivers,
						new IntField
							.Builder("licensedExperience")
							.setMaxLength(3)
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
				.setMaxLength(100)
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
			new BooleanField
				.Builder("hasExactVehicleIdentification")
				.setOptional()
				.build());

		assertField(insuredObjects,
			new StringField
				.Builder("modality")
				.setOptional()
				.setMaxLength(29)
				.setEnums(MODALITY)
				.build());

		assertField(insuredObjects,
			new StringField
				.Builder("amountReferenceTable")
				.setMaxLength(15)
				.setEnums(AMOUNT_REFERENCE_TABLE)
				.setOptional()
				.build());

		assertField(insuredObjects,
			new StringField
				.Builder("model")
				.setMaxLength(8)
				.setOptional()
				.build());

		assertField(insuredObjects,
			new StringField
				.Builder("year")
				.setMaxLength(4)
				.setPattern("^\\d{4}$")
				.setOptional()
				.build());

		assertField(insuredObjects,
			new StringField
				.Builder("fareCategory")
				.setMaxLength(3)
				.setEnums(FARE_CATEGORY)
				.setOptional()
				.build());

		assertField(insuredObjects,
			new StringField
				.Builder("riskPostCode")
				.setMaxLength(60)
				.setOptional()
				.build());

		assertField(insuredObjects,
			new StringField
				.Builder("vehicleUsage")
				.setEnums(VEHICLE_USAGE)
				.setMaxLength(21)
				.setOptional()
				.build());

		assertField(insuredObjects,
			new StringField
				.Builder("frequentDestinationPostCode")
				.setMaxLength(60)
				.setOptional()
				.build());

		assertField(insuredObjects,
			new StringField
				.Builder("overnightPostCode")
				.setMaxLength(60)
				.setOptional()
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
				.setMaxLength(51)
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
				.setMaxLength(500)
				.build());

		assertField(coverages,
			new ObjectField
				.Builder("LMI")
				.setValidator(lmi -> {
					assertField(lmi,
						new DoubleField
							.Builder("amount")
							.setPattern("^\\d{1,16}\\.\\d{2}$")
							.build());

					assertField(lmi,
						new StringField
							.Builder("currency")
							.setPattern("^(\\w{3}){1}$")
							.build());
				})
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

		assertField(coverages,
			new DoubleField
				.Builder("adjustmentRate")
				.setPattern("^\\d{1,3}\\.\\d{1,9}$")
				.setOptional()
				.build());

		assertField(coverages,
			new ObjectField
				.Builder("premiumAmount")
				.setValidator(premiumAmount -> {
					assertField(premiumAmount,
						new DoubleField
							.Builder("amount")
							.setPattern("^\\d{1,16}\\.\\d{2}$")
							.build());

					assertField(premiumAmount,
						new StringField
							.Builder("currency")
							.setPattern("^(\\w{3}){1}$")
							.build());
				})
				.build());

		assertField(coverages,
			new StringField
				.Builder("compensationType")
				.setMaxLength(8)
				.setEnums(COMPENSATION_TYPE)
				.setOptional()
				.build());

		assertField(coverages,
			new DoubleField
				.Builder("partialCompensationPercentage")
				.setOptional()
				.setPattern("^\\d{1,3}\\.\\d{1,9}$")
				.build());

		assertField(coverages,
			new DoubleField
				.Builder("percentageOverLMI")
				.setOptional()
				.setPattern("^\\d{1,3}\\.\\d{1,9}$")
				.build());

		assertField(coverages,
			new IntField
				.Builder("daysForTotalCompensation")
				.setMaxLength(4)
				.setOptional()
				.build());

		assertField(coverages,
			new StringField
				.Builder("boundCoverage")
				.setMaxLength(8)
				.setEnums(BOUND_COVERAGE)
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
				.setMaxLength(51)
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
				.setValidator(amount -> {
					assertField(amount,
						new DoubleField
							.Builder("amount")
							.setPattern("^\\d{1,16}\\.\\d{2}$")
							.build());

					assertField(amount,
						new StringField
							.Builder("currency")
							.setPattern("^(\\w{3}){1}$")
							.build());
				})
				.setOptional()
				.build());

		assertField(deductible,
			new IntField
				.Builder("period")
				.setMaxLength(5)
				.setOptional()
				.build());

		assertField(deductible,
			new StringField
				.Builder("periodicity")
				.setMaxLength(3)
				.setEnums(GRACE_PERIODICITY)
				.setOptional()
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
				.setOptional()
				.build());

		assertField(deductible,
			new StringField
				.Builder("periodEndDate")
				.setOptional()
				.build());

		assertField(deductible,
			new StringField
				.Builder("description")
				.setMaxLength(60)
				.setOptional()
				.build());

		assertField(deductible,
			new BooleanField
				.Builder("hasDeductibleOverTotalCompensation")
				.setOptional()
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
				.setOptional()
				.build());

		assertField(pOS,
			new ObjectField
				.Builder("minValue")
				.setValidator(minValue -> {
					assertField(minValue,
						new DoubleField
							.Builder("amount")
							.setPattern("^\\d{1,16}\\.\\d{2}$")
							.build());

					assertField(minValue,
						new StringField
							.Builder("currency")
							.setPattern("^(\\w{3}){1}$")
							.build());
				})
				.setOptional()
				.build());

		assertField(pOS,
			new ObjectField
				.Builder("maxValue")
				.setValidator(maxValue -> {
					assertField(maxValue,
						new DoubleField
							.Builder("amount")
							.setPattern("^\\d{1,16}\\.\\d{2}$")
							.build());

					assertField(maxValue,
						new StringField
							.Builder("currency")
							.setPattern("^(\\w{3}){1}$")
							.build());
				})
				.setOptional()
				.build());

		assertField(pOS,
			new DoubleField
				.Builder("percentage")
				.setPattern("^\\d{1,3}\\.\\d{1,9}$")
				.setOptional()
				.build());
	}
}
