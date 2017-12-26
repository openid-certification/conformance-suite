var FAPI_UI = {

	logTemplates : {},

    isAdmin : [[${#authorization.expression('hasRole(''ROLE_ADMIN'')')}]],

	getUserInfoDiv: function(divName) {


		userInfoHTML = '<div>Logged in as [[${displayName}]]';
		if (this.isAdmin) {
			userInfoHTML += ' <span class="bg-danger">ADMIN</span>';
		}

		userInfoHTML +=	'</div><form action="/logout" method="post" class="form-inline">' +
			'<input type="submit" class="btn btn-sm btn-primary" value="Logout">' +
			'</form>';
		//console.log(userInfoHTML);
		$(divName).html(userInfoHTML);
		$(divName).attr("data-toggle","tooltip");
        $(divName).attr("title",[[${principal}]]);
		$('[data-toggle="tooltip"]').tooltip();
	},

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
		this.logTemplates.RESULT = _.template($("#logDetailTemplate_Result").html());
		this.logTemplates.TIME = _.template($("#logDetailTemplate_Time").html());
		this.logTemplates.MORE = _.template($("#logDetailTemplate_More").html());
		this.logTemplates.LOG_END = _.template($("#logDetailTemplate_LogEnd").html());
	},

	visibleFields : ["msg", "src", "time", "result", "requirements", "upload"],
	testResults : {success:0, warning:0, failure:0, interrupted:0, review:0, default:0, total:0},
	
	/**
	 * 
	 */
	loadAvailableLogs : function() {
		$.ajax({ 
	        type: 'GET', 
	        url: "log", 
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


