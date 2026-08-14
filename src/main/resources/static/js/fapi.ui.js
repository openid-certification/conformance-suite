var FAPI_UI = {

		logTemplates : {},

		loadScheduleTestPageTemplates : function() {
			const p1 = fetch('templates/testOption.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.TEST_OPTION = _.template(data);
				});

			const p2 = fetch('templates/testOptGroup.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.TEST_OPTGROUP = _.template(data);
				});

			const p3 = fetch('templates/userinfo.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.USER_INFO = _.template(data);
				});

			const promises = [p1, p2, p3];

			return Promise.allSettled(promises);
		},

		loadRunningTestPageTemplates : function() {
			const p1 = fetch('templates/runningTest.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.RUNNING_TEST = _.template(data);
				});

			const p2 = fetch('templates/owner.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.OWNER = _.template(data);
				});

			const p3 = fetch('templates/statusAndResult.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.TEST_STATUS = _.template(data);
				});

			const p4 = fetch('templates/userinfo.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.USER_INFO = _.template(data);
				});

			const promises = [p1, p2, p3, p4];

			return Promise.allSettled(promises);
		},

		availablePlans : {},

		getUserInfo : function() {
			// get the current user info
			return fetch("/api/currentuser")
				.then((response) => {
					if (! response.ok) {
						// Tag the error with the status so the catch can
						// distinguish expected-401 from real failures.
						const err = new Error("Network response was not OK");
						err.status = response.status;
						throw err;
					}

					return response.json();
				})
				.then((userInfo) => {
					this.currentUser = userInfo;
					// Pages using <cts-navbar> no longer have #userInfoHolder;
					// guard against its absence while still setting currentUser above.
					const elem = document.getElementById('userInfoHolder');
					if (elem && FAPI_UI.logTemplates.USER_INFO) {
						elem.innerHTML = FAPI_UI.logTemplates.USER_INFO({userInfo: userInfo});
					}
				})
				.catch((error) => {
					// 401 is the expected "not logged in" path — stay quiet.
					// Anything else (network error, 5xx) should log so operators
					// can diagnose silent breakage.
					if (error && error.status !== 401) {
						console.warn("[fapi.ui.js getUserInfo] /api/currentuser failed:", error);
					}
				});

		},

		getStatusHelp : function(value) {
			switch (value ? value.toLowerCase() : undefined) {
				case "waiting":
					return "The test is waiting for something to happen. For example, for the authorization server to redirect back to it, for the user to visit a link or for the user to upload an image (see the test description for details). In some cases this means the authorization server did not redirect back to the conformance suite, indicating that the test failed.";
				case "configured":
					return "The test has successfully setup the initial environment. Please read the test description and when ready press the 'START' button to begin the test.";
				case "interrupted":
					return "The test failed to run to completion as a critical element failed. Please see the log, fix the error and run the test again to get a complete set of results.";
				case "finished":
					return "The test has run to completion.";
				case "running":
					return "The test is actively executing. Reload this page to see the latest status.";
				default:
					return "";
			}
		},

		getResultHelp : function(value) {
			switch (value ? value.toLowerCase() : undefined) {
				case "passed":
					return "The test has passed all conditions.";
				case "failed":
					return "The test has failed at least one critical condition. This means an important error has been detected and the system under test cannot be certified.";
				case "warning":
					return "The test has generated some warnings during its execution, see the log for details. Test results with warnings are accepted for certification, but they generally indicate that the software under test is behaving unexpected or not following recommendations, and the tester should check the results to ensure any warnings are expected behaviour of the software being tested.";
				case "review":
					return "The test requires manual review, for example it contains images that need to be manually checked. These images will be checked by the certification team when a certification request is submitted.";
				case "skipped":
					return "The test could not be completed due to configuration or optional features. Please check if the feature being tested is supported, if it is please check the configuration of the test and of the software under test. If the feature being tested is not supported by the software under test then skipped tests do not prevent certification.";
				default:
					return "";

			}
		},

		/**
		 * Call show()/hide() on a cts-modal, defensively.
		 *
		 * cts-modal is a lazily-upgraded custom element: until its module has
		 * evaluated, `getElementById` returns a plain unknown element with no
		 * show()/hide(), and the page CSS hides `cts-modal:not(:defined)` so
		 * nothing is on screen either. Calling through would throw a
		 * TypeError — which, on the error path, means the error the modal was
		 * meant to report gets swallowed instead. Returns whether the call
		 * landed so callers can fall back to another surface.
		 *
		 * "Not upgraded" is not the only way this fails, so the guard covers
		 * three shapes rather than one:
		 *
		 *  - method missing (element never upgraded);
		 *  - method throws — the native `dialog.showModal()` underneath
		 *    raises InvalidStateError when the dialog is not connected;
		 *  - method silently no-ops — cts-modal's show() early-returns when
		 *    its internal `_dialog` is missing, e.g. after a partial
		 *    connectedCallback failure. For 'show' we therefore trust the
		 *    host's mirrored `open` attribute rather than the mere absence of
		 *    an exception, since "returned normally" is not the same as
		 *    "something is on screen" — and reporting success for an invisible
		 *    modal is the exact swallow this guard exists to prevent.
		 */
		callModal : function(id, method) {
			const el = document.getElementById(id);
			if (!el || typeof el[method] !== 'function') {
				return false;
			}
			try {
				el[method]();
			} catch (e) {
				console.warn('[fapi.ui.js] ' + id + '.' + method + '() threw; falling back', e);
				return false;
			}
			return method === 'show' ? el.hasAttribute('open') : true;
		},

		/**
		 * Takes in a JSON object representing the error from the server and shows an error display
		 */
		showError : function(error) {
			const elem = document.getElementById('errorMessage');
			var msg = 'Error from server.';

			if (error != null) {
				msg = error.error || error.code;
				if (/^\d+$/.test(msg)) {
					// Tomcat considers that HTTP status messages should not be sent,
					// so we get unhelpful responses like "HTTP/1.1 404 404". Make it
					// clear that this is an HTTP error code.
					msg = "HTTP Error " + msg;
				}
				if (error.message) {
					msg += " : " + error.message
				}
				var msgHtml = _.escape(msg);
				if (error.error == "Unauthorized") {
					msgHtml += "<br><br>Refresh the page to renew your session";
				}
				if (elem) elem.innerHTML = msgHtml;
			} else if (elem) {
				elem.innerHTML = msg;
			}

			FAPI_UI.hideBusy(); // only one modal at a time

			// Whichever surface wins, the previous fallback toast must go —
			// otherwise a stale duration-0 toast sits beside the new modal.
			FAPI_UI.dismissErrorToast();

			if (FAPI_UI.callModal('errorModal', 'show')) {
				return;
			}

			// No usable modal — degrade to a toast, then to alert(), so a
			// server error is never reported to an empty screen. The modal
			// body carries the session hint as markup; the plain-text
			// surfaces have to state it themselves or the user is told
			// "Unauthorized" with no idea that a refresh fixes it.
			if (error != null && error.error == "Unauthorized") {
				msg += " Refresh the page to renew your session.";
			}

			const toast = window.ctsToast;
			if (typeof toast === 'function') {
				// The modal is a singleton the rest of this file keeps to
				// "only one at a time"; the toast has to be tracked by hand to
				// honour the same invariant (any previous one was already
				// dismissed above). duration 0 = no auto-dismiss, standing in
				// for a modal the user would have dismissed.
				FAPI_UI._errorToast = toast({ title: 'Error', message: msg, kind: 'error', duration: 0 });
			} else {
				window.alert(msg);
			}
		},

		/** Handle on the fallback error toast, so hideError() can clear it. */
		_errorToast : null,

		dismissErrorToast : function() {
			const toast = FAPI_UI._errorToast;
			FAPI_UI._errorToast = null;
			if (toast && typeof toast.dismiss === 'function') {
				toast.dismiss();
			}
		},

		hideError : function() {
			// Clears whichever surface actually rendered the last error.
			FAPI_UI.dismissErrorToast();
			FAPI_UI.callModal('errorModal', 'hide');
		},

		showBusy : function(label, message) {
			if (!label) {
				label = "Loading...";
			}

			// The title node is rendered BY cts-modal (from its `heading`
			// attribute), so it does not exist at all until the component has
			// upgraded — hence the null guards rather than a bare write.
			var elem = document.getElementById('loadingModal-title');
			if (elem) elem.innerHTML = _.escape(label);
			elem = document.getElementById('loadingMessage');
			if (elem) elem.innerHTML = _.escape(message);

			FAPI_UI.hideError(); // only one modal at a time

			FAPI_UI.callModal('loadingModal', 'show');
		},

		hideBusy : function() {
			FAPI_UI.callModal('loadingModal', 'hide');
		},

		selectedVariant: undefined

};
