package net.openid.conformance.fapiciba.rp;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class BackchannelEnsureClientAssertionSignatureAlgorithmMatchesRegistered extends AbstractCondition {

	// TODO: This can't be right. backchannel_authentication_request_signing_alg is client metadata, we don't have that.
	// TODO: If anything, the check could be that it matches one of the backchannel_authentication_request_signing_alg_supported values.
	@Override
	@PreEnvironment(required = { "client", "client_assertion" })
	public Environment evaluate(Environment env) {

		String clientAssertionString = env.getString("client_assertion", "value");
		try {
			SignedJWT jwt = SignedJWT.parse(clientAssertionString);
			String expectedAlgName = env.getString("client", "backchannel_authentication_request_signing_alg");
			if(expectedAlgName!=null) {
				JWSAlgorithm expectedAlg = JWSAlgorithm.parse(expectedAlgName);
				JWSAlgorithm actualAlg = jwt.getHeader().getAlgorithm();
				if(expectedAlg.equals(actualAlg)) {
					logSuccess("Client assertion is signed using the registered backchannel_authentication_request_signing_alg algorithm",
								args("algorithm", expectedAlgName));
				} else {
					throw error("Client assertion is not signed using the registered backchannel_authentication_request_signing_alg algorithm.",
						args("expected", expectedAlgName, "actual", actualAlg.getName()));
				}
			} else {
				log("backchannel_authentication_request_signing_alg is not set for the client, any supported algorithm can be used");
			}
		} catch (ParseException ex) {
			throw error("Invalid client assertion", ex,
				args("client_assertion", clientAssertionString));
		}
		return env;
	}
}
