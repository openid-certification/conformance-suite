package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;
import net.openid.conformance.util.field.*;

import java.text.ParseException;
import java.util.Set;

public class ValidateErrorAndMetaFieldNames extends AbstractJsonAssertingCondition {
	private Set<String> errorCodes;

	@Override
	public Environment evaluate(Environment env) {

		JsonObject apiResponse;
		if (env.getObject("resource_endpoint_response_full") != null) {
			apiResponse = env.getObject("resource_endpoint_response_full");
			errorCodes = Sets.newHashSet(
				"SALDO_INSUFICIENTE", "BENEFICIARIO_INCOMPATIVEL", "VALOR_INCOMPATIVEL", "VALOR_ACIMA_LIMITE", "VALOR_INVALIDO",
				"COBRANCA_INVALIDA", "CONSENTIMENTO_INVALIDO", "JANELA_OPER_INVALIDA", "NAO_INFORMADO", "PAGAMENTO_DIVERGENTE_DO_CONSENTIMENTO"
			);
		} else {
			apiResponse = env.getObject("consent_endpoint_response_full");
			errorCodes = Sets.newHashSet(
				"FORMA_PGTO_INVALIDA", "DATA_PGTO_INVALIDA", "DETALHE_PGTO_INVALIDO", "NAO_INFORMADO"
			);
		}

		JsonObject decodedJwt;
		try {
			decodedJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(OIDFJSON.getString(apiResponse.getAsJsonObject().get("body")));
		} catch (ParseException exception) {
			throw error("Could not parse the body: ", apiResponse.getAsJsonObject());
		}
		JsonObject claims = decodedJwt.getAsJsonObject("claims");


		assertField(claims,
			new ObjectField
				.Builder("meta")
				.setValidator(this::assertMeta)
				.build());

		assertField(claims,
			new ObjectArrayField
				.Builder("errors")
				.setValidator(this::assertError)
				.setMinItems(1)
				.build());

		return env;
	}

	private void assertError(JsonObject error) {
		String pattern = "[\\w\\W\\s]*";

		assertField(error,
			new StringField
				.Builder("code")
				.setEnums(errorCodes)
				.build());

		assertField(error,
			new StringField
				.Builder("title")
				.setPattern(pattern)
				.setMaxLength(255)
				.build());

		assertField(error,
			new StringField
				.Builder("detail")
				.setPattern(pattern)
				.setMaxLength(2048)
				.build());

		if (error.size() > 3) {
			throw error("Error object contains extra fields not defined in swagger", args("Error", error));
		}
	}


	private void assertMeta(JsonObject meta) {
		assertField(meta,
			new IntField
				.Builder("totalRecords")
				.build());

		assertField(meta,
			new IntField
				.Builder("totalPages")
				.build());

		assertField(meta,
			new DatetimeField
				.Builder("requestDateTime")
				.setMaxLength(20)
				.build());

		if (meta.size() > 3) {
			throw error("Meta object contains extra fields not defined in swagger", args("Meta", meta));
		}
	}
}
