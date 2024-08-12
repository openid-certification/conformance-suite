package net.openid.conformance.security;

import net.openid.conformance.runner.TestDispatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

public class PathMatchingTest {

	AntPathMatcher pathMatcher = new AntPathMatcher();

	String originalPattern = TestDispatcher.TEST_PATH + "**";
	String patternWithExclusions = TestDispatcherCorsConfiguration.TEST_PATH_WITH_CORS_EXCLUSIONS;

	@Test
	public void the_exclusion_pattern_excludes_paths_that_end_with_authorize() throws Exception {


		Assertions.assertTrue(pathMatcher.match(originalPattern, "/test/a/b/authorize"));
		Assertions.assertFalse(pathMatcher.match(patternWithExclusions, "/test/a/b/authorize"));

		Assertions.assertTrue(pathMatcher.match(originalPattern, "/test/a/b/authorize"));
		Assertions.assertFalse(pathMatcher.match(patternWithExclusions, "/test/a/b/authorize"));

		Assertions.assertTrue(pathMatcher.match(originalPattern, "/test/a/b/authorize"));
		Assertions.assertFalse(pathMatcher.match(patternWithExclusions, "/test/a/b/authorize"));

		Assertions.assertTrue(pathMatcher.match(originalPattern, "/test/authorize"));
		Assertions.assertFalse(pathMatcher.match(patternWithExclusions, "/test/authorize"));
	}

	@Test
	public void the_exclusion_pattern_excludes_paths_that_end_with_logout_endpoints() throws Exception {

		Assertions.assertTrue(pathMatcher.match(originalPattern, "/test/a/b/end_session_endpoint"));
		Assertions.assertFalse(pathMatcher.match(patternWithExclusions, "/test/a/b/end_session_endpoint"));

		Assertions.assertTrue(pathMatcher.match(originalPattern, "/test/a/b/check_session_iframe"));
		Assertions.assertFalse(pathMatcher.match(patternWithExclusions, "/test/a/b/check_session_iframe"));

		Assertions.assertTrue(pathMatcher.match(originalPattern, "/test/a/b/get_session_state"));
		Assertions.assertFalse(pathMatcher.match(patternWithExclusions, "/test/a/b/get_session_state"));
	}

	@Test
	public void the_exclusion_pattern_does_not_exclude_paths_that_do_not_end_with_authorize() throws Exception {

		Assertions.assertTrue(pathMatcher.match(originalPattern, "/test/a/b"));
		Assertions.assertTrue(pathMatcher.match(patternWithExclusions, "/test/a/b"));

		Assertions.assertTrue(pathMatcher.match(originalPattern, "/test/a/fintech-clienttest/.well-known/openid-configuration"));
		Assertions.assertTrue(pathMatcher.match(patternWithExclusions, "/test/a/fintech-clienttest/.well-known/openid-configuration"));
	}

	@Test
	public void the_exclusion_pattern_does_not_exclude_paths_that_contain_authorize_as_a_proper_substring() throws Exception {

		Assertions.assertTrue(pathMatcher.match(originalPattern, "/test/authorize/b"));
		Assertions.assertTrue(pathMatcher.match(patternWithExclusions, "/test/authorize/b"));

		Assertions.assertTrue(pathMatcher.match(originalPattern, "/test/a/fintech-authorize/.well-known/openid-configuration"));
		Assertions.assertTrue(pathMatcher.match(patternWithExclusions, "/test/a/fintech-authorize/.well-known/openid-configuration"));
	}

	@Test
	// Using the term "directory" in the test name because that's what AntPathMatcher does:
	// https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/util/AntPathMatcher.html
	public void the_exclusion_pattern_excludes_paths_without_additional_directories() throws Exception {

		// The original pattern includes it, the excluding pattern doesn't, even if it doesn't end with authorize
		Assertions.assertTrue(pathMatcher.match(originalPattern, "/test"));
		Assertions.assertFalse(pathMatcher.match(patternWithExclusions, "/test"));

		// The original pattern includes it, the excluding pattern doesn't, even if it doesn't end with authorize
		Assertions.assertTrue(pathMatcher.match(originalPattern, "/test/"));
		Assertions.assertFalse(pathMatcher.match(patternWithExclusions, "/test/"));
	}
}
