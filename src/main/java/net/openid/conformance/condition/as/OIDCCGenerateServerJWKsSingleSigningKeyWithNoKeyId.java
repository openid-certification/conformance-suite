package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OIDCCGenerateServerJWKsSingleSigningKeyWithNoKeyId extends OIDCCGenerateServerJWKs {

	@Override
	protected void setupParameters()
	{
		this.setGenerateKids(false);
		this.setNumberOfRSASigningKeysWithNoAlg(1);
		this.setNumberOfECCurveP256SigningKeysWithNoAlg(1);
		this.setNumberOfECCurveSECP256KSigningKeysWithNoAlg(1);
		this.setNumberOfOKPSigningKeysWithNoAlg(1);
	}
}
