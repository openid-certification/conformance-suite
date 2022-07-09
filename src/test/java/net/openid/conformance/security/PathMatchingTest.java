package net.openid.conformance.security;

import net.openid.conformance.runner.TestDispatcher;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.AntPathMatcher;

public class PathMatchingTest {

	AntPathMatcher pathMatcher = new AntPathMatcher();

	String originalPattern = TestDispatcher.TEST_PATH + "**";
	String patternWithExclusions = TestDispatcherCorsConfiguration.TEST_PATH_WITH_CORS_EXCLUSIONS;

	@Test
	public void the_exclusion_pattern_excludes_paths_that_end_with_authorize() throws Exception {


		Assert.assertTrue(pathMatcher.match(originalPattern, "/test/a/b/authorize"));
		Assert.assertFalse(pathMatcher.match(patternWithExclusions, "/test/a/b/authorize"));

		Assert.assertTrue(pathMatcher.match(originalPattern, "/test/a/b/authorize"));
		Assert.assertFalse(pathMatcher.match(patternWithExclusions, "/test/a/b/authorize"));

		Assert.assertTrue(pathMatcher.match(originalPattern, "/test/a/b/authorize"));
		Assert.assertFalse(pathMatcher.match(patternWithExclusions, "/test/a/b/authorize"));

		Assert.assertTrue(pathMatcher.match(originalPattern, "/test/authorize"));
		Assert.assertFalse(pathMatcher.match(patternWithExclusions, "/test/authorize"));
	}

	@Test
	public void the_exclusion_pattern_excludes_paths_that_end_with_end_session_endpoint() throws Exception {

		Assert.assertTrue(pathMatcher.match(originalPattern, "/test/a/b/end_session_endpoint"));
		Assert.assertFalse(pathMatcher.match(patternWithExclusions, "/test/a/b/end_session_endpoint"));

		Assert.assertTrue(pathMatcher.match(originalPattern, "/test/a/b/end_session_endpoint"));
		Assert.assertFalse(pathMatcher.match(patternWithExclusions, "/test/a/b/end_session_endpoint"));

		Assert.assertTrue(pathMatcher.match(originalPattern, "/test/a/b/end_session_endpoint"));
		Assert.assertFalse(pathMatcher.match(patternWithExclusions, "/test/a/b/end_session_endpoint"));

		Assert.assertTrue(pathMatcher.match(originalPattern, "/test/end_session_endpoint"));
		Assert.assertFalse(pathMatcher.match(patternWithExclusions, "/test/end_session_endpoint"));
	}

	@Test
	public void the_exclusion_pattern_does_not_exclude_paths_that_do_not_end_with_authorize() throws Exception {

		Assert.assertTrue(pathMatcher.match(originalPattern, "/test/a/b/"));
		Assert.assertTrue(pathMatcher.match(patternWithExclusions, "/test/a/b/"));

		Assert.assertTrue(pathMatcher.match(originalPattern, "/test/a/fintech-clienttest/.well-known/openid-configuration"));
		Assert.assertTrue(pathMatcher.match(patternWithExclusions, "/test/a/fintech-clienttest/.well-known/openid-configuration"));
	}

	@Test
	public void the_exclusion_pattern_does_not_exclude_paths_that_contain_authorize_as_a_proper_substring() throws Exception {

		Assert.assertTrue(pathMatcher.match(originalPattern, "/test/authorize/b/"));
		Assert.assertTrue(pathMatcher.match(patternWithExclusions, "/test/authorize/b/"));

		Assert.assertTrue(pathMatcher.match(originalPattern, "/test/a/fintech-authorize/.well-known/openid-configuration"));
		Assert.assertTrue(pathMatcher.match(patternWithExclusions, "/test/a/fintech-authorize/.well-known/openid-configuration"));
	}

	@Test
	// Using the term "directory" in the test name because that's what AntPathMatcher does:
	// https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/util/AntPathMatcher.html
	public void the_exclusion_pattern_excludes_paths_without_additional_directories() throws Exception {

		// The original pattern includes it, the excluding pattern doesn't, even if it doesn't end with authorize
		Assert.assertTrue(pathMatcher.match(originalPattern, "/test"));
		Assert.assertFalse(pathMatcher.match(patternWithExclusions, "/test"));

		// The original pattern includes it, the excluding pattern doesn't, even if it doesn't end with authorize
		Assert.assertTrue(pathMatcher.match(originalPattern, "/test/"));
		Assert.assertFalse(pathMatcher.match(patternWithExclusions, "/test/"));
	}
}
