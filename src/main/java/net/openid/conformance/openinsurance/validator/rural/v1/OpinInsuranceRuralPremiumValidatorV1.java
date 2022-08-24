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
 * Api endpoint: /{policyId}/premium
 * Api version: 1.0.0
 */

@ApiName("Insurance Rural Premium V1")
public class OpinInsuranceRuralPremiumValidatorV1 extends AbstractJsonAssertingCondition {
	private final OpenInsuranceLinksAndMetaValidator linksAndMetaValidator = new OpenInsuranceLinksAndMetaValidator(this);
	public static final Set<String> CODE = SetUtils.createSet("GRANIZO, GEADA, GRANIZO_GEADA, GRANIZO_GEADA_CHUVA_EXCESSIVA, COMPREENSIVA, COMPREENSIVA_COM_DOENCAS_E_PRAGAS, CANCRO_CITRICO, COMPREENSIVA_PARA_A_MODALIDADE_BENFEITORIAS_E_PRODUTOS_AGROPECUARIO, COMPREENSIVA_PARA_A_MODALIDADE_PENHOR_RURAL, MORTE_DE_ANIMAIS, CONFINAMENTO_SEMI_CONFINAMENTO_BOVINOS_DE_CORTE, CONFINAMENTO_BOVINOS_DE_LEITE, VIAGEM, EXPOSICAO_MOSTRA_E_LEILAO, CARREIRA, SALTO_E_ADESTRAMENTO, PROVAS_FUNCIONAIS, HIPISMO_RURAL, POLO, TROTE, VAQUEJADA, EXTENSAO_DE_COBERTURA_EM_TERRITORIO_ESTRANGEIRO, TRANSPORTE, RESPONSABILIDADE_CIVIL, PERDA_DE_FERTILIDADE_DE_GARANHAO, REEMBOLSO_CIRURGICO, COLETA_DE_SEMEN, PREMUNICAO, COMPREENSIVA_PARA_A_MODALIDADE_FLORESTAS, VIDA_DO_PRODUTOR_RURAL, BASICA_DE_FATURAMENTO_PECUARIO, OUTRAS");
	public static final Set<String> MOVEMENT_TYPE = SetUtils.createSet("LIQUIDACAO_DE_PREMIO, LIQUIDACAO_DE_RESTITUICAO_DE_PREMIO, LIQUIDACAO_DE_CUSTO_DE_AQUISICAO, LIQUIDACAO_DE_RESTITUICAO_DE_CUSTO_DE_AQUISICAO, ESTORNO_DE_PREMIO, ESTORNO_DE_RESTITUICAO_DE_PREMIO, ESTORNO_DE_CUSTO_DE_AQUISICAO, EMISSAO_DE_PREMIO, CANCELAMENTO_DE_PARCELA, EMISSAO_DE_RESTITUICAO_DE_PREMIO, REABERTURA_DE_PARCELA, BAIXA_POR_PERDA");
	public static final Set<String> MOVEMENT_ORIGIN = SetUtils.createSet("EMISSAO_DIRETA, EMISSAO_ACEITA_DE_COSSEGURO, EMISSAO_CEDIDA_DE_COSSEGURO");
	public static final Set<String> ID_TYPE = SetUtils.createSet("CPF, CNPJ, OUTROS");
	public static final Set<String> PAYMENT_TYPE = SetUtils.createSet("BOLETO, TED, TEF, CARTAO, DOC, CHEQUE, DESCONTO_EM_FOLHA, PIX, DINHEIRO_EM_ESPECIE, OUTROS");

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

	private void assertPayments(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("movementDate")
				.build());

		assertField(data,
			new StringField
				.Builder("movementType")
				.setMaxLength(47)
				.setEnums(MOVEMENT_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("movementOrigin")
				.setMaxLength(27)
				.setEnums(MOVEMENT_ORIGIN)
				.setOptional()
				.build());

		assertField(data,
			new NumberField
				.Builder("movementPaymentsNumber")
				.setMaxLength(3)
				.build());

		assertField(data,
			new ObjectField
				.Builder("amount")
				.setValidator(this::assertAmount)
				.build());

		assertField(data,
			new StringField
				.Builder("maturityDate")
				.build());

		assertField(data,
			new StringField
				.Builder("tellerId")
				.setMaxLength(1024)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("tellerIdType")
				.setMaxLength(6)
				.setEnums(ID_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("tellerName")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("financialInstitutionCode")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("paymentType")
				.setMaxLength(19)
				.setOptional()
				.setEnums(PAYMENT_TYPE)
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
