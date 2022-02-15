package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JsonObjectBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

public class FAPIBrazilGeneratePaymentConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config" )
	@PostEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonElement paymentConsent = env.getElementFromObject("resource", "brazilPaymentConsent");
		if(paymentConsent == null) {
			throw error("As 'payments' is included in the 'scope' within the test configuration, a payment consent request JSON object must also be provided in the test configuration.");
		}

		JsonObject paymentRequestObject = buildFromNewConfigFields(env);

		env.putObject("consent_endpoint_request", paymentRequestObject);

		logSuccess(args("consent_endpoint_request", paymentRequestObject));
		return env;
	}

	private JsonObject buildFromNewConfigFields(Environment env) {

		String identification = extractOrDie(env, "resource", "brazilPaymentConsent.data.loggedUser.document.identification");
		String rel = extractOrDie(env, "resource", "brazilPaymentConsent.data.loggedUser.document.rel");
		String paymentAmount = extractOrDie(env, "resource", "brazilPaymentConsent.data.payment.amount");
		String debtorAccountIspb = extractOrDie(env, "resource", "brazilPaymentConsent.data.debtorAccount.ispb");
		String debtorAccountIssuer = extractOrDie(env, "resource", "brazilPaymentConsent.data.debtorAccount.issuer");
		String debtorAccountNumber = extractOrDie(env, "resource", "brazilPaymentConsent.data.debtorAccount.number");
		String debtorAccountType = extractOrDie(env, "resource", "brazilPaymentConsent.data.debtorAccount.accountType");
		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

		JsonObjectBuilder consentRequestObject = new JsonObjectBuilder()
			.addFields( "data.loggedUser.document", Map.of("identification", identification, "rel", rel))
			.addFields( "data.creditor",
				Map.of("personType", DictHomologKeys.PROXY_EMAIL_PERSON_TYPE,
					"cpfCnpj", DictHomologKeys.PROXY_EMAIL_CPF,
					"name", DictHomologKeys.PROXY_EMAIL_OWNER_NAME))

			.addFields( "data.creditor",
				Map.of("personType", DictHomologKeys.PROXY_EMAIL_PERSON_TYPE,
					"cpfCnpj", DictHomologKeys.PROXY_EMAIL_CPF,
					"name", DictHomologKeys.PROXY_EMAIL_OWNER_NAME))


			.addFields( "data.payment", Map.of("type", "PIX",
				"date", currentDate.toString(),
				"currency", "BRL",
				"amount", paymentAmount))

			.addFields( "data.payment.details", Map.of("localInstrument", DictHomologKeys.PROXY_EMAIL_STANDARD_LOCALINSTRUMENT,
				"proxy", DictHomologKeys.PROXY_EMAIL))

			.addFields( "data.payment.details.creditorAccount",
				Map.of("ispb", DictHomologKeys.PROXY_EMAIL_ISPB,
					"issuer", DictHomologKeys.PROXY_EMAIL_BRANCH_NUMBER,
					"number", DictHomologKeys.PROXY_EMAIL_ACCOUNT_NUMBER,
					"accountType", DictHomologKeys.PROXY_EMAIL_ACCOUNT_TYPE))

			.addFields("data.debtorAccount", Map.of("ispb", debtorAccountIspb,
				"issuer", debtorAccountIssuer,
				"number", debtorAccountNumber,
				"accountType", debtorAccountType));

		return consentRequestObject.build();
	}

	private String extractOrDie(Environment env, final String key, final String path) {
		Optional<String> string = Optional.ofNullable(env.getString(key, path));
		return string.orElseThrow(() -> error(String.format("Unable to find element %s in config at %s", key, path)));
	}

}

