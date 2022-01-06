package net.openid.conformance.openinsurance.testplan.utils;

import net.openid.conformance.openbanking_brasil.testmodules.support.CallResource;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;

public class CallNoCacheResource extends CallResource {

	@Override
	protected HttpHeaders getHeaders(Environment env) {
		HttpHeaders headers = super.getHeaders(env);
		headers.set("cache-control", "no-cache");
		return headers;
	}
}
