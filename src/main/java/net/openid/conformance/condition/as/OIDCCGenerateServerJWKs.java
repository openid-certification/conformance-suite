package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

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
			createKeys(numberOfRSASigningKeysWithNoAlg, KeyType.RSA, KeyUse.SIGNATURE, null, null);
			createKeys(numberOfECCurveP256SigningKeysWithNoAlg, KeyType.EC, KeyUse.SIGNATURE, null, esCurve);
			createKeys(numberOfECCurveSECP256KSigningKeysWithNoAlg, KeyType.EC, KeyUse.SIGNATURE, null, esKCurve);
			createKeys(numberOfOKPSigningKeysWithNoAlg, KeyType.OKP, KeyUse.SIGNATURE, null, null);

			createKeys(numberOfRSSigningKeys, KeyType.RSA, KeyUse.SIGNATURE, rsSigningAlgorithm, null);
			createKeys(numberOfES256SigningKeys, KeyType.EC, KeyUse.SIGNATURE, esSigningAlgorithm, esCurve);
			createKeys(numberOfPSSigningKeys, KeyType.RSA, KeyUse.SIGNATURE, psSigningAlgorithm, null);
			createKeys(numberOfEdSigningKeys, KeyType.OKP, KeyUse.SIGNATURE, JWSAlgorithm.EdDSA, null);

			createKeys(numberOfRSAEncKeys, KeyType.RSA, KeyUse.ENCRYPTION, encryptionAlgorithmForRSAKeys, null);
			createKeys(numberOfECEncKeys, KeyType.EC, KeyUse.ENCRYPTION, encryptionAlgorithmForECKeys, esCurve);

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
	protected void createKeys(int keyCount, KeyType keyType, KeyUse keyUse, Algorithm algorithm, Curve curveForECKeys) throws JOSEException {
		if(keyCount<1) {
			return;
		}
		int whichKeyToUse = getIndexOfKeyToUse(keyCount);

		for(int i=0; i<keyCount; i++) {
			JWKGenerator<? extends JWK> jwkGenerator = null;
			if (KeyType.EC.equals(keyType)) {
				jwkGenerator = new ECKeyGenerator(curveForECKeys);
				jwkGenerator.provider(BouncyCastleProviderSingleton.getInstance());
			} else if (KeyType.RSA.equals(keyType)) {
				jwkGenerator = new RSAKeyGenerator(rsaKeySize);
			} else if (KeyType.OKP.equals(keyType)) {
				jwkGenerator = new OctetKeyPairGenerator(edCurve);
			}
			if(keyUse!=null) {
				jwkGenerator.keyUse(keyUse);
			}
			if( (generateSigKids && ((null == keyUse) || KeyUse.SIGNATURE.equals(keyUse))) ||
				(generateEncKids && ((null == keyUse) || KeyUse.ENCRYPTION.equals(keyUse)))) {
				jwkGenerator.keyID(UUID.randomUUID().toString());
			}
			if(algorithm!=null) {
				jwkGenerator.algorithm(algorithm);
			}

			JWK generatedJWK = jwkGenerator.generate();
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
		index = random.ints(1, 0, keyCount - 1).findFirst().getAsInt();
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
