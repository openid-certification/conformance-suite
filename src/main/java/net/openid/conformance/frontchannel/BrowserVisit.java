package net.openid.conformance.frontchannel;

import com.google.gson.JsonArray;

/**
 * One scripted visit requested through {@link BrowserControl#goToUrl}: the url to open, the
 * "tasks" array of the matching "browser" configuration entry, the optional image placeholder the
 * scripted browser may fulfil, the HTTP method used for the initial request and how long to wait
 * before starting.
 */
record BrowserVisit(String url, JsonArray tasks, String placeholder, String method, int delaySeconds) {
}
