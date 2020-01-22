package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class OIDCCGenerateServerJWKs extends AbstractCondition {

	protected int numberOfRSSigningKeys = 2;
	protected int numberOfPSSigningKeys = 2;
	protected int numberOfESSigningKeys = 2;
	protected int numberOfEdSigningKeys = 2;

	protected int numberOfRSAEncKeys = 1;
	protected int numberOfECEncKeys = 1;

	protected boolean generateKids = true;

	protected int rsaKeySize = 2048;
	protected Curve ecCurve = Curve.P_256;

	protected List<JWK> allGeneratedKeys;
	protected List<JWK> signingKeyToBeUsed;
	protected List<JWK> encryptionKeysToBeUsed;

	protected JWSAlgorithm rsSigningAlgorithm = JWSAlgorithm.RS256;
	protected JWSAlgorithm psSigningAlgorithm = JWSAlgorithm.PS256;
	protected JWSAlgorithm esSigningAlgorithm = JWSAlgorithm.ES256;

	protected JWEAlgorithm encryptionAlgorithmForRSAKeys = JWEAlgorithm.RSA_OAEP_256;
	protected JWEAlgorithm encryptionAlgorithmForECKeys = JWEAlgorithm.ECDH_ES;

	/**
	 * override this and call setters
	 */
	protected void setupParameters() {

	}

	@Override
	@PostEnvironment(required = { "server_public_jwks", "server_jwks", "server_encryption_keys" })
	public Environment evaluate(Environment env) {
		allGeneratedKeys = new LinkedList<>();
		signingKeyToBeUsed = new LinkedList<>();
		encryptionKeysToBeUsed = new LinkedList<>();
		setupParameters();

		try {
			//changing the order of createKeys calls here may change the selected keys and signing algorithm
			createKeys(numberOfRSSigningKeys, KeyType.RSA, KeyUse.SIGNATURE, rsSigningAlgorithm);
			createKeys(numberOfESSigningKeys, KeyType.EC, KeyUse.SIGNATURE, esSigningAlgorithm);
			createKeys(numberOfPSSigningKeys, KeyType.RSA, KeyUse.SIGNATURE, psSigningAlgorithm);
			createKeys(numberOfEdSigningKeys, KeyType.EC, KeyUse.SIGNATURE, JWSAlgorithm.EdDSA);

			createKeys(numberOfRSAEncKeys, KeyType.RSA, KeyUse.ENCRYPTION, encryptionAlgorithmForRSAKeys);
			createKeys(numberOfECEncKeys, KeyType.EC, KeyUse.ENCRYPTION, encryptionAlgorithmForECKeys);

			JWKSet publicJwkSet = new JWKSet(allGeneratedKeys);
			JsonObject publicJwks = JWKUtil.getPublicJwksAsJsonObject(publicJwkSet);

			JWKSet privateJwkSet = new JWKSet(signingKeyToBeUsed);
			JsonObject jwks = JWKUtil.getPrivateJwksAsJsonObject(privateJwkSet);

			JWKSet encJwkSet = new JWKSet(encryptionKeysToBeUsed);
			JsonObject encJwks = JWKUtil.getPrivateJwksAsJsonObject(encJwkSet);

			env.putObject("server_public_jwks", publicJwks);
			env.putObject("server_jwks", jwks);
			env.putObject("server_encryption_keys", encJwks);


			logSuccess("Generated server public private JWK sets", args("server_public_jwks", publicJwks,
																				"server_jwks", jwks,
																				"server_encryption_keys", encJwks));

			return env;

		} catch (JOSEException e) {
			throw error("Failed to generate server JWK Set", e);
		}

	}

	protected void createKeys(int keyCount, KeyType keyType, KeyUse keyUse, Algorithm algorithm) throws JOSEException {
		if(keyCount<1) {
			return;
		}
		int whichKeyToUse = getIndexOfKeyToUse(keyCount);

		for(int i=0; i<keyCount; i++) {
			JWKGenerator<? extends JWK> jwkGenerator = null;
			if (KeyType.EC.equals(keyType)) {
				jwkGenerator = new ECKeyGenerator(ecCurve);
			} else if (KeyType.RSA.equals(keyType)) {
				jwkGenerator = new RSAKeyGenerator(rsaKeySize);
			}
			jwkGenerator.keyUse(keyUse);
			if(generateKids) {
				jwkGenerator.keyID(UUID.randomUUID().toString());
			}
			jwkGenerator.algorithm(algorithm);

			JWK generatedJWK = jwkGenerator.generate();
			allGeneratedKeys.add(generatedJWK);
			if (keyUse == KeyUse.ENCRYPTION) {
				encryptionKeysToBeUsed.add(generatedJWK);
			}

			if(i==whichKeyToUse && (keyUse==KeyUse.SIGNATURE)) {
				signingKeyToBeUsed.add(generatedJWK);
			}
		}
	}


	/**
	 * Returns a random key index
	 * Override to use a constant index
	 * @return
	 */
	protected int getIndexOfKeyToUse(int keyCount) {
		if(keyCount<2) {
			return 0;
		}
		Random random = new Random();
		int index = 0;
		index = random.ints(1, 0, keyCount - 1).findFirst().getAsInt();
		return index;
	}


	public void setNumberOfRSAEncKeys(int numberOfRSAEncKeys) {
		this.numberOfRSAEncKeys = numberOfRSAEncKeys;
	}

	public void setNumberOfECEncKeys(int numberOfECEncKeys) {
		this.numberOfECEncKeys = numberOfECEncKeys;
	}

	public void setGenerateKids(boolean generateKids) {
		this.generateKids = generateKids;
	}

	public void setRsaKeySize(int rsaKeySize) {
		this.rsaKeySize = rsaKeySize;
	}

	public void setEcCurve(Curve ecCurve) {
		this.ecCurve = ecCurve;
	}

	public void setEncryptionAlgorithmForECKeys(JWEAlgorithm encryptionAlgorithmForECKeys) {
		this.encryptionAlgorithmForECKeys = encryptionAlgorithmForECKeys;
	}

	public int getNumberOfRSSigningKeys() {
		return numberOfRSSigningKeys;
	}

	public void setNumberOfRSSigningKeys(int numberOfRSSigningKeys) {
		this.numberOfRSSigningKeys = numberOfRSSigningKeys;
	}

	public int getNumberOfPSSigningKeys() {
		return numberOfPSSigningKeys;
	}

	public void setNumberOfPSSigningKeys(int numberOfPSSigningKeys) {
		this.numberOfPSSigningKeys = numberOfPSSigningKeys;
	}

	public int getNumberOfESSigningKeys() {
		return numberOfESSigningKeys;
	}

	public void setNumberOfESSigningKeys(int numberOfESSigningKeys) {
		this.numberOfESSigningKeys = numberOfESSigningKeys;
	}

	public int getNumberOfEdSigningKeys() {
		return numberOfEdSigningKeys;
	}

	public void setNumberOfEdSigningKeys(int numberOfEdSigningKeys) {
		this.numberOfEdSigningKeys = numberOfEdSigningKeys;
	}

	public int getNumberOfRSAEncKeys() {
		return numberOfRSAEncKeys;
	}

	public int getNumberOfECEncKeys() {
		return numberOfECEncKeys;
	}

	public boolean isGenerateKids() {
		return generateKids;
	}

	public int getRsaKeySize() {
		return rsaKeySize;
	}

	public Curve getEcCurve() {
		return ecCurve;
	}


	public JWSAlgorithm getRsSigningAlgorithm() {
		return rsSigningAlgorithm;
	}

	public void setRsSigningAlgorithm(JWSAlgorithm rsSigningAlgorithm) {
		this.rsSigningAlgorithm = rsSigningAlgorithm;
	}

	public JWSAlgorithm getPsSigningAlgorithm() {
		return psSigningAlgorithm;
	}

	public void setPsSigningAlgorithm(JWSAlgorithm psSigningAlgorithm) {
		this.psSigningAlgorithm = psSigningAlgorithm;
	}

	public JWSAlgorithm getEsSigningAlgorithm() {
		return esSigningAlgorithm;
	}

	public void setEsSigningAlgorithm(JWSAlgorithm esSigningAlgorithm) {
		this.esSigningAlgorithm = esSigningAlgorithm;
	}

	public JWEAlgorithm getEncryptionAlgorithmForRSAKeys() {
		return encryptionAlgorithmForRSAKeys;
	}

	public void setEncryptionAlgorithmForRSAKeys(JWEAlgorithm encryptionAlgorithmForRSAKeys) {
		this.encryptionAlgorithmForRSAKeys = encryptionAlgorithmForRSAKeys;
	}

	public JWEAlgorithm getEncryptionAlgorithmForECKeys() {
		return encryptionAlgorithmForECKeys;
	}
}
