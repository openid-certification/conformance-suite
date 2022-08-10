package net.openid.conformance.openinsurance.validator.insuranceNautical.v1;

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
 * Api Source: swagger/openinsurance/insuranceNautical/v1/swagger-insurance-nautical.yaml
 * Api endpoint: /{policyId}/claim
 * Api version: 1.0.0
 */

@ApiName("Insurance Nautical Claim V1")
public class OpinInsuranceNauticalClaimValidatorV1 extends AbstractJsonAssertingCondition {
	private final OpenInsuranceLinksAndMetaValidator linksAndMetaValidator = new OpenInsuranceLinksAndMetaValidator(this);
	public static final Set<String> STATUS = SetUtils.createSet("ABERTO, ENCERRADO_COM_INDENIZACAO, ENCERRADO_SEM_INDENIZACAO, REABERTO, CANCELADO_POR_ERRO_OPERACIONAL, AVALIACAO_INICIAL");
	public static final Set<String> JUSTIFICATION = SetUtils.createSet("RISCO_EXCLUIDO, RISCO_AGRAVADO, SEM_DOCUMENTACAO, DOCUMENTACAO_INCOMPLETA, PRESCRICAO, FORA_COBERTURA, OUTROS");
	public static final Set<String> CODE = SetUtils.createSet("BASICA, BASICA_DE_OPERADOR_PORTUARIO, DANOS_ELETRICOS, DANOS_FISICOS_A_BENS_MOVEIS_E_IMOVEIS, DANOS_MORAIS, DESPESAS_COM_HONORARIOS_DE_ESPECIALISTAS, EXTENSAO_DO_LIMITE_DE_NAVEGACAO, GUARDA_DE_EMBARCACOES, LIMITE_DE_NAVEGACAO, PARTICIPACAO_EM_EVENTOS, PERDA_DE_RECEITA_BRUTA_E_OU_DESPESAS_ADICIONAIS_OU_EXTRAORDINARIAS, PERDA_E_OU_PAGAMENTO_DE_ALUGUEL, REMOCAO_DE_DESTROCOS, RESPONSABILIDADE_CIVIL, ROUBO_E_OU_FURTO_QUALIFICADO, SEGURO_DE_CONSTRUTORES_NAVAIS, TRANSPORTE_TERRESTRE, OUTRAS");

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
				.setMaxLength(66)
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
