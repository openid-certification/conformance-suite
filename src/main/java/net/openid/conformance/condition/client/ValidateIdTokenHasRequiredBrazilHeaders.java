package net.openid.conformance.condition.client;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

@SuppressWarnings("deprecation")
public class ValidateIdTokenHasRequiredBrazilHeaders extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = { "id_token" })
	public Environment evaluate(Environment env) {

		String idToken = env.getString("id_token", "value");
		try {
			SignedJWT jwt = SignedJWT.parse(idToken);
			JWSHeader header = jwt.getHeader();

			String kid = header.getKeyID();
			String alg = header.getAlgorithm() != null ? header.getAlgorithm().getName() : null;
			Base64URL x5t = header.getX509CertThumbprint();
			Base64URL x5tS256 = header.getX509CertSHA256Thumbprint();

			if(kid == null || alg == null || (x5t == null && x5tS256 == null)) {
				throw error("The id_token header is missing required parameters", args(
					"kid", kid,
					"alg", alg,
					"x5t", x5t,
					"x5t#256", x5tS256
				));
			}

			logSuccess("All required header parameters present in id_token", args("header", header));
		} catch (ParseException e) {
			throw error("Failed to parse id_token", e);
		}

		return env;
	}

}
