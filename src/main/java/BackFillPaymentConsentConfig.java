package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static net.openid.conformance.util.JsonObjectBuilder.addFields;

public class BackFillPaymentConsentConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config" )
	public Environment evaluate(Environment env) {
		JsonElement paymentConsent = env.getElementFromObject("resource", "brazilPaymentConsent");
		if(paymentConsent != null) {
			log("You have provided a brazilPaymentConsent config block. " +
				"This field is deprecated in favour of more specific fields, " +
				"and will be removed in a future release. " +
				"This config will be used for this test");
			return env;
		}
		logSuccess("Config for brazilPaymentConsent not provided, generating from other fields");
		JsonObject brazilPaymentConsent = buildFromNewConfigFields(env);
		env.getObject("resource").add("brazilPaymentConsent", brazilPaymentConsent);
		return env;
	}

	private JsonObject buildFromNewConfigFields(Environment env) {
		JsonObject consentRequestObject = new JsonObject();
		String identification = extractOrDie(env, "resource", "brazilLoggedUser.identification");
		String rel = extractOrDie(env, "resource", "brazilLoggedUser.rel");
		String paymentAmount = extractOrDie(env, "resource", "paymentAmount");
		String debtorAccountIspb = extractOrDie(env, "resource", "brazilDebtorAccount.ispb");
		String debtorAccountIssuer = extractOrDie(env, "resource", "brazilDebtorAccount.issuer");
		String debtorAccountNumber = extractOrDie(env, "resource", "brazilDebtorAccount.number");
		String debtorAccountType = extractOrDie(env, "resource", "brazilDebtorAccount.accountType");

		addFields(consentRequestObject, "data.loggedUser.document", Map.of("identification", identification, "rel", rel));

		addFields(consentRequestObject, "data.creditor",
				Map.of("personType", DictHomologKeys.PROXY_EMAIL_PERSON_TYPE,
						"cpfCnpj", DictHomologKeys.PROXY_EMAIL_CPF,
						"name", DictHomologKeys.PROXY_EMAIL_OWNER_NAME));

		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
		addFields(consentRequestObject, "data.payment", Map.of("type", "PIX",
			"date", currentDate.toString(),
			"currency", "BRL",
			"amount", paymentAmount));

		addFields(consentRequestObject, "data.payment.details", Map.of("localInstrument", DictHomologKeys.PROXY_EMAIL_STANDARD_LOCALINSTRUMENT,
			"proxy", DictHomologKeys.PROXY_EMAIL));

		addFields(consentRequestObject, "data.payment.details.creditorAccount",
			Map.of("ispb", DictHomologKeys.PROXY_EMAIL_ISPB,
					"issuer", DictHomologKeys.PROXY_EMAIL_BRANCH_NUMBER,
					"number", DictHomologKeys.PROXY_EMAIL_ACCOUNT_NUMBER,
					"accountType", DictHomologKeys.PROXY_EMAIL_ACCOUNT_TYPE));

		addFields(consentRequestObject, "data.debtorAccount", Map.of("ispb", debtorAccountIspb,
														"issuer", debtorAccountIssuer,
														"number", debtorAccountNumber,
														"accountType", debtorAccountType));
		return consentRequestObject;
	}

	private String extractOrDie(Environment env, final String key, final String path) {
		Optional<String> string = Optional.ofNullable(env.getString(key, path));
		return string.orElseThrow(() -> error(String.format("Unable to find element %s in config at %s", key, path)));
	}

}
