package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.CertificateList;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * Builds CRLs for the mdoc DS certificate CRL checks: by default a Table B.10 conformant,
 * empty CRL issued by the suite's IACA, with each method breaking one thing.
 */
final class MdocCrlTestFixtures {

	private MdocCrlTestFixtures() {
		// utility class
	}

	static CrlBuilder iacaCrl() throws Exception {
		return new CrlBuilder();
	}

	/** Stores {@code der} where {@code FetchMdocDsCertificateCrl} leaves a fetched CRL. */
	static void putCrl(Environment env, byte[] der) {
		env.putString(AbstractMdocDsCertificateCrlCondition.ENV_CRL, Base64.getEncoder().encodeToString(der));
	}

	static final class CrlBuilder {
		private X500Name issuer = TestKeysAndCerts.getIacaRootCertHolder().getSubject();
		private PrivateKey signer = TestKeysAndCerts.getIacaRootPrivateKey();
		private String signatureAlgorithm = "SHA256withECDSA";
		private Date thisUpdate = Date.from(Instant.now().minus(Duration.ofMinutes(1)));
		private Date nextUpdate = Date.from(Instant.now().plus(Duration.ofDays(7)));
		private byte[] authorityKeyIdentifier = TestKeysAndCerts.getIacaRootSubjectKeyIdentifier();
		private BigInteger crlNumber = BigInteger.valueOf(42);
		private final List<BigInteger> revokedSerials = new ArrayList<>();
		private boolean entryExtensions;
		private boolean emptyRevokedList;
		private final List<Extension> extraExtensions = new ArrayList<>();

		CrlBuilder issuer(String dn) {
			issuer = new X500Name(dn);
			return this;
		}

		CrlBuilder signer(PrivateKey key) {
			signer = key;
			return this;
		}

		CrlBuilder signatureAlgorithm(String jcaName) {
			signatureAlgorithm = jcaName;
			return this;
		}

		CrlBuilder thisUpdate(Instant at) {
			thisUpdate = Date.from(at);
			return this;
		}

		CrlBuilder nextUpdate(Instant at) {
			nextUpdate = at == null ? null : Date.from(at);
			return this;
		}

		CrlBuilder authorityKeyIdentifier(byte[] value) {
			authorityKeyIdentifier = value;
			return this;
		}

		CrlBuilder withoutCrlNumber() {
			crlNumber = null;
			return this;
		}

		/** Encoded as a bare INTEGER, so values {@code CRLNumber} refuses, such as negative ones, can be built. */
		CrlBuilder crlNumber(BigInteger value) {
			crlNumber = value;
			return this;
		}

		CrlBuilder revoke(BigInteger serial) {
			revokedSerials.add(serial);
			return this;
		}

		/** Gives each revoked entry a reasonCode entry extension, which Table B.10 forbids. */
		CrlBuilder withEntryExtensions() {
			entryExtensions = true;
			return this;
		}

		/** Encodes an empty revokedCertificates SEQUENCE, which Table B.10 forbids and the builder never emits. */
		CrlBuilder withEmptyRevokedList() {
			emptyRevokedList = true;
			return this;
		}

		CrlBuilder extension(ASN1ObjectIdentifier oid, boolean critical, ASN1Encodable value) throws Exception {
			extraExtensions.add(Extension.create(oid, critical, value));
			return this;
		}

		byte[] build() throws Exception {
			X509v2CRLBuilder builder = new X509v2CRLBuilder(issuer, thisUpdate);
			if (nextUpdate != null) {
				builder.setNextUpdate(nextUpdate);
			}
			for (BigInteger serial : revokedSerials) {
				if (entryExtensions) {
					builder.addCRLEntry(serial, thisUpdate, CRLReason.keyCompromise);
				} else {
					builder.addCRLEntry(serial, thisUpdate, 0);
				}
			}
			if (authorityKeyIdentifier != null) {
				builder.addExtension(Extension.authorityKeyIdentifier, false,
					new AuthorityKeyIdentifier(authorityKeyIdentifier));
			}
			if (crlNumber != null) {
				builder.addExtension(Extension.cRLNumber, false, new ASN1Integer(crlNumber));
			}
			for (Extension extension : extraExtensions) {
				builder.addExtension(extension);
			}
			byte[] der = builder.build(new JcaContentSignerBuilder(signatureAlgorithm).build(signer)).getEncoded();
			return emptyRevokedList ? withEmptyRevokedCertificates(der) : der;
		}

		/** Re-signs {@code der} with an empty revokedCertificates SEQUENCE inserted after nextUpdate. */
		private byte[] withEmptyRevokedCertificates(byte[] der) throws Exception {
			CertificateList list = CertificateList.getInstance(der);
			ASN1Sequence tbs = ASN1Sequence.getInstance(list.getTBSCertList().toASN1Primitive());
			ASN1EncodableVector fields = new ASN1EncodableVector();
			for (int i = 0; i < tbs.size(); i++) {
				ASN1Encodable field = tbs.getObjectAt(i);
				if (field instanceof ASN1TaggedObject) {
					fields.add(new DERSequence());
				}
				fields.add(field);
			}
			DERSequence newTbs = new DERSequence(fields);
			Signature signature = Signature.getInstance(signatureAlgorithm);
			signature.initSign(signer);
			signature.update(newTbs.getEncoded());
			return new DERSequence(new ASN1Encodable[] {
				newTbs, list.getSignatureAlgorithm(), new DERBitString(signature.sign()) }).getEncoded();
		}
	}
}
