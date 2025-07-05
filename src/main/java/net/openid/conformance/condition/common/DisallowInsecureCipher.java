package net.openid.conformance.condition.common;

import net.openid.conformance.util.FAPITLSClient;

import org.bouncycastle.tls.CipherSuite;
import org.bouncycastle.tls.ProtocolVersion;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@SuppressWarnings("deprecation")
public class DisallowInsecureCipher extends AbstractCheckInsecureCiphers {

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
					if (!CipherSuite.isSCSV(cipherId)) {
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
		// Obtain a list of allowed ciphers.
		List<Integer> allowed_ciphers = IntStream.of(FAPITLSClient.getTLS12Ciphers(useBCP195Ciphers())).boxed().collect( Collectors.toList());
		Map<Integer, String> insecure_cipher_names = new HashMap<>(CIPHER_NAMES);

		// Remove the allowed ciphers from the list of available ciphers.
		for (Integer cipherId: allowed_ciphers) {
			insecure_cipher_names.remove(cipherId);
		}

		return insecure_cipher_names;
	}

	@Override
	ProtocolVersion getProtocolVersion() {
		return ProtocolVersion.TLSv12;
	}

	protected boolean useBCP195Ciphers() {
		return false;
	}
}
