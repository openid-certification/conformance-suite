package io.fintechlabs.testframework.condition.util;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jricher
 *
 */
public class PEMFormatter {

	private static final Pattern PEM_PATTERN = Pattern.compile("^-----BEGIN [^-]+-----$(.*?)^-----END [^-]+-----$", Pattern.MULTILINE | Pattern.DOTALL);

	public static String stripPEM(String in) throws IllegalArgumentException {

		Matcher m = PEM_PATTERN.matcher(in);

		if (m.find()) {

			// There may be multiple certificates, so concatenate and re-encode
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			do {
				String certStr = m.group(1).replaceAll("[\r\n]", "");
				byte[] cert = Base64.getDecoder().decode(certStr);
				out.write(cert, 0, cert.length);
			} while (m.find());

			return Base64.getEncoder().encodeToString(out.toByteArray());
		} else {

			// Assume it's a Base64-encoded DER format; check that it decodes OK
			Base64.getDecoder().decode(in);
			return in;
		}

	}

}
