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
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.PreGeneratedJwks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class OIDCCGenerateServerJWKs extends AbstractCondition {

	protected int numberOfRSASigningKeysWithNoAlg = 2;
	protected int numberOfECCurveP256SigningKeysWithNoAlg = 2;
	protected int numberOfECCurveSECP256KSigningKeysWithNoAlg = 1;
	protected int numberOfOKPSigningKeysWithNoAlg = 1;

	protected int numberOfRSSigningKeys = 0;
	protected int numberOfPSSigningKeys = 0;
	protected int numberOfES256SigningKeys = 0;
	protected int numberOfEdSigningKeys = 0;

	protected int numberOfRSAEncKeys = 1;
	protected int numberOfECEncKeys = 1;

	protected boolean generateSigKids = true;
	protected boolean generateEncKids = true;

	protected int rsaKeySize = 2048;
	protected Curve esCurve = Curve.P_256;
	protected Curve esKCurve = Curve.SECP256K1;
	protected Curve edCurve = Curve.Ed25519;

	protected List<JWK> allGeneratedKeys;
	protected List<JWK> signingKeyToBeUsed;
	protected List<JWK> encryptionKeysToBeUsed;

	protected JWSAlgorithm rsSigningAlgorithm = JWSAlgorithm.RS256;
	protected JWSAlgorithm psSigningAlgorithm = JWSAlgorithm.PS256;
	protected JWSAlgorithm esSigningAlgorithm = JWSAlgorithm.ES256;

	@SuppressWarnings("deprecation")
	protected JWEAlgorithm encryptionAlgorithmForRSAKeys = JWEAlgorithm.RSA_OAEP;
	protected JWEAlgorithm encryptionAlgorithmForECKeys = JWEAlgorithm.ECDH_ES;

	/**
	 * override this and call setters to set number of keys
	 */
	protected void setupParameters() {

	}

	@Override
	@PostEnvironment(required = { "server_public_jwks", "server_jwks", "server_encryption_keys" })
	public Environment evaluate(Environment env) {
		allGeneratedKeys = new ArrayList<>();
		signingKeyToBeUsed = new ArrayList<>();
		encryptionKeysToBeUsed = new ArrayList<>();
		setupParameters();

		try {
			//changing the order of createKeys calls here may affect the signing key selection
			//See JWKUtil.selectAsymmetricJWSKey for full details
			createKeys(env, numberOfRSASigningKeysWithNoAlg, KeyType.RSA, KeyUse.SIGNATURE, null, null);
			createKeys(env, numberOfECCurveP256SigningKeysWithNoAlg, KeyType.EC, KeyUse.SIGNATURE, null, esCurve);
			createKeys(env, numberOfECCurveSECP256KSigningKeysWithNoAlg, KeyType.EC, KeyUse.SIGNATURE, null, esKCurve);
			createKeys(env, numberOfOKPSigningKeysWithNoAlg, KeyType.OKP, KeyUse.SIGNATURE, null, null);

			createKeys(env, numberOfRSSigningKeys, KeyType.RSA, KeyUse.SIGNATURE, rsSigningAlgorithm, null);
			createKeys(env, numberOfES256SigningKeys, KeyType.EC, KeyUse.SIGNATURE, esSigningAlgorithm, esCurve);
			createKeys(env, numberOfPSSigningKeys, KeyType.RSA, KeyUse.SIGNATURE, psSigningAlgorithm, null);
			createKeys(env, numberOfEdSigningKeys, KeyType.OKP, KeyUse.SIGNATURE, JWSAlgorithm.EdDSA, null);

			createKeys(env, numberOfRSAEncKeys, KeyType.RSA, KeyUse.ENCRYPTION, encryptionAlgorithmForRSAKeys, null);
			createKeys(env, numberOfECEncKeys, KeyType.EC, KeyUse.ENCRYPTION, encryptionAlgorithmForECKeys, esCurve);

			JWKSet publicJwkSet = new JWKSet(allGeneratedKeys);
			JsonObject publicJwks = JWKUtil.getPublicJwksAsJsonObject(publicJwkSet);

			JWKSet privateJwkSet = new JWKSet(signingKeyToBeUsed);
			JsonObject jwks = JWKUtil.getPrivateJwksAsJsonObject(privateJwkSet);

			JWKSet encJwkSet = new JWKSet(encryptionKeysToBeUsed);
			JsonObject encJwks = JWKUtil.getPrivateJwksAsJsonObject(encJwkSet);

			env.putObject("server_public_jwks", publicJwks);
			env.putObject("server_jwks", jwks);
			env.putObject("server_encryption_keys", encJwks);


			log("Generated server public private JWK sets", args("server_public_jwks", publicJwks,
																				"server_jwks", jwks,
																				"server_encryption_keys", encJwks));

			return env;

		} catch (JOSEException e) {
			throw error("Failed to generate server JWK Set", e);
		}

	}

	/**
	 *
	 * @param keyCount
	 * @param keyType EC, RSA or OKP
	 * @param keyUse if null keys won't have use
	 * @param algorithm if null keys won't have alg
	 * @throws JOSEException
	 */
	protected void createKeys(Environment env, int keyCount, KeyType keyType, KeyUse keyUse, Algorithm algorithm, Curve curveForECKeys) throws JOSEException {
		if(keyCount<1) {
			return;
		}
		int whichKeyToUse = getIndexOfKeyToUse(keyCount);

		for(int i=0; i<keyCount; i++) {
			// Pull from a process-wide pool of pre-generated keypairs rather than
			// running fresh RSA/EC primality work per test. The kid (when set) is
			// still fresh per handout so kid-based lookups behave the same as before.
			String kid = ((generateSigKids && ((null == keyUse) || KeyUse.SIGNATURE.equals(keyUse))) ||
				(generateEncKids && ((null == keyUse) || KeyUse.ENCRYPTION.equals(keyUse))))
				? UUID.randomUUID().toString() : null;
			JWK generatedJWK;
			if (KeyType.EC.equals(keyType)) {
				ECKey base = PreGeneratedJwks.nextEcKey(env, curveForECKeys);
				ECKey.Builder b = new ECKey.Builder(base);
				if (keyUse != null) { b.keyUse(keyUse); }
				if (kid != null) { b.keyID(kid); }
				if (algorithm != null) { b.algorithm(algorithm); }
				generatedJWK = b.build();
			} else if (KeyType.RSA.equals(keyType)) {
				RSAKey base = PreGeneratedJwks.nextRsaKey(env, rsaKeySize);
				RSAKey.Builder b = new RSAKey.Builder(base);
				if (keyUse != null) { b.keyUse(keyUse); }
				if (kid != null) { b.keyID(kid); }
				if (algorithm != null) { b.algorithm(algorithm); }
				generatedJWK = b.build();
			} else if (KeyType.OKP.equals(keyType)) {
				OctetKeyPair base = PreGeneratedJwks.nextOkpKey(env, edCurve);
				OctetKeyPair.Builder b = new OctetKeyPair.Builder(base);
				if (keyUse != null) { b.keyUse(keyUse); }
				if (kid != null) { b.keyID(kid); }
				if (algorithm != null) { b.algorithm(algorithm); }
				generatedJWK = b.build();
			} else {
				throw new JOSEException("Unsupported key type: " + keyType);
			}
			allGeneratedKeys.add(generatedJWK);
			if (keyUse.equals(KeyUse.ENCRYPTION)) {
				encryptionKeysToBeUsed.add(generatedJWK);
			}

			if(i==whichKeyToUse && keyUse.equals(KeyUse.SIGNATURE)) {
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
		int index;
		// Bound is exclusive, so it must be keyCount (not keyCount - 1) to allow every key
		// index 0..keyCount-1 to be selectable; the old keyCount-1 never picked the last key
		// (and was deterministically 0 for keyCount == 2).
		index = random.ints(1, 0, keyCount).findFirst().getAsInt();
		return index;
	}


	public void setNumberOfRSAEncKeys(int numberOfRSAEncKeys) {
		this.numberOfRSAEncKeys = numberOfRSAEncKeys;
	}

	public void setNumberOfECEncKeys(int numberOfECEncKeys) {
		this.numberOfECEncKeys = numberOfECEncKeys;
	}

	public void setGenerateSigKids(boolean generateSigKids) {
		this.generateSigKids = generateSigKids;
	}

	public void setGenerateEncKids(boolean generateEncKids) {
		this.generateEncKids = generateEncKids;
	}

	public void setRsaKeySize(int rsaKeySize) {
		this.rsaKeySize = rsaKeySize;
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

	public int getNumberOfES256SigningKeys() {
		return numberOfES256SigningKeys;
	}

	public void setNumberOfES256SigningKeys(int numberOfES256SigningKeys) {
		this.numberOfES256SigningKeys = numberOfES256SigningKeys;
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

	public boolean isGenerateSigKids() {
		return generateSigKids;
	}

	public boolean isGenerateEncKids() {
		return generateEncKids;
	}

	public int getRsaKeySize() {
		return rsaKeySize;
	}

	public Curve getEdCurve() {
		return edCurve;
	}

	public void setEdCurve(Curve edCurve) {
		this.edCurve = edCurve;
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

	public int getNumberOfRSASigningKeysWithNoAlg() {
		return numberOfRSASigningKeysWithNoAlg;
	}

	public void setNumberOfRSASigningKeysWithNoAlg(int numberOfRSASigningKeysWithNoAlg) {
		this.numberOfRSASigningKeysWithNoAlg = numberOfRSASigningKeysWithNoAlg;
	}

	public int getNumberOfOKPSigningKeysWithNoAlg() {
		return numberOfOKPSigningKeysWithNoAlg;
	}

	public void setNumberOfOKPSigningKeysWithNoAlg(int numberOfOKPSigningKeysWithNoAlg) {
		this.numberOfOKPSigningKeysWithNoAlg = numberOfOKPSigningKeysWithNoAlg;
	}

	public int getNumberOfECCurveP256SigningKeysWithNoAlg() {
		return numberOfECCurveP256SigningKeysWithNoAlg;
	}

	public void setNumberOfECCurveP256SigningKeysWithNoAlg(int numberOfECCurveP256SigningKeysWithNoAlg) {
		this.numberOfECCurveP256SigningKeysWithNoAlg = numberOfECCurveP256SigningKeysWithNoAlg;
	}

	public int getNumberOfECCurveP256KSigningKeysWithNoAlg() {
		return numberOfECCurveSECP256KSigningKeysWithNoAlg;
	}

	public void setNumberOfECCurveSECP256KSigningKeysWithNoAlg(int numberOfECCurveSECP256KSigningKeysWithNoAlg) {
		this.numberOfECCurveSECP256KSigningKeysWithNoAlg = numberOfECCurveSECP256KSigningKeysWithNoAlg;
	}

	public Curve getEsCurve() {
		return esCurve;
	}

	public void setEsCurve(Curve esCurve) {
		this.esCurve = esCurve;
	}

	public Curve getEsKCurve() {
		return esKCurve;
	}

	public void setEsKCurve(Curve esKCurve) {
		this.esKCurve = esKCurve;
	}
}
