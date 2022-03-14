package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.common.base.Strings;
import net.openid.conformance.condition.client.FAPIBrazilCallPaymentConsentEndpointWithBearerToken;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.Collections;


public class SetConsentsRequestToPatch extends FAPIBrazilCallPaymentConsentEndpointWithBearerToken {

	@Override
	protected HttpMethod getMethod(Environment env) {
		return HttpMethod.PATCH;
	}

	@Override
	protected String getUri(Environment env) {
		String consentUrl = env.getString("consent_url");
		if (Strings.isNullOrEmpty(consentUrl)) {
			throw error("consent url missing from configuration");
		}
		log(consentUrl);
		return consentUrl;
	}

	@Override
	protected HttpHeaders getHeaders(Environment env) {
		HttpHeaders headers = super.getHeaders(env);
		headers.setAccept(Collections.singletonList(DATAUTILS_MEDIATYPE_APPLICATION_JWT));
		headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
		return headers;
	}
}
