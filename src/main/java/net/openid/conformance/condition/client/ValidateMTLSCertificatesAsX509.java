package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;

import static java.util.stream.Collectors.toCollection;

public class ValidateMTLSCertificatesAsX509 extends AbstractCondition {

	@Override
	@PreEnvironment(required = "mutual_tls_authentication")
	public Environment evaluate(Environment env) {
		String certString = env.getString("mutual_tls_authentication", "cert");
		String keyString = env.getString("mutual_tls_authentication", "key");
		String caString = env.getString("mutual_tls_authentication", "ca");

		if (Strings.isNullOrEmpty(certString) || Strings.isNullOrEmpty(keyString)) {
			throw error("Couldn't find TLS client certificate or key for MTLS");
		}

		CertificateFactory certFactory = null;
		try {
			certFactory = CertificateFactory.getInstance("X.509", BouncyCastleProviderSingleton.getInstance());
		} catch (CertificateException | IllegalArgumentException e) {
			throw error("Couldn't get CertificateFactory", e);
		}

		X509Certificate certificate = generateCertificateFromMTLSCert(certString, certFactory);

		validateMTLSKey(certString, keyString, certificate);

		if (!Strings.isNullOrEmpty(caString)) {
			validateMTLSCa(env, certFactory, caString);
		}

		logSuccess("Mutual TLS authentication cert validated as X.509");

		return env;
	}

