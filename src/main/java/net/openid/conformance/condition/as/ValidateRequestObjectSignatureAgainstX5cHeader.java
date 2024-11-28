package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.List;

public class ValidateRequestObjectSignatureAgainstX5cHeader extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_request_object" })
	@PostEnvironment(strings = "request_object_signing_alg")
	public Environment evaluate(Environment env) {

		String requestObject = env.getString("authorization_request_object", "value");

		try {

			SignedJWT jwt = SignedJWT.parse(requestObject);
			// Parse X.509 certificate
			List<Base64> x5c = jwt.getHeader().getX509CertChain();
			String encodedCert = x5c.get(0).toString();
			byte der[] = java.util.Base64.getDecoder().decode(encodedCert);
			X509Certificate cert = X509CertUtils.parse(der);

			PublicKey pubKey = cert.getPublicKey();

			KeyPair keyPair;
			JWK key;
			if (pubKey instanceof RSAPublicKey) {
				// We have an RSA public key
				// ...
				RSAKey rsaJWK = RSAKey.parse(cert);
				keyPair = rsaJWK.toKeyPair();
				key = rsaJWK;
			} else if (pubKey instanceof ECPublicKey) {
				ECKey ecJWK = ECKey.parse(cert);
				keyPair = ecJWK.toKeyPair();
				key = ecJWK;
			} else {
				throw error("Unknown key type");
			}

			JsonObject client = env.getObject("client");
			if (client != null && client.has("request_object_signing_alg")) {
				//https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata
				//request_object_signing_alg
				//All Request Objects from this Client MUST be rejected, if not signed with this algorithm.
				//The default, if omitted, is that any algorithm supported by the OP and the RP MAY be used
				String expectedAlg = OIDFJSON.getString(client.get("request_object_signing_alg"));
				JWSAlgorithm jwsAlgorithm = jwt.getHeader().getAlgorithm();
				if (!jwsAlgorithm.getName().equals(expectedAlg)) {
					throw error("Algorithm in JWT header does not match client request_object_signing_alg.",
						args("actual", jwsAlgorithm.getName(), "expected", expectedAlg));
				}
			}

			JWSVerifierFactory factory = new DefaultJWSVerifierFactory();

			JWSVerifier verifier = factory.createJWSVerifier(jwt.getHeader(), keyPair.getPublic());

			if (jwt.verify(verifier)) {
				String alg = jwt.getHeader().getAlgorithm().getName();
				env.putString("request_object_signing_alg", alg);
				logSuccess("Request object signature validated against the x5c header",
					args("request_object_signing_alg", alg,
						"jwk", key.toString(), "request_object", requestObject));
				return env;
			} else {
				throw error("Failed to verify signature using key from x5c header", args("key", key.toString(), "requestObject", requestObject));
			}

		} catch (JOSEException | ParseException e) {
			throw error("error validating request object signature", e);
		}

	}

}
