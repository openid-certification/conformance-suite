package net.openid.conformance.condition.util;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PEMFormatter {

	private static final Pattern PEM_PATTERN = Pattern.compile("^-----BEGIN [^-]+-----$(.*?)^-----END [^-]+-----$", Pattern.MULTILINE | Pattern.DOTALL);

	public static List<String> extractPEMHeader(String in) {

		Matcher m = PEM_PATTERN.matcher(in);

		List<String> headerList = new ArrayList<>();
		if (m.find()) {
			do {
				headerList.add(in.substring(m.start(), m.start(1)));
			} while(m.find());
		}

		return headerList;
	}

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
