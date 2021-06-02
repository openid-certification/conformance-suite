package net.openid.conformance.validation;

import java.util.regex.Pattern;

public class RegexMatch implements Match {

	private final Pattern pattern;

	private RegexMatch(String regex) {
		this.pattern = Pattern.compile(regex);
	}

	@Override
	public boolean matches(String value) {
		return pattern.matcher(value).matches();
	}

	@Override
	public boolean matches(Number value) {
		return matches(String.valueOf(value));
	}

	@Override
	public String toString() {
		return "Pattern: " + pattern.pattern();
	}

	public static Match regex(String pattern) {
		return new RegexMatch(pattern);
	}
}
