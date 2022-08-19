package net.openid.conformance.openinsurance.validator.insuranceAuto.v1;

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
 * Api Source: swagger/openinsurance/insuranceAuto/v1/swagger-insurance-auto-api.yaml
 * Api endpoint: /{policyId}/claim
 * Api version: 1.0.0
 */

@ApiName("Insurance Auto Claim V1")
public class OpinInsuranceAutoClaimValidatorV1 extends AbstractJsonAssertingCondition {
	private final OpenInsuranceLinksAndMetaValidator linksAndMetaValidator = new OpenInsuranceLinksAndMetaValidator(this);
	public static final Set<String> STATUS = SetUtils.createSet("ABERTO, ENCERRADO_COM_INDENIZACAO, ENCERRADO_SEM_INDENIZACAO, REABERTO, CANCELADO_POR_ERRO_OPERACIONAL, AVALIACAO_INICIAL");
	public static final Set<String> JUSTIFICATION = SetUtils.createSet("RISCO_EXCLUIDO, RISCO_AGRAVADO, SEM_DOCUMENTACAO, DOCUMENTACAO_INCOMPLETA, PRESCRICAO, FORA_COBERTURA, OUTROS");
	public static final Set<String> CODE = SetUtils.createSet("CASCO_COMPREENSIVA, CASCO_INCENDIO_ROUBO_E_FURTO, CASCO_ROUBO_E_FURTO, CASCO_INCENDIO, CASCO_ALAGAMENTO, CASCO_COLISAO_INDENIZACAO_PARCIAL, CASCO_COLISAO_INDENIZACAO_INTEGRAL, RESPONSABILIDADE_CIVIL_FACULTATIVA_DE_VEICULOS_RCFV, RESPONSABILIDADE_CIVIL_FACULTATIVA_DO_CONDUTOR_RCFC, ACIDENTE_PESSOAIS_DE_PASSAGEIROS_APP_VEICULO, ACIDENTE_PESSOAIS_DE_PASSAGEIROS_APP_CONDUTOR, VIDROS, DIARIA_POR_INDISPONIBILIDADE, LFR_LANTERNAS_FAROIS_E_RETROVISORES, ACESSORIOS_E_EQUIPAMENTOS, CARRO_RESERVA, PEQUENOS_REPAROS, RESPONSABILIDADE_CIVIL_CARTA_VERDE, OUTRAS");
	public static final Set<String> OCCURRENCE_CAUSE = SetUtils.createSet("ROUBO_OU_FURTO, ROUBO, FURTO, COLISAO_PARCIAL, COLISAO_INDENIZACAO_INTEGRAL, INCENDIO, ASSISTENCIA_HORAS, OUTROS");
	public static final Set<String> DRIVER_AT_OCCURRENCE_SEX = SetUtils.createSet("MASCULINO, FEMININO, NAO_DECLARADO, OUTROS");

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
				.Builder("covenantNumber")
				.setMaxLength(60)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("occurrenceCause")
				.setMaxLength(28)
				.setEnums(OCCURRENCE_CAUSE)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("driverAtOccurrenceSex")
				.setMaxLength(13)
				.setOptional()
				.setEnums(DRIVER_AT_OCCURRENCE_SEX)
				.build());

		assertField(data,
			new StringField
				.Builder("driverAtOccurrenceBirthDate")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("occurrenceCountry")
				.setMaxLength(3)
				.setOptional()
				.setPattern("^(\\w{3}){1}$")
				.build());

		assertField(data,
			new StringField
				.Builder("occurrencePostCode")
				.setMaxLength(60)
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
