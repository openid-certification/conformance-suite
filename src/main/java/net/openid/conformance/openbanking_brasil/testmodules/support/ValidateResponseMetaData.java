package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.field.DatetimeField;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ValidateResponseMetaData extends AbstractJsonAssertingCondition {

    @Override
	public Environment evaluate(Environment env) {

		JsonElement apiResponse = bodyFrom(env);

        if (!JsonHelper.ifExists(apiResponse, "$.data")) {
			apiResponse = env.getObject("consent_endpoint_response");
		}

        JsonElement dataElement = findByPath(apiResponse, "$.data");
        int metaTotalRecords = 1;
        int metaTotalPages = 1;

        if (JsonHelper.ifExists(apiResponse, "$.meta.totalRecords")) {
            metaTotalRecords = OIDFJSON.getInt(findByPath(apiResponse, "$.meta.totalRecords"));
        }

        if (JsonHelper.ifExists(apiResponse, "$.meta.totalPages")) {
            metaTotalPages = OIDFJSON.getInt(findByPath(apiResponse, "$.meta.totalPages"));
        }

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

        }

        Boolean isConsentRequest = false;
        Boolean isPaymentConsent = false;
        Boolean isPayment = false;
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
        String nextLink = "";
        String prevLink = "";

        if (JsonHelper.ifExists(apiResponse, "$.links.self")) {
            selfLink = OIDFJSON.getString(findByPath(apiResponse, "$.links.self"));
            log("Validating self link: " + selfLink);
            if(isConsentRequest && !isPaymentConsent && !isPayment) {
				validateSelfLink(selfLink,
					OIDFJSON.getString(apiResponse.getAsJsonObject().getAsJsonObject("data").get("consentId")));
			}
        } else {
            //  self link is mandatory for all resources except dados Consents (payment consents do require a self link)
            if (isConsentRequest == false) {
                throw error("There should be a 'self' link.");
            } else {
                if (isPaymentConsent) {
                    throw error("Payment consent requires a 'self' link.");
                }
            }
        }

        if (JsonHelper.ifExists(apiResponse, "$.links.next")) {
            nextLink = OIDFJSON.getString(findByPath(apiResponse, "$.links.next"));
        }

        if (JsonHelper.ifExists(apiResponse, "$.links.prev")) {
            prevLink = OIDFJSON.getString(findByPath(apiResponse, "$.links.prev"));
        }

        // Check if the record count in meta tallies with the actual data.
        // i.e. if record count > 1, then we should find an array in the data element.

        int arrayCount = 1; // We'll assume there is at least one data element.
		if (dataElement.isJsonArray()) {
            arrayCount = dataElement.getAsJsonArray().size();
        }

        if (arrayCount > metaTotalRecords) {
            throw error("Data contains more items than the metadata totalRecords.");
        }

        // check if there is 1 page - if so, there should not be a next and prev link.
        if (metaTotalPages == 1) {

            // Make sure we don't have a next or prev link
            if (!Strings.isNullOrEmpty(nextLink) || !Strings.isNullOrEmpty(prevLink) ) {

                throw error("There should not be a 'next' or 'prev' link.");
            }
        } else {

            // There is more than one page. Parse the self link
            URI selfLinkURI;
            try {
                selfLinkURI = new URI(selfLink);
            } catch (URISyntaxException e) {
                throw error("Invalid 'self' link URI.");
            }

            List<NameValuePair> selfLinkParamList = URLEncodedUtils.parse(selfLinkURI, StandardCharsets.UTF_8);
            MultiValueMap<String, String> selfLinkQueryStringParams = convertQueryStringParamsToMap(selfLinkParamList);

            // if Self is page=1, then we should not see a prev link
            int selfLinkPageNum = 1;
            try {
                selfLinkPageNum  = Integer.parseInt(selfLinkQueryStringParams.getFirst("page"));
            } catch (NumberFormatException e) {}

            if ( selfLinkPageNum == 1) {

                if (!Strings.isNullOrEmpty(prevLink) ) {

                    throw error("There should not be a 'prev' link.");
                }

                // self link page = 1, total page > 1 - we need a next link.
                if (Strings.isNullOrEmpty(nextLink) ) {
                    throw error("There should be a 'next' link.");
                }
            }

            if ( selfLinkPageNum > 1 && selfLinkPageNum < metaTotalPages) {
                // Total pages > 1 and self page > 1 and self page < total pages - so we should see a next & prev link
                if (Strings.isNullOrEmpty(nextLink) ) {
                    throw error("There should be a 'next' link.");
                }

                if (Strings.isNullOrEmpty(prevLink) ) {
                    throw error("There should be a 'prev' link.");
                }
            }

            // if Self page= metaTotalPages (i.e. we are on the last page), then we should not find a next link.
            if (selfLinkPageNum == metaTotalPages) {

                if (!Strings.isNullOrEmpty(nextLink) ) {

                    String errorMsg = "There should not be a 'next' link.";
                    throw error(errorMsg);
                }

                if (Strings.isNullOrEmpty(prevLink) ) {

                    String errorMsg = "There should be a 'prev' link.";
                    throw error(errorMsg);
                }
            }
        }

        return env;
	}


	private void validateMetaDateTimeFormat(String requestDateTime){
			if(!requestDateTime.matches(DatetimeField.ALTERNATIVE_PATTERN)){
				throw error("requestDateTime field is not compliant with the swagger format", Map.of("requestedDateTime", requestDateTime));
			}
			logSuccess("requestDateTime field is compliant with the swagger format", Map.of("requestedDateTime", requestDateTime));
	}

    protected MultiValueMap<String, String> convertQueryStringParamsToMap(List<NameValuePair> parameters) {
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

		for (NameValuePair pair : parameters) {
			queryParams.add(pair.getName(), pair.getValue());
		}
		return queryParams;
    }

    private void validateSelfLink(String selfLink, String consentIdField){
    	final String consent_regex = "consents/v1/consents/";
    	final String consent_payment_regex = "payments/v1/consents";
    	if(selfLink.contains(consent_regex)){
			String consentID = selfLink.split(consent_regex)[1];
			if(consentID.isBlank() || consentID.isEmpty()){
				throw error("Consent ID needs to be attached to the self link post creation");
			} else {
				if(consentID.equalsIgnoreCase(consentIdField)){
					logSuccess("Consent ID in self link matches the consent ID in the returned object");
				}
			}
		} else if(!selfLink.contains(consent_payment_regex)){
    		throw error("Invalid 'self' link URI. URI: " + selfLink);
		}
	}
}
