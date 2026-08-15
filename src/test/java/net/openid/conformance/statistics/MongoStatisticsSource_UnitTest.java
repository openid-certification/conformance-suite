package net.openid.conformance.statistics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The host regex runs inside MongoDB's {@code $regexFind}; its engine and Java's agree on
 * the syntax used here, so this is what pins down which part of a URL the hosts table shows.
 */
class MongoStatisticsSource_UnitTest {

	private static final Pattern HOST = Pattern.compile(MongoStatisticsSource.HOST_OF_URL);

	@ParameterizedTest
	@CsvSource({
		"https://op.example.com/.well-known/openid-configuration, op.example.com",
		"http://op.example.com:8443/path?x=1#f, op.example.com",
		"https://op.example.com, op.example.com",
		"https://user:secret@op.example.com/, op.example.com",
		"https://user@op.example.com?x=a@b, op.example.com",
		"https://[::1]:8443/x, [::1]",
		"https://[2001:db8::1]/x, [2001:db8::1]",
	})
	void capturesTheHostAndNothingElse(String url, String host) {
		Matcher matcher = HOST.matcher(url);

		assertThat(matcher.find()).isTrue();
		assertThat(matcher.group(1)).isEqualTo(host);
	}

	@ParameterizedTest
	@CsvSource({"ftp://op.example.com/", "op.example.com", "https:///path", "''"})
	void doesNotMatchAnythingThatIsNotAnHttpUrlWithAHost(String url) {
		assertThat(HOST.matcher(url).find()).isFalse();
	}
}
