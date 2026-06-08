package net.openid.conformance.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates BCP47 subtags against a bundled snapshot of the IANA Language Subtag Registry
 * (src/main/resources/iana/language-subtag-registry.txt). Only the canonical forms
 * (language lowercase, region uppercase, script Title-case) are stored — callers should
 * pass the values returned by {@link java.util.Locale#getLanguage()},
 * {@link java.util.Locale#getCountry()}, etc., which already match those forms.
 */
public final class Bcp47SubtagRegistry {

	private static final String RESOURCE_PATH = "/iana/language-subtag-registry.txt";

	private static final class Holder {
		private static final Bcp47SubtagRegistry INSTANCE = new Bcp47SubtagRegistry();
	}

	private final Set<String> languages;
	private final Set<String> regions;
	private final Set<String> scripts;
	private final Set<String> variants;
	private final String fileDate;

	private Bcp47SubtagRegistry() {
		Set<String> langs = new HashSet<>();
		Set<String> regs = new HashSet<>();
		Set<String> scr = new HashSet<>();
		Set<String> vars = new HashSet<>();
		String date = null;
		try (InputStream is = Bcp47SubtagRegistry.class.getResourceAsStream(RESOURCE_PATH)) {
			if (is == null) {
				throw new IllegalStateException("IANA Language Subtag Registry resource not found at " + RESOURCE_PATH);
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				String type = null;
				String subtag = null;
				for (String line = reader.readLine(); line != null; line = reader.readLine()) {
					if (line.equals("%%")) {
						addEntry(type, subtag, langs, regs, scr, vars);
						type = null;
						subtag = null;
					} else if (line.startsWith("File-Date: ")) {
						date = line.substring("File-Date: ".length()).trim();
					} else if (line.startsWith("Type: ")) {
						type = line.substring("Type: ".length()).trim();
					} else if (line.startsWith("Subtag: ")) {
						subtag = line.substring("Subtag: ".length()).trim();
					}
				}
				addEntry(type, subtag, langs, regs, scr, vars);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load IANA Language Subtag Registry", e);
		}
		languages = Collections.unmodifiableSet(langs);
		regions = Collections.unmodifiableSet(regs);
		scripts = Collections.unmodifiableSet(scr);
		variants = Collections.unmodifiableSet(vars);
		fileDate = date;
	}

	public static Bcp47SubtagRegistry getInstance() {
		// Lazy initialization-on-demand holder: the ~49k-line IANA registry is parsed only when a
		// locale check first needs it, not eagerly at class-load.
		return Holder.INSTANCE;
	}

	public boolean isRegisteredLanguage(String subtag) {
		return languages.contains(subtag);
	}

	public boolean isRegisteredRegion(String subtag) {
		return regions.contains(subtag);
	}

	public boolean isRegisteredScript(String subtag) {
		return scripts.contains(subtag);
	}

	public boolean isRegisteredVariant(String subtag) {
		return variants.contains(subtag);
	}

	public String getFileDate() {
		return fileDate;
	}

	private static void addEntry(String type, String subtag, Set<String> langs, Set<String> regs, Set<String> scr, Set<String> vars) {
		if (type == null || subtag == null) {
			return;
		}
		Set<String> target = switch (type) {
			case "language" -> langs;
			case "region" -> regs;
			case "script" -> scr;
			case "variant" -> vars;
			default -> null;
		};
		if (target == null) {
			return;
		}
		if (subtag.contains("..")) {
			String[] parts = subtag.split("\\.\\.");
			if (parts.length == 2 && parts[0].length() == parts[1].length()) {
				expandRange(target, parts[0], parts[1]);
			}
		} else {
			target.add(subtag);
		}
	}

	private static void expandRange(Set<String> target, String from, String to) {
		if (from.charAt(0) != to.charAt(0)) {
			// All current IANA registry ranges (qaa..qtz, Qaaa..Qabx, QM..QZ, XA..XZ) hold
			// position 0 fixed; the increment loop below relies on that to keep carries from
			// propagating past it. If a future registry adds a range that violates this,
			// surface it loudly so the limitation gets revisited rather than silently
			// emitting wrong subtags or looping forever.
			throw new IllegalStateException(
				"Bcp47SubtagRegistry range '" + from + ".." + to
					+ "' would carry past position 0; expandRange does not support this. Update the parser.");
		}
		String current = from;
		target.add(current);
		while (!current.equals(to)) {
			current = increment(current);
			target.add(current);
		}
	}

	private static String increment(String s) {
		char[] chars = s.toCharArray();
		for (int i = chars.length - 1; i >= 0; i--) {
			char c = chars[i];
			if (c == 'z') {
				chars[i] = 'a';
			} else if (c == 'Z') {
				chars[i] = 'A';
			} else {
				chars[i] = (char) (c + 1);
				return new String(chars);
			}
		}
		return new String(chars);
	}
}
