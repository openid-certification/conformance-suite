var FAPI_UI = {

	logTemplates : {},

	loadHomepageTemplates : function() {
		this.logTemplates.TEST_LAUNCH_BUTTON = _.template($("#indexTemplate_TestButton").html());
		this.logTemplates.RUNNING_TEST = _.template($("#indexTemplate_RunningTest").html());
		this.logTemplates.RUNNING_TEST_EXPOSED_KEY_VALUES = _.template($("#indexTemplate_RunningTestExposedKeyValues").html());
		this.logTemplates.RUNNING_TEST_EXTERNAL_URL = _.template($("#indexTemplate_RunningTestExternalURL").html());
	},
	// TO DO rename as loadLogDetailTemplates or some such...
	loadTemplates: function() {
		this.logTemplates.LOG_START = _.template($("#logDetailTemplate_LogStart").html());
		this.logTemplates.LOG_DETAIL = _.template($("#logDetailTemplate").html());
		this.logTemplates.SOURCE = _.template($("#logDetailTemplate_Source").html());
		this.logTemplates.MESSAGE = _.template($("#logDetailTemplate_Message").html());
		this.logTemplates.REQUIREMENTS = _.template($("#logDetailTemplate_Requirements").html());
		this.logTemplates.UPLOAD = _.template($("#logDetailTemplate_Upload").html());
		this.logTemplates.OWNER = _.template($("#logDetailTemplate_Owner").html());
		this.logTemplates.RESULT = _.template($("#logDetailTemplate_Result").html());
		this.logTemplates.TIME = _.template($("#logDetailTemplate_Time").html());
		this.logTemplates.MORE = _.template($("#logDetailTemplate_More").html());
		this.logTemplates.LOG_END = _.template($("#logDetailTemplate_LogEnd").html());
	},

	visibleFields : ["msg", "src", "time", "result", "requirements", "upload", "testOwner"],
	testResults : {passed:false, finished:false, success:0, warning:0, failure:0, interrupted:0, review:0, default:0, total:0},

    getUserInfoDiv : function( divToReplace ) {
		if (!('USER_INFO' in this.logTemplates)) {
			this.logTemplates.USER_INFO = _.template($("#userInfoTemplate").html());
		}
		// get the current user info
		return $.getJSON({
			url: '/currentuser',
			context: this
		}).done(function(userInfo) {
            this.currentUser = userInfo;
            $(divToReplace).html(this.logTemplates.USER_INFO({userInfo: userInfo}));
            $('[data-toggle="tooltip"]').tooltip();
        });
	},

	getTooltipHelp : function(value) {
		var info = "";
		if (value) {
			switch (value.toUpperCase()) {

				case "WAITING":
					info = "The test is waiting for an external callback, for example, for the authorisation server to redirect back to it. In some cases this means the authorisation server did not redirect back to the conformance suite, indicating that the test failed.";
				break;

				case "INTERRUPTED":
					info = "The test failed to run to completion as a critical element failed. Please see the log, fix the error and run the test again to get a complete set of results.";
				break;

				default:
					info="";
				break;
			}
		}
		return info;
	},
	
	/**
	 * 
	 */
	loadAvailableLogs : function() {
		$.ajax({ 
	        type: 'GET', 
	        url: "/log", 
	        data: {}, 
	        success: render
	    });
	},
	
	/**
	 * Takes in a JSON object representing the error from the server and shows an error display
	 */
	showError : function(error) {
		if (error != null) {
			$('#errorMessage').html(_.escape(error.error));
		} else {
			$('#errorMessage').html('Error from server.');
		}
		
		$('#errorModal').modal();
	},

	testJSON : {}/*
				server : {
					discoveryUrl: ""
				},
				client : {
					client_id: "",
					client_secret:"",
					redirect_uri:"",
					scope: "",
					jwks: {
						keys:[]
					}
				},
				jwks: {
					keys:[]
				},
				tls : {
					testHost: "",
					testPort: ""
				}
			}*/

};


