package net.openid.conformance.frontchannel;

import com.google.gson.JsonArray;

/**
 * One scripted visit requested through {@link BrowserControl#goToUrl}: the url to open, the
 * "tasks" array of the matching "browser" configuration entry, the optional image placeholder the
 * scripted browser may fulfil, the HTTP method used for the initial request, how long to wait
 * before starting, and the visit's number among the scripted visits of its test (from 1, in the
 * order the test requested them).
 */
record BrowserVisit(String url, JsonArray tasks, String placeholder, String method, int delaySeconds, int number) {
}
