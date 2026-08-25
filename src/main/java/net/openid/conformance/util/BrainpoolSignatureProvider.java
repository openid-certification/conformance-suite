package net.openid.conformance.util;

import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import org.bouncycastle.asn1.teletrust.TeleTrusTNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.security.interfaces.ECKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.EllipticCurve;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A JCA provider that makes ECDSA signatures over brainpool curves work for code that calls
 * the provider-less {@code Signature.getInstance("SHA*withECDSA")} - notably the multipaz
 * library (COSE_Sign1 verification, VICAL trust chain evaluation, X509Cert verification),
 * which offers no provider hook, and the JDK's own X.509/PKIX certificate validation.
 *
 * <p>The JVM's SunEC provider does not support brainpool curves, but crucially it accepts a
 * brainpool key at {@code initVerify}/{@code initSign} time and only fails at
 * {@code verify()}/{@code sign()}. JCA "delayed provider selection" settles on the first
 * provider that accepts the key at init, so SunEC captures brainpool keys and then fails,
 * even with BouncyCastle registered (appended) - real-world example: the Aptitude-signed
 * Geneva interop VICAL, whose signer certificate uses brainpoolP256r1.
 *
 * <p>This provider is installed at position 1 (see {@link #ensureInstalled()}) and registers
 * only the three SHA-2 ECDSA signature algorithms - everything mdoc/COSE/JWS/PKIX as used by
 * this suite needs; SHA-224 and SHA-3 variants are deliberately not intercepted to keep the
 * position-1 footprint minimal. Its SPI throws {@link InvalidKeyException} at
 * init for any key that is not on a brainpool curve, which makes delayed provider selection
 * fall through to SunEC exactly as before - so behaviour for P-256/P-384/P-521 (TLS
 * handshakes, JWS, PKIX) is unchanged. For brainpool keys - previously a guaranteed
 * failure - it delegates to the BouncyCastle implementation already on the classpath.
 *
 * <p>Known limitation: fall-through relies on delayed provider selection, so a provider-less
 * {@code Signature} whose provider is fixed <em>before</em> init ({@code getProvider()},
 * {@code getParameters()} or {@code setParameter()} called first) fails a later non-brainpool
 * init instead of falling through. The SPI cannot distinguish that case from selection-time
 * probing (both arrive as the first {@code engineInit*} call on a fresh instance), and
 * accepting all keys instead would make this provider capture every ECDSA operation in the
 * JVM. No known ECDSA caller uses the pre-init pattern (it is an RSASSA-PSS idiom, and PSS
 * is not registered here); the behaviour is pinned by a unit test.
 */
public final class BrainpoolSignatureProvider extends Provider {

	public static final String NAME = "OIDFBrainpoolECDSA";

	private static final long serialVersionUID = 1L;

	private static final String[][] ALGORITHMS = {
		{ "SHA256withECDSA", "1.2.840.10045.4.3.2" },
		{ "SHA384withECDSA", "1.2.840.10045.4.3.3" },
		{ "SHA512withECDSA", "1.2.840.10045.4.3.4" },
	};

	private record BrainpoolCurve(EllipticCurve curve, BigInteger order) {}

	/**
	 * Every brainpool curve BouncyCastle knows, keyed by field prime - so the non-brainpool
	 * keys that dominate {@link #isBrainpoolKey} calls are rejected with one map lookup.
	 */
	private static final Map<BigInteger, List<BrainpoolCurve>> BRAINPOOL_CURVES = loadBrainpoolCurves();

	public BrainpoolSignatureProvider() {
		super(NAME, "1.0", "Delegates brainpool-curve ECDSA to BouncyCastle; rejects other keys so JCA falls through to the usual providers");
		for (String[] algorithm : ALGORITHMS) {
			String name = algorithm[0];
			String oid = algorithm[1];
			putService(new Service(this, "Signature", name, BrainpoolDelegatingSignatureSpi.class.getName(),
					List.of(oid, "OID." + oid), null) {
				@Override
				public Object newInstance(Object constructorParameter) {
					return new BrainpoolDelegatingSignatureSpi(name);
				}
			});
		}
	}

	/**
	 * The suite's crypto-provider bootstrap, called from {@code Application.main} and the
	 * {@code AbstractTestModule} static initializer: installs this provider at position 1
	 * (highest priority) and registers BouncyCastle (appended) - each only if not already
	 * present.
	 */
	public static synchronized void ensureInstalled() {
		if (Security.getProvider(NAME) == null) {
			Security.insertProviderAt(new BrainpoolSignatureProvider(), 1);
		}
		if (Security.getProvider("BC") == null) {
			Security.addProvider(BouncyCastleProviderSingleton.getInstance());
		}
	}

	private static Map<BigInteger, List<BrainpoolCurve>> loadBrainpoolCurves() {
		Map<BigInteger, List<BrainpoolCurve>> curves = new HashMap<>();
		Enumeration<?> names = TeleTrusTNamedCurves.getNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			if (!name.startsWith("brainpool")) {
				continue;
			}
			X9ECParameters params = TeleTrusTNamedCurves.getByName(name);
			BigInteger p = params.getCurve().getField().getCharacteristic();
			EllipticCurve curve = new EllipticCurve(new ECFieldFp(p),
				params.getCurve().getA().toBigInteger(), params.getCurve().getB().toBigInteger());
			curves.computeIfAbsent(p, k -> new ArrayList<>()).add(new BrainpoolCurve(curve, params.getN()));
		}
		curves.replaceAll((p, list) -> List.copyOf(list));
		return Map.copyOf(curves);
	}

	/**
	 * True if the key is an EC key on a brainpool curve. Matches by curve parameters (field
	 * prime, coefficients, order), not by name - keys parsed by other providers carry no
	 * curve name.
	 */
	static boolean isBrainpoolKey(Key key) {
		if (!(key instanceof ECKey ecKey)) {
			return false;
		}
		ECParameterSpec spec = ecKey.getParams();
		if (spec == null) {
			return false;
		}
		EllipticCurve curve = spec.getCurve();
		if (!(curve.getField() instanceof ECFieldFp field)) {
			return false;
		}
		List<BrainpoolCurve> candidates = BRAINPOOL_CURVES.get(field.getP());
		if (candidates == null) {
			return false;
		}
		for (BrainpoolCurve bp : candidates) {
			if (bp.curve().equals(curve) && bp.order().equals(spec.getOrder())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Thrown at init for every non-brainpool key so JCA delayed provider selection falls
	 * through to the next provider. Pure control flow on a hot path (the first init of every
	 * provider-less SHA*withECDSA Signature in the JVM, including TLS handshakes and JWS
	 * verification), so no stack trace is captured.
	 */
	private static final class NotBrainpoolKeyException extends InvalidKeyException {
		private static final long serialVersionUID = 1L;

		NotBrainpoolKeyException() {
			super("Not a brainpool-curve EC key; deferring to the standard providers. If this exception "
				+ "reached you, this Signature's provider was likely fixed before init (getProvider(), "
				+ "getParameters() or setParameter() called first) so JCA could not fall through - "
				+ "request a provider explicitly via Signature.getInstance(algorithm, provider)");
		}

		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}
	}

	/** Accepts only brainpool keys, delegating the actual ECDSA to BouncyCastle. */
	public static final class BrainpoolDelegatingSignatureSpi extends SignatureSpi {

		private final String algorithm;
		private Signature delegate;

		BrainpoolDelegatingSignatureSpi(String algorithm) {
			this.algorithm = algorithm;
		}

		private Signature delegate() throws InvalidKeyException {
			if (delegate == null) {
				try {
					delegate = Signature.getInstance(algorithm, BouncyCastleProviderSingleton.getInstance());
				} catch (NoSuchAlgorithmException e) {
					// BC always provides SHA*withECDSA; this cannot happen in practice
					throw new InvalidKeyException("BouncyCastle does not provide " + algorithm, e);
				}
			}
			return delegate;
		}

		private void requireBrainpool(Key key) throws InvalidKeyException {
			if (!isBrainpoolKey(key)) {
				throw new NotBrainpoolKeyException();
			}
		}

		@Override
		protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
			requireBrainpool(publicKey);
			delegate().initVerify(publicKey);
		}

		@Override
		protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
			requireBrainpool(privateKey);
			delegate().initSign(privateKey);
		}

		@Override
		protected void engineInitSign(PrivateKey privateKey, SecureRandom random) throws InvalidKeyException {
			requireBrainpool(privateKey);
			delegate().initSign(privateKey, random);
		}

		// the update/sign/verify engine methods are only invoked after a successful
		// engineInit* (the Signature front-end enforces this), so delegate is non-null there

		@Override
		protected void engineUpdate(byte b) throws SignatureException {
			delegate.update(b);
		}

		@Override
		protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {
			delegate.update(b, off, len);
		}

		@Override
		protected byte[] engineSign() throws SignatureException {
			return delegate.sign();
		}

		@Override
		protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
			return delegate.verify(sigBytes);
		}

		@Override
		protected void engineSetParameter(AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
			try {
				delegate().setParameter(params);
			} catch (InvalidKeyException e) {
				throw new InvalidAlgorithmParameterException(e);
			}
		}

		@Override
		protected AlgorithmParameters engineGetParameters() {
			return delegate == null ? null : delegate.getParameters();
		}

		@Deprecated
		@Override
		protected void engineSetParameter(String param, Object value) {
			throw new UnsupportedOperationException("engineSetParameter(String, Object) is deprecated and unsupported");
		}

		@Deprecated
		@Override
		protected Object engineGetParameter(String param) {
			throw new UnsupportedOperationException("engineGetParameter(String) is deprecated and unsupported");
		}
	}
}
