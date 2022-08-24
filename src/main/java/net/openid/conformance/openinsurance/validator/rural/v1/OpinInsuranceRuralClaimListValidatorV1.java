package net.openid.conformance.openinsurance.validator.rural.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openinsurance.validator.OpenInsuranceLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.NumberField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api Source: swagger/openinsurance/rural/v1/swagger-insurance-rural-api.yaml
 * Api endpoint: /{policyId}/claim
 * Api version: 1.0.0
 */

@ApiName("Insurance Rural Claims List V1")
public class OpinInsuranceRuralClaimListValidatorV1 extends AbstractJsonAssertingCondition {
	private final OpenInsuranceLinksAndMetaValidator linksAndMetaValidator = new OpenInsuranceLinksAndMetaValidator(this);
	public static final Set<String> STATUS = SetUtils.createSet("ABERTO, ENCERRADO_COM_INDENIZACAO, ENCERRADO_SEM_INDENIZACAO, REABERTO, CANCELADO_POR_ERRO_OPERACIONAL, AVALIACAO_INICIAL");
	public static final Set<String> JUSTIFICATION = SetUtils.createSet("RISCO_EXCLUIDO, RISCO_AGRAVADO, SEM_DOCUMENTACAO, DOCUMENTACAO_INCOMPLETA, PRESCRICAO, FORA_COBERTURA, OUTROS");
	public static final Set<String> CODE = SetUtils.createSet("GRANIZO, GEADA, GRANIZO_GEADA, GRANIZO_GEADA_CHUVA_EXCESSIVA, COMPREENSIVA, COMPREENSIVA_COM_DOENCAS_E_PRAGAS, CANCRO_CITRICO, COMPREENSIVA_PARA_A_MODALIDADE_BENFEITORIAS_E_PRODUTOS_AGROPECUARIO, COMPREENSIVA_PARA_A_MODALIDADE_PENHOR_RURAL, MORTE_DE_ANIMAIS, CONFINAMENTO_SEMI_CONFINAMENTO_BOVINOS_DE_CORTE, CONFINAMENTO_BOVINOS_DE_LEITE, VIAGEM, EXPOSICAO_MOSTRA_E_LEILAO, CARREIRA, SALTO_E_ADESTRAMENTO, PROVAS_FUNCIONAIS, HIPISMO_RURAL, POLO, TROTE, VAQUEJADA, EXTENSAO_DE_COBERTURA_EM_TERRITORIO_ESTRANGEIRO, TRANSPORTE, RESPONSABILIDADE_CIVIL, PERDA_DE_FERTILIDADE_DE_GARANHAO, REEMBOLSO_CIRURGICO, COLETA_DE_SEMEN, PREMUNICAO, COMPREENSIVA_PARA_A_MODALIDADE_FLORESTAS, VIDA_DO_PRODUTOR_RURAL, BASICA_DE_FATURAMENTO_PECUARIO, OUTRAS");
	public static final Set<String> COUNTRY_SUB_DEVISION = SetUtils.createSet("AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA, PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO");
	public static final Set<String> ID_TYPE = SetUtils.createSet("CPF, CNPJ, OUTROS");

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

		assertField(data,
			new ObjectField
				.Builder("branchInfo")
				.setValidator(this::assertBranchInfo)
				.setOptional()
				.build());
	}

	private void assertBranchInfo(JsonObject data) {
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
				.setOptional()
				.setEnums(COUNTRY_SUB_DEVISION)
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
				.setOptional()
				.setPattern("^(\\w{3}){1}$")
				.build());

		assertField(data,
			new StringField
				.Builder("surveyorIdType")
				.setMaxLength(6)
				.setOptional()
				.setEnums(ID_TYPE)
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
