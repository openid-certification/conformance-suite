package net.openid.conformance.condition.client;

import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.util.Set;

public class FAPIValidateIdTokenSigningAlg extends AbstractValidateIdTokenSigningAlg {
	@Override
	protected Set<String> getPermitted() {
		return Set.of( "PS256", "ES256" );
	}
}
