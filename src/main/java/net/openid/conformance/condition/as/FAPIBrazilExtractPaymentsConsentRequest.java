package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class FAPIBrazilExtractPaymentsConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"incoming_request","client", "server_encryption_keys"})

	@PostEnvironment(strings = {"consent_request_cpf"}, required = {"new_consent_request"})
	public Environment evaluate(Environment env) {

		String requestJwtString = env.getString("incoming_request", "body");
		JsonObject parsedRequest = null;
		try {
			JsonObject client = env.getObject("client");
			JsonObject serverEncKeys = env.getObject("server_encryption_keys");
			parsedRequest = JWTUtil.jwtStringToJsonObjectForEnvironment(requestJwtString, client, serverEncKeys);

			if(parsedRequest==null) {
				throw error("Couldn't extract payments consent request", args("request", requestJwtString));
			}

		} catch (ParseException e) {
			throw error("Couldn't parse payment consent request", e, args("request", requestJwtString));
		} catch (JOSEException e) {
			throw error("Payment consent request decryption failed", e, args("request", requestJwtString));
		}

		JsonObject claims = parsedRequest.get("claims").getAsJsonObject();

		if(!claims.has("data")) {
			throw error("Request must contain a 'data' element", args("request_json", claims));
		}
		JsonObject data = claims.get("data").getAsJsonObject();

		//required: loggedUser, creditor, payment
		//optional: businessEntity, debtorAccount
		if(!data.has("loggedUser")) {
			throw error("'data' must contain a 'loggedUser' element", args("request_json", claims));
		} else if(!data.get("loggedUser").isJsonObject()) {
			throw error("'loggedUser' must be a json object", args("request_json", claims));
		} else {
			JsonObject loggedUser = data.get("loggedUser").getAsJsonObject();

			JsonObject loggedUserDocument = loggedUser.get("document").getAsJsonObject();
			if(!loggedUserDocument.has("rel") || !"CPF".equals(OIDFJSON.getString(loggedUserDocument.get("rel")))) {
				throw error("loggedUser.document.rel is not equal to 'CPF'", args("loggedUser", loggedUser));
			}
			String identification = OIDFJSON.getString(loggedUserDocument.get("identification"));
			env.putString("consent_request_cpf", identification);
		}
		if(data.has("businessEntity")) {
			JsonObject businessEntity = data.get("businessEntity").getAsJsonObject();
			JsonObject businessEntityDocument = businessEntity.get("document").getAsJsonObject();
			if(!businessEntityDocument.has("rel") || !"CNPJ".equals(OIDFJSON.getString(businessEntityDocument.get("rel")))) {
				throw error("businessEntity.document.rel is not equal to 'CNPJ'", args("businessEntity", businessEntity));
			}
			String businessIdentification = OIDFJSON.getString(businessEntityDocument.get("identification"));
			env.putString("consent_request_cnpj", businessIdentification);
		}

		if(!data.has("creditor")) {
			throw error("'data' must contain a 'creditor' element", args("request_json", claims));
		} else {
			validateCreditor(data.get("creditor"), env);
		}
		if(!data.has("payment")) {
			throw error("'data' must contain a 'payment' element", args("request_json", claims));
		} else {
			validatePayment(data.get("payment"), env);
		}
		logSuccess("Parsed payments consent request", args("payments_consent_request", claims));
		env.putObject("new_consent_request", parsedRequest);

		return env;

	}
	//TODO not validating all details
	protected void validatePayment(JsonElement paymentElement, Environment env){
		if(!paymentElement.isJsonObject()) {
			throw error("Invalid payment element, not a json object", args("payment", paymentElement));
		}
	}
	protected void validateCreditor(JsonElement creditorElement, Environment env){
		if(!creditorElement.isJsonObject()) {
			throw error("Invalid creditor element, not a json object", args("creditor", creditorElement));
		}
		JsonObject creditor = creditorElement.getAsJsonObject();
		String personType = OIDFJSON.getString(creditor.get("personType"));
		if("PESSOA_NATURAL".equals(personType)) {
			String cpf = OIDFJSON.getString(creditor.get("cpfCnpj"));
			if(cpf==null || cpf.isEmpty()) {
				throw error("Invalid cpfCnpj element in creditor", args("creditor", creditorElement));
			}
			if(!cpf.matches("^([0-9]{11})$")) {
				throw error("Invalid cpfCnpj element in creditor, must be an 11 digit number", args("creditor", creditorElement));
			}
			env.putString("payment_consent_creditor_id", cpf);
			env.putString("payment_consent_creditor_type", "person");
		} else if("PESSOA_JURIDICA".equals(personType)){
			String cnpj = OIDFJSON.getString(creditor.get("cpfCnpj"));
			if(cnpj==null || cnpj.isEmpty()) {
				throw error("Invalid cpfCnpj element in creditor", args("creditor", creditorElement));
			}
			if(!cnpj.matches("^([0-9]{14})$")) {
				throw error("Invalid cpfCnpj element in creditor, must be a 14 digit number", args("creditor", creditorElement));
			}
			env.putString("payment_consent_creditor_id", cnpj);
			env.putString("payment_consent_creditor_type", "business");
		} else {
			throw error("Invalid personType value in creditor element. Expected one of: PESSOA_NATURAL, PESSOA_JURIDICA",
				args("creditor", creditorElement));
		}
		String name = OIDFJSON.getString(creditor.get("name"));
		if(name==null || name.isEmpty()) {
			throw error("Missing 'name' element in 'creditor'");
		}
	}

}
