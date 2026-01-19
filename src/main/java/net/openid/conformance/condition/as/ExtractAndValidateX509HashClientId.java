package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.security.cert.X509Certificate;

public class ExtractAndValidateX509HashClientId extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_request_object" })
	public Environment evaluate(Environment env) {

		String actual = env.getString("authorization_request_object", "claims.client_id");

		JsonElement x5c = env.getElementFromObject("authorization_request_object", "header.x5c");
		if (x5c == null) {
			throw error("Couldn't find required x5c array in authorization request object header");
		}
		if (!x5c.isJsonArray()) {
			throw error("x5c field in authorization request object header is not an array");
		}
		if (x5c.getAsJsonArray().isEmpty()) {
			throw error("x5c field in authorization request object header is an empty array");
		}
		JsonElement el = x5c.getAsJsonArray().get(0);
		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
			throw error("First entry in x5c array in authorization request object header is not a string");
		}

		Base64 certBase64 = Base64.from(OIDFJSON.getString(el));
		X509Certificate cert = X509CertUtils.parse(certBase64.decode());
		String expected = "x509_hash:" + X509CertUtils.computeSHA256Thumbprint(cert).toString();

		env.putString("client", "client_id", expected);
		env.putString("client_id", expected);

		if (!Strings.isNullOrEmpty(expected) && expected.equals(actual)) {
			logSuccess("Client ID matched",
				args("client_id", Strings.nullToEmpty(actual)));
			return env;
		} else {
			throw error("Mismatch between Client ID in authorization request and the calculated x509 hash", args("calculated", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual), "x5c", x5c));
		}

	}

}
