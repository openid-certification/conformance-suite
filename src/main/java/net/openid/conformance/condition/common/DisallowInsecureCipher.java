package net.openid.conformance.condition.common;


import com.google.common.collect.ImmutableList;
import org.bouncycastle.tls.CipherSuite;
import org.bouncycastle.tls.ProtocolVersion;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
public class DisallowInsecureCipher extends AbstractCheckInsecureCiphers {

	private static final List<Integer> ALLOWED_CIPHERS = ImmutableList.of(
		CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
		CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
		CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,
		CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384);

	private static final Map<Integer, String> CIPHER_NAMES = new HashMap<>();

	static {
		// Reflect on BouncyCastle to get a list of supported ciphers and names
		for (Field field : CipherSuite.class.getDeclaredFields()) {
			String name = field.getName();
			int modifiers = field.getModifiers();
			Class<?> type = field.getType();
			final int PUBLIC_STATIC_FINAL = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;

			if (type.isPrimitive()
				&& type.getName().equals("int")
				&& ((modifiers & PUBLIC_STATIC_FINAL) == PUBLIC_STATIC_FINAL)) {
				try {
					int cipherId = field.getInt(null);
					if (!CipherSuite.isSCSV(cipherId) && !ALLOWED_CIPHERS.contains(cipherId)) {
						CIPHER_NAMES.put(cipherId, name);
					}
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}


	@Override
	Map<Integer, String> getInsecureCiphers() {
		return CIPHER_NAMES;
	}

	@Override
	ProtocolVersion getProtocolVersion() {
		return ProtocolVersion.TLSv12;
	}
}
