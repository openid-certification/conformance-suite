package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Validate Meta only with RequestDateTime field
 **/
public class ValidateMetaOnlyRequestDateTime extends ValidateResponseMetaData {

	@Override
	@PostEnvironment(strings = "metaOnlyRequestDateTime")
	public Environment evaluate(Environment env) {

		JsonElement apiResponse = bodyFrom(env);

		if (!JsonHelper.ifExists(apiResponse, "$.data")) {
			apiResponse = env.getObject("consent_endpoint_response");
		}

		if(JsonHelper.ifExists(apiResponse, "$.meta")) {

			if (JsonHelper.ifExists(apiResponse, "$.meta.requestDateTime")) {
				String metaRequestDateTime = OIDFJSON.getString(findByPath(apiResponse, "$.meta.requestDateTime"));

				// Check that we have a Timezone element to this datetime object and that it is not longer than 20 chars
				if (metaRequestDateTime.length() > 20) {
					throw error("requestDateTime is more than 20 characters in length.");
				}

				// Parse the dateTime as RFC3339 and check that we have the 'Z'
				try {
					new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(metaRequestDateTime);
				} catch (ParseException e) {
					throw error("requestDateTime is not in valid RFC 3339 format.");
				}
				validateMetaDateTimeFormat(metaRequestDateTime);
			}else {
				throw error("requestDateTime field is missing in meta");
			}
		}


		boolean isConsentRequest = false;
		boolean isPaymentConsent = false;
		boolean isPayment = false;
		if (JsonHelper.ifExists(apiResponse, "$.data.consentId")) {
			isConsentRequest = true;
		}

		if (JsonHelper.ifExists(apiResponse, "$.data.payment")) {
			isPaymentConsent = true;
		}

		if (JsonHelper.ifExists(apiResponse, "$.data.paymentId")) {
			isPayment = true;
		}

		String selfLink = "";

		if (JsonHelper.ifExists(apiResponse, "$.links.self")) {
			selfLink = OIDFJSON.getString(findByPath(apiResponse, "$.links.self"));
			log("Validating self link: " + selfLink);
			if(isConsentRequest && !isPaymentConsent && !isPayment) {
				validateSelfLink(selfLink,
					OIDFJSON.getString(apiResponse.getAsJsonObject().getAsJsonObject("data").get("consentId")));
			}
		} else {
			//  self link is mandatory for all resources except dados Consents (payment consents do require a self link)
			if (!isConsentRequest) {
				throw error("There should be a 'self' link.");
			} else {
				if (isPaymentConsent) {
					throw error("Payment consent requires a 'self' link.");
				}
			}
		}

		env.putString("metaOnlyRequestDateTime", "true");
		return env;
	}
}
