package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.FAPIBrazilCallPaymentConsentEndpointWithBearerToken;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Collections;

import com.google.common.base.Strings;
	import com.google.gson.JsonObject;
	import net.openid.conformance.condition.AbstractCondition;
	import net.openid.conformance.condition.PostEnvironment;
	import net.openid.conformance.condition.PreEnvironment;
	import net.openid.conformance.testmodule.Environment;
	import org.springframework.http.HttpEntity;
	import org.springframework.http.HttpHeaders;
	import org.springframework.http.HttpMethod;
	import org.springframework.http.MediaType;
	import org.springframework.http.ResponseEntity;
	import org.springframework.http.client.ClientHttpResponse;
	import org.springframework.web.client.DefaultResponseErrorHandler;
	import org.springframework.web.client.RestClientException;
	import org.springframework.web.client.RestClientResponseException;
	import org.springframework.web.client.RestTemplate;

	import java.io.IOException;
	import java.security.KeyManagementException;
	import java.security.KeyStoreException;
	import java.security.NoSuchAlgorithmException;
	import java.security.UnrecoverableKeyException;
	import java.security.cert.CertificateException;
	import java.security.spec.InvalidKeySpecException;
	import java.util.Collections;


public class FAPIBrazilCallPaymentConsentEndpointWithBearerTokenNoAcceptField extends FAPIBrazilCallPaymentConsentEndpointWithBearerToken {
	@Override
	protected HttpHeaders getHeaders(Environment env) {
		HttpHeaders headers = super.getHeaders(env);
		//Spring internally adds accept field, therefore we set it to ALL
		headers.setAccept(Collections.singletonList(MediaType.ALL));
		return headers;
	}
}
