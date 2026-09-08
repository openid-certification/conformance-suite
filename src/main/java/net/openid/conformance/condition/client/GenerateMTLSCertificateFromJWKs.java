package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import javax.security.auth.x500.X500Principal;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

// BouncyCastle has deprecated X509V3CertificateGenerator in favor of org.bouncycastle.cert.X509v3CertificateBuilder
@SuppressWarnings("deprecation")
public class GenerateMTLSCertificateFromJWKs extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client_jwks", strings = "client_name")
	@PostEnvironment(required = "mutual_tls_authentication")
	public Environment evaluate(Environment env) {

		JWKSet jwks;
		try {
			jwks = JWKSet.parse(env.getObject("client_jwks").toString());
		} catch (ParseException e) {
			throw error("Failed to parse JWKs", e);
		}

		JWK jwk = jwks.getKeys().get(0);
		KeyPair keyPair = toKeyPair(jwk);

		String clientName = env.getString("client_name");

		long now = System.currentTimeMillis();
		Date notBefore = new Date(now);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(notBefore);
		calendar.add(Calendar.YEAR, 1);
		Date notAfter = calendar.getTime();

		X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
		certGen.setSerialNumber(BigInteger.valueOf(now));
		certGen.setSubjectDN(new X500Principal("cn=" + clientName));
		certGen.setIssuerDN(new X500Principal("cn=" + clientName));
		certGen.setNotBefore(notBefore);
		certGen.setNotAfter(notAfter);
		certGen.setPublicKey(keyPair.getPublic());
		certGen.setSignatureAlgorithm(getSigningAlgorithm(jwk));
		certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(true));

		X509Certificate cert;
		try {
			cert = certGen.generate(keyPair.getPrivate(), "BC");
		} catch (CertificateEncodingException | InvalidKeyException | IllegalStateException
				| NoSuchProviderException | NoSuchAlgorithmException | SignatureException e) {
			throw error("Failed to generate certificate", e);
		}

		JsonObject mtls = new JsonObject();

		try {
			mtls.addProperty("cert", Base64.getEncoder().encodeToString(cert.getEncoded()));
		} catch (CertificateEncodingException e) {
			throw error("Error encoding certificate", e);
		}

		mtls.addProperty("key", Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));

		env.putObject("mutual_tls_authentication", mtls);

		logSuccess("Generated client MTLS certificate", args("mutual_tls_authentication", mtls));

		return env;
	}

	private String getSigningAlgorithm(JWK jwk) {
		Algorithm alg = jwk.getAlgorithm();
		if (JWSAlgorithm.Family.RSA.contains(alg)) {
			return "SHA256withRSA";
		} else if (JWSAlgorithm.Family.EC.contains(alg)) {
			return "SHA256withECDSA";
		} else {
			throw error("Unsupported algorithm: " + alg.getName());
		}
	}

	private KeyPair toKeyPair(JWK jwk) {
		try {
			Method toKeyPairMethod = jwk.getClass().getMethod("toKeyPair");
			Object keyPairObject = toKeyPairMethod.invoke(jwk);
			return KeyPair.class.cast(keyPairObject);
		} catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | ClassCastException e) {
			throw error("Failed to convert JWK to KeyPair", e);
		}
	}
}
