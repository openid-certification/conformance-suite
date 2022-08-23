package net.openid.conformance.openinsurance.validator.insuranceAviation.v1;

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
 * Api Source: swagger/openinsurance/insuranceAviation/v1/swagger-insurance-aviation.yaml
 * Api endpoint: /{policyId}/policy-info
 * Api version: 1.0.0
 */

@ApiName("Insurance Aviation PolicyInfo V1")
public class OpinInsuranceAviationPolicyInfoValidatorV1 extends AbstractJsonAssertingCondition {

	public static final Set<String> DOCUMENT_TYPE = SetUtils.createSet("APOLICE_INDIVIDUAL, BILHETE, CERTIFICADO, APOLICE_INDIVIDUAL_AUTOMOVEL, APOLICE_FROTA_AUTOMOVEL");
	public static final Set<String> ISSUANCE_TYPE = SetUtils.createSet("EMISSAO_PROPRIA, COSSEGURO_ACEITO");
	public static final Set<String> IDENTIFICATION_TYPE = SetUtils.createSet("CPF, CNPJ, OUTROS");
	public static final Set<String> INTERMEDIARIES_TYPE = SetUtils.createSet("CORRETOR, REPRESENTANTE, ESTIPULANTE, CORRESPONDENTE, AGENTE_DE_MICROSSEGUROS, OUTROS");
	public static final Set<String> INSURED_OBJECTS_TYPE = SetUtils.createSet("CONTRATO, PROCESSO_ADMINISTRATIVO, PROCESSO_JUDICIAL, AUTOMOVEL, CONDUTOR, OUTROS");
	public static final Set<String> GRACE_PERIODICITY = SetUtils.createSet("DIA, MES, ANO");
	public static final Set<String> GRACE_PERIOD_COUNTING_METHOD = SetUtils.createSet("DIAS_UTEIS, DIAS_CORRIDOS");
	public static final Set<String> CODE = SetUtils.createSet("CASCO, RESPONSABILIDADE_CIVIL_FACULTATIVA, RESPONSABILIDADE_CIVIL_AEROPORTUARIA, RESPONSABILIDADE_DO_EXPLORADOR_E_TRANSPORTADOR_AEREO, OUTRAS");
	public static final Set<String> FEATURE = SetUtils.createSet("MASSIFICADOS, MASSIFICADOS_MICROSEGUROS, GRANDES_RISCOS");
	public static final Set<String> TYPE = SetUtils.createSet("PARAMETRICO, INTERMITENTE, REGULAR_COMUM, CAPITAL_GLOBAL, PARAMETRICO_E_INTERMITENTE");
	public static final Set<String> DEDUCTIBLE_TYPE = SetUtils.createSet("REDUZIDA, NORMAL, MAJORADA, DEDUTIVEL, OUTROS");
	public static final Set<String> APPLICATION_TYPE = SetUtils.createSet("VALOR, PERCENTUAL, OUTROS");
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
				.setMaxLength(52)
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
				.setMaxLength(52)
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