	protected static PrivateKey generateAlgPrivateKeyFromDER(String alg, byte[] keyBytes) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		try {
			// try to generate private key using PKCS8, works for both RSA and EC and Ed25519 alg
			// RSA alg will handle both PKCS1 and PKCS8 format here
			// EC alg will throw exception for PKCS1, Ed25519 not possible with PKCS1
			KeySpec kspec = new PKCS8EncodedKeySpec(keyBytes);
			return KeyFactory.getInstance(alg, BouncyCastleProviderSingleton.getInstance()).generatePrivate(kspec);
		} catch (InvalidKeySpecException e) {
			if("EC".equals(alg)) {
				// try to generate private key using PKCS1
				ASN1Sequence seq = ASN1Sequence.getInstance(keyBytes);
				org.bouncycastle.asn1.sec.ECPrivateKey pKey = org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(seq);
				AlgorithmIdentifier algId = new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, pKey.getParametersObject());
				byte[] server_pkcs8 = new PrivateKeyInfo(algId, pKey).getEncoded();
				return KeyFactory.getInstance(alg, BouncyCastleProviderSingleton.getInstance()).generatePrivate(new PKCS8EncodedKeySpec(server_pkcs8));
			}
			throw e;
		}
	}


	private X509Certificate generateCertificateFromMTLSCert(String certString, CertificateFactory certFactory) {
		byte[] decodedCert;
		try {
			decodedCert = Base64.getDecoder().decode(certString);
		} catch (IllegalArgumentException e) {
			throw error("base64 decode of cert failed", e, args("cert", certString));
		}

		X509Certificate certificate;
		try {
			certificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(decodedCert));
		} catch (CertificateException | IllegalArgumentException e) {
			throw error("Calling generateCertificate on cert failed", e, args("cert", certString));
		}
		return certificate;
	}

	private void validateMTLSKey(String certString, String keyString, X509Certificate certificate) {
		byte[] decodedKey;
		try {
			decodedKey = Base64.getDecoder().decode(keyString);
		} catch (IllegalArgumentException e) {
			throw error("base64 decode of key failed", e, args("key", keyString));
		}

		PublicKey publicKey = certificate.getPublicKey();
		String alg = publicKey.getAlgorithm();

		if ("RSA".equals(alg)) {
			verifyRSAPrivateKey(certString, keyString, decodedKey, certificate);
		} else if ("EC".equals(alg) ) {
			verifyECPrivateKey(certString, keyString, decodedKey, certificate);
		} else if ("Ed25519".equals(alg)) { // alg can be EdDSA with Java default provider
			verifyEd25519PrivateKey(certString, keyString, decodedKey, certificate);
		} else {
			throw error("The private key format is not support. You need to provide a private key which is RSA or EC or EdDSA with Ed25519 curve");
		}

	}

	private void verifyRSAPrivateKey(String certString, String keyString, byte[] decodedKey, X509Certificate certificate) {
		RSAPrivateKey privateKey;
		try {
			privateKey = (RSAPrivateKey) generateAlgPrivateKeyFromDER("RSA", decodedKey);
		} catch (InvalidKeySpecException | IllegalArgumentException | NoSuchAlgorithmException | IOException e) {
			throw error("Couldn't generate RSA private key", e, args("key", keyString));
		}

		// Check that the private key and the certificate match
		RSAPublicKey rsaPublicKey = (RSAPublicKey) certificate.getPublicKey();
		if (!privateKey.getModulus().equals(rsaPublicKey.getModulus())) {
			throw error("MTLS Private Key and Cert do not match", args("cert", certString, "key", keyString));
		}
	}

	private void verifyECPrivateKey(String certString, String keyString, byte[] decodedKey, X509Certificate certificate) {
		PrivateKey privateKey;
		try {
			privateKey = generateAlgPrivateKeyFromDER("EC",decodedKey);
		} catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException e) {
			throw error("Couldn't generate EC private key", e, args("key", keyString));
		}

		// Check that the private key and the certificate match
		// This check isn't sure yet
		ECPublicKey ecPublicKey = (ECPublicKey) certificate.getPublicKey();
		if (!((ECPrivateKey) privateKey).getParameters().equals(ecPublicKey.getParameters())) {
			throw error("MTLS Private Key and Cert do not match", args("cert", certString, "key", keyString));
		}
	}

	private void verifyEd25519PrivateKey(String certString, String keyString, byte[] decodedKey, X509Certificate certificate) {
		PrivateKey privateKey;
		try {
			privateKey = generateAlgPrivateKeyFromDER("Ed25519",decodedKey);
		} catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw error("Couldn't generate Ed25519 private key", e, args("key", keyString));
		}

		// Check that the private key and the certificate match
		PublicKey pubKey = certificate.getPublicKey();
		BCEdDSAPublicKey ecPublicKey = (BCEdDSAPublicKey) pubKey;
		if (!((BCEdDSAPrivateKey) privateKey).getPublicKey().equals(ecPublicKey)) {
			throw error("MTLS Private Key and Cert do not match", args("cert", certString, "key", keyString));
		}
	}


	private void validateMTLSCa(Environment env, CertificateFactory certFactory, String caString) {
		byte[] decodedCa;
		try {
			decodedCa = Base64.getDecoder().decode(caString);
			Collection<? extends Certificate> caCertificateChain = certFactory.generateCertificates(new ByteArrayInputStream(decodedCa));
			ArrayList<Certificate> caCertificateChainList = caCertificateChain.stream().collect(toCollection(ArrayList::new));

			boolean isWrongOrder = false;
			for (int i = 0; i < caCertificateChainList.size(); i++) {
				X509Certificate x509Certificate = (X509Certificate) caCertificateChainList.get(i);
				if (isSelfSigned(x509Certificate) && i < caCertificateChainList.size() - 1) {
					caCertificateChainList.remove(i);
					caCertificateChainList.add(x509Certificate);
					isWrongOrder = true;
					break;
				}
			}

			if (isWrongOrder) {
				log("Root & issuing in mtls.ca is wrong order. Automatically correct it (Issuing first, then root)");

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				for (Certificate certificate : caCertificateChainList) {
					out.write(certificate.getEncoded(), 0, certificate.getEncoded().length);
				}

				String newCaString = Base64.getEncoder().encodeToString(out.toByteArray());
				JsonObject mtls = env.getObject("mutual_tls_authentication");
				mtls.addProperty("ca", newCaString);
				env.putObject("mutual_tls_authentication", mtls);
			}
		} catch (IllegalArgumentException e) {
			throw error("base64 decode of ca failed", e, args("ca", caString));
		} catch (CertificateException | NoSuchAlgorithmException | NoSuchProviderException e) {
			throw error("Couldn't validate ca cert", e, args("ca", caString));
		}
	}

	private boolean isSelfSigned(X509Certificate cert) throws CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
		try {
			// Try to verify certificate signature with its own public key
			PublicKey key = cert.getPublicKey();
			cert.verify(key);
			return true;
		} catch (SignatureException | InvalidKeyException e) {
			return false;
		}
	}
}
