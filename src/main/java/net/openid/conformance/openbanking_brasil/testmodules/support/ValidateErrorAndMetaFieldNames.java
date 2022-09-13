package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;
import net.openid.conformance.util.field.*;
import org.springframework.http.HttpStatus;

import java.text.ParseException;
import java.util.Set;

public class ValidateErrorAndMetaFieldNames extends AbstractJsonAssertingCondition {
	private Set<String> errorCodes;
	private int numberOfErrorRecords;
	private int status;

	@Override
	public Environment evaluate(Environment env) {

		JsonObject apiResponse;

		Boolean forceConsentsResponse = env.getBoolean("force_consents_response");
		if(forceConsentsResponse != null && forceConsentsResponse){
			env.putBoolean("force_consents_response", false);
			apiResponse = getConsentJsonObject(env);
		}else {
			if (env.getObject("resource_endpoint_response_full") != null) {
				apiResponse = env.getObject("resource_endpoint_response_full");
				errorCodes = Sets.newHashSet(
					"SALDO_INSUFICIENTE", "BENEFICIARIO_INCOMPATIVEL", "VALOR_INCOMPATIVEL", "VALOR_ACIMA_LIMITE", "VALOR_INVALIDO",
					"COBRANCA_INVALIDA", "CONSENTIMENTO_INVALIDO", "JANELA_OPER_INVALIDA", "NAO_INFORMADO", "PAGAMENTO_DIVERGENTE_DO_CONSENTIMENTO",
					"PARAMETRO_INVALIDO", "PARAMETRO_NAO_INFORMADO"
				);
			} else {
				apiResponse = getConsentJsonObject(env);
			}
		}

		if(apiResponse == null){
			throw error("Could not find the response object");
		}



		JsonObject decodedJwt;
		try {
			decodedJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(OIDFJSON.getString(apiResponse.getAsJsonObject().get("body")));
		} catch (ParseException exception) {
			throw error("Could not parse the body: ", apiResponse.getAsJsonObject());
		}
		JsonObject claims = decodedJwt.getAsJsonObject("claims");

		if (apiResponse.has("status")) {
			status = OIDFJSON.getInt(apiResponse.get("status"));
		} else {
			throw error("Could not get status from the response", args("apiResponse", apiResponse));
		}

		assertField(claims,
			new ObjectArrayField
				.Builder("errors")
				.setValidator(this::assertError)
				.setMinItems(1)
				.build());

		numberOfErrorRecords = findByPath(claims, "errors").getAsJsonArray().size();

		assertField(claims,
			new ObjectField
				.Builder("meta")
				.setValidator(this::assertMeta)
				.setOptional()
				.build());

		return env;
	}

	private JsonObject getConsentJsonObject(Environment env) {
		JsonObject apiResponse;
		apiResponse = env.getObject("consent_endpoint_response_full");
		errorCodes = Sets.newHashSet(
			"FORMA_PGTO_INVALIDA", "DATA_PGTO_INVALIDA", "DETALHE_PGTO_INVALIDO", "NAO_INFORMADO"
		);
		return apiResponse;
	}

	private void assertError(JsonObject error) {
		String pattern = "[\\w\\W\\s]*";

		StringField.Builder codeFieldBuilder = new StringField.Builder("code");

		if (status == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
			codeFieldBuilder.setEnums(errorCodes);
		}else {
			codeFieldBuilder.setPattern(pattern);
			codeFieldBuilder.setMaxLength(255);
		}

		assertField(error,
			codeFieldBuilder
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
				.Builder("totalPages")
				.setMinValue(1)
				.build());

		int totalPages = OIDFJSON.getInt(findByPath(meta, "totalPages"));


		IntField.Builder totalRecordsFieldBuilder = new IntField
			.Builder("totalRecords")
			.setMinValue(numberOfErrorRecords);


		if (totalPages == 1) {
			totalRecordsFieldBuilder.setMaxValue(numberOfErrorRecords);
		}


		assertField(meta, totalRecordsFieldBuilder.build());


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
