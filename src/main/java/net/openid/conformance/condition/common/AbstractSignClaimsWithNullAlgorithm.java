package net.openid.conformance.condition.common;

import com.google.gson.JsonObject;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public abstract class AbstractSignClaimsWithNullAlgorithm extends AbstractCondition {

	protected abstract String getClaimsNotFoundErrorMsg();
	protected abstract String getSuccessMsg();

	protected Environment signWithNullAlgorithm(Environment env, String claimsSourceKey, String jwtTargetKey)
	{
		JsonObject objectClaims = env.getObject(claimsSourceKey);

		if (objectClaims == null) {
			throw error(getClaimsNotFoundErrorMsg());
		}

		try {
			JWTClaimsSet claimSet = JWTClaimsSet.parse(objectClaims.toString());

			PlainHeader header = new PlainHeader();

			PlainJWT plainJwt = new PlainJWT(header, claimSet);

			env.putString(jwtTargetKey, plainJwt.serialize());

			logSuccess(getSuccessMsg(), args("header", header.toJSONObject(),
				"claims", claimSet.toJSONObject(),
				jwtTargetKey + "_serialized", plainJwt.serialize()));

			return env;
		} catch (ParseException e) {
			throw error(e);
		}

	}

}
