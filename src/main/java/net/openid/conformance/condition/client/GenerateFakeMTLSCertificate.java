package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

// BouncyCastle has deprecated X509V3CertificateGenerator in favor of org.bouncycastle.cert.X509v3CertificateBuilder
@SuppressWarnings("deprecation")
public class GenerateFakeMTLSCertificate extends AbstractFAPIBrazilExtractCertificateSubject {

	// loosely based on mitmDuplicateCertificate from
	// https://github.com/groupon/odo/blob/master/browsermob-proxy/src/main/java/com/groupon/odo/bmp/CertificateCreator.java

	public static final String OID_SUBJECT_KEY_IDENTIFIER = "2.5.29.14";
	public static final String OID_AUTHORITY_KEY_IDENTIFIER = "2.5.29.35";

	// We add these to the cert ourselves so shouldn't copy from original cert
	private static Set<String> clientCertOidsNeverToCopy = Set.of(
		OID_SUBJECT_KEY_IDENTIFIER,
		OID_AUTHORITY_KEY_IDENTIFIER
	);

	@Override
	@PreEnvironment(required = "mutual_tls_authentication")
	@PostEnvironment(required = "fake_mutual_tls_authentication")
	public Environment evaluate(Environment env) {
		var extensionOidsNotToCopy = new HashSet<String>();

		KeyPairGenerator generator;
		try {
			generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
		} catch (NoSuchAlgorithmException e) {
			throw error(e.getMessage(), e);
		}

		KeyPair kp = generator.generateKeyPair();

		KeyPair cakp = generator.generateKeyPair();

		PublicKey newPubKey = kp.getPublic();

		String certString = env.getString("mutual_tls_authentication", "cert");
		X509Certificate originalCert = generateCertificateFromMTLSCert(certString);

		X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();

		v3CertGen.setSubjectDN(originalCert.getSubjectX500Principal());
		v3CertGen.setSignatureAlgorithm(originalCert.getSigAlgName());
		v3CertGen.setPublicKey(newPubKey);
		v3CertGen.setNotAfter(originalCert.getNotAfter());
		v3CertGen.setNotBefore(originalCert.getNotBefore());
	 	v3CertGen.setIssuerDN(originalCert.getIssuerX500Principal());
		v3CertGen.setSerialNumber(originalCert.getSerialNumber());

		// copy other extensions:
		Set<String> critExts = originalCert.getCriticalExtensionOIDs();

		try {
		if(critExts != null) {
			for (String oid : critExts) {
				if(!clientCertOidsNeverToCopy.contains(oid)
					&& !extensionOidsNotToCopy.contains(oid)) {
						v3CertGen.copyAndAddExtension(new ASN1ObjectIdentifier(oid), true, originalCert);
				}
			}
		}
		Set<String> nonCritExs = originalCert.getNonCriticalExtensionOIDs();

		if(nonCritExs != null) {
			for(String oid: nonCritExs) {
				if(!clientCertOidsNeverToCopy.contains(oid)
					&& !extensionOidsNotToCopy.contains(oid)){
					v3CertGen.copyAndAddExtension(new ASN1ObjectIdentifier(oid), false, originalCert);
				}
			}
		}
		} catch (CertificateParsingException e) {
			throw error("x509 copyAndAddExtension failed", e);
		}

		JcaX509ExtensionUtils jcaX509ExtensionUtils = null;
		try {
			jcaX509ExtensionUtils = new JcaX509ExtensionUtils();
		} catch (NoSuchAlgorithmException e) {
			throw error("JcaX509ExtensionUtils failed", e);
		}
		v3CertGen.addExtension(X509Extension.subjectKeyIdentifier, false,
			jcaX509ExtensionUtils.createSubjectKeyIdentifier(newPubKey));

		X509Certificate cert;
		try {
		v3CertGen.addExtension(
			X509Extensions.AuthorityKeyIdentifier,
			false,
			new AuthorityKeyIdentifierStructure(cakp.getPublic()));

		var caPrivateKey = cakp.getPrivate();
			cert = v3CertGen.generate(caPrivateKey, "BC");
		} catch (CertificateEncodingException|NoSuchProviderException|NoSuchAlgorithmException|SignatureException|InvalidKeyException e) {
			throw error("cert.generate failed", e);
		}


		JsonObject mtls = new JsonObject();

		try {
			mtls.addProperty("cert", Base64.getEncoder().encodeToString(cert.getEncoded()));
		} catch (CertificateEncodingException e) {
			throw error("Error encoding certificate", e);
		}

		mtls.addProperty("key", Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded()));

		env.putObject("fake_mutual_tls_authentication", mtls);

		// we could add a ca cert too perhaps

		logSuccess("Generated our own client MTLS certificate based on the supplied one", args("fake_mutual_tls_authentication", mtls));

		return env;
	}

}
