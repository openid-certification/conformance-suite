package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
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
import net.openid.conformance.util.JWKUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class OIDCCGenerateServerJWKs extends AbstractCondition {
	public enum KeyTypeToUse { RSA, EC };
	protected KeyTypeToUse signingKeyTypeToUse = KeyTypeToUse.RSA;
	protected KeyTypeToUse encKeyTypeToUse = KeyTypeToUse.RSA;
	protected boolean generateRSAKeys = true;
	protected boolean generateECKeys = true;
	protected int numberOfRSASigningKeys = 2;
	protected int numberOfECSigningKeys = 2;
	protected int numberOfRSAEncKeys = 1;
	protected int numberOfECEncKeys = 1;
	protected boolean generateKids = true;
	protected int rsaKeySize = 2048;
	protected Curve ecCurve = Curve.P_256;
	protected List<JWK> allGeneratedKeys;
	protected List<JWK> signingKeyToBeUsed;
	protected List<JWK> encryptionKeysToBeUsed;
	protected JWSAlgorithm signingAlgorithmForRSAKeys = JWSAlgorithm.RS256;
	protected JWEAlgorithm encryptionAlgorithmForRSAKeys = JWEAlgorithm.RSA_OAEP_256;
	protected JWSAlgorithm signingAlgorithmForECKeys = JWSAlgorithm.ES256;
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

			if(generateRSAKeys) {
				createRSAKeys(KeyUse.SIGNATURE);
				createRSAKeys(KeyUse.ENCRYPTION);
			}
			if(generateECKeys) {
				createECKeys(KeyUse.SIGNATURE);
				createECKeys(KeyUse.ENCRYPTION);
			}

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

	protected void createRSAKeys(KeyUse keyUse) throws JOSEException {
		int whichKeyToUse = getIndexOfKeyToUse(keyUse);
		int loopMax = numberOfRSASigningKeys;
		if (keyUse == KeyUse.ENCRYPTION) {
			loopMax = numberOfRSAEncKeys;
		}

		for(int i=0; i<loopMax; i++) {
			RSAKeyGenerator rsaKeyGenerator = new RSAKeyGenerator(rsaKeySize);
			rsaKeyGenerator.keyUse(keyUse);
			if(generateKids) {
				rsaKeyGenerator.keyID(UUID.randomUUID().toString());
			}
			if(keyUse == KeyUse.SIGNATURE) {
				rsaKeyGenerator.algorithm(signingAlgorithmForRSAKeys);
			} else {
				rsaKeyGenerator.algorithm(encryptionAlgorithmForRSAKeys);
			}
			RSAKey rsaKey = rsaKeyGenerator.generate();
			allGeneratedKeys.add(rsaKey);
			if (keyUse == KeyUse.ENCRYPTION) {
				encryptionKeysToBeUsed.add(rsaKey);
			}

			if(i==whichKeyToUse && (keyUse==KeyUse.SIGNATURE && signingKeyTypeToUse == KeyTypeToUse.RSA)) {
				signingKeyToBeUsed.add(rsaKey);
			}
		}
	}

	protected void createECKeys(KeyUse keyUse) throws JOSEException {
		int whichKeyToUse = getIndexOfKeyToUse(keyUse);
		int loopMax = numberOfECSigningKeys;
		if (keyUse == KeyUse.ENCRYPTION) {
			loopMax = numberOfECEncKeys;
		}
		for(int i=0; i<loopMax; i++) {
			ECKeyGenerator ecKeyGenerator = new ECKeyGenerator(ecCurve);
			ecKeyGenerator.keyUse(keyUse);
			if(generateKids) {
				ecKeyGenerator.keyID(UUID.randomUUID().toString());
			}
			if(keyUse == KeyUse.SIGNATURE) {
				ecKeyGenerator.algorithm(signingAlgorithmForECKeys);
			} else {
				ecKeyGenerator.algorithm(encryptionAlgorithmForECKeys);
			}
			ECKey ecKey = ecKeyGenerator.generate();
			allGeneratedKeys.add(ecKey);
			if (keyUse == KeyUse.ENCRYPTION) {
				encryptionKeysToBeUsed.add(ecKey);
			}

			if(i==whichKeyToUse && (keyUse==KeyUse.SIGNATURE && signingKeyTypeToUse == KeyTypeToUse.EC)) {
				signingKeyToBeUsed.add(ecKey);
			}
		}
	}

	/**
	 * Returns a random key index
	 * Override to use a constant index
	 * @param keyUse
	 * @return
	 */
	protected int getIndexOfKeyToUse(KeyUse keyUse) {
		Random random = new Random();
		int index = 0;
		if(keyUse==KeyUse.SIGNATURE) {
			if(signingKeyTypeToUse==KeyTypeToUse.RSA) {
				if(numberOfRSASigningKeys>1) {
					index = random.ints(1, 0, numberOfRSASigningKeys - 1).findFirst().getAsInt();
				}
			} else {
				if(numberOfECSigningKeys>1) {
					index = random.ints(1, 0, numberOfECSigningKeys - 1).findFirst().getAsInt();
				}
			}
		} else {
			if(encKeyTypeToUse==KeyTypeToUse.RSA) {
				if(numberOfRSAEncKeys>1) {
					index = random.ints(1, 0, numberOfRSAEncKeys - 1).findFirst().getAsInt();
				}
			} else {
				if(numberOfECEncKeys>1) {
					index = random.ints(1, 0, numberOfECEncKeys - 1).findFirst().getAsInt();
				}
			}
		}
		return index;
	}

	public void setSigningKeyTypeToUse(KeyTypeToUse signingKeyTypeToUse)
	{
		this.signingKeyTypeToUse = signingKeyTypeToUse;
	}

	public void setEncKeyTypeToUse(KeyTypeToUse encKeyTypeToUse)
	{
		this.encKeyTypeToUse = encKeyTypeToUse;
	}

	public void setGenerateRSAKeys(boolean generateRSAKeys)
	{
		this.generateRSAKeys = generateRSAKeys;
	}

	public void setGenerateECKeys(boolean generateECKeys)
	{
		this.generateECKeys = generateECKeys;
	}

	public void setNumberOfRSASigningKeys(int numberOfRSASigningKeys)
	{
		this.numberOfRSASigningKeys = numberOfRSASigningKeys;
	}

	public void setNumberOfECSigningKeys(int numberOfECSigningKeys)
	{
		this.numberOfECSigningKeys = numberOfECSigningKeys;
	}

	public void setNumberOfRSAEncKeys(int numberOfRSAEncKeys)
	{
		this.numberOfRSAEncKeys = numberOfRSAEncKeys;
	}

	public void setNumberOfECEncKeys(int numberOfECEncKeys)
	{
		this.numberOfECEncKeys = numberOfECEncKeys;
	}

	public void setGenerateKids(boolean generateKids)
	{
		this.generateKids = generateKids;
	}

	public void setRsaKeySize(int rsaKeySize)
	{
		this.rsaKeySize = rsaKeySize;
	}

	public void setEcCurve(Curve ecCurve)
	{
		this.ecCurve = ecCurve;
	}

	public void setSigningAlgorithmForRSAKeys(JWSAlgorithm signingAlgorithmForRSAKeys)
	{
		this.signingAlgorithmForRSAKeys = signingAlgorithmForRSAKeys;
	}

	public void setEncryptionAlgorithmForRSAKeys(JWEAlgorithm encryptionAlgorithmForRSAKeys)
	{
		this.encryptionAlgorithmForRSAKeys = encryptionAlgorithmForRSAKeys;
	}

	public void setSigningAlgorithmForECKeys(JWSAlgorithm signingAlgorithmForECKeys)
	{
		this.signingAlgorithmForECKeys = signingAlgorithmForECKeys;
	}

	public void setEncryptionAlgorithmForECKeys(JWEAlgorithm encryptionAlgorithmForECKeys)
	{
		this.encryptionAlgorithmForECKeys = encryptionAlgorithmForECKeys;
	}
}
