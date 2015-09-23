package fi.luomus.triplestore.utils;

import fi.luomus.commons.http.HttpClientService;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.session.SessionHandler;
import fi.luomus.triplestore.dao.TriplestoreDAO;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

public class LoginUtil  {

	private static class AuthenticationResponse {
		
		private final boolean success;
		private String errorMessage;
		private String userId;
		private String userFullname;
		private String userQname;
		private boolean isAdmin = false;
		
		public AuthenticationResponse(boolean success) {
			this.success = success;
		}

		public boolean successful() {
			return success;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public String getUserId() {
			return userId;
		}

		public String getUserFullname() {
			return userFullname;
		}

		public String getUserQname() {
			return userQname;
		}

		public boolean isForAdminUser() {
			return isAdmin;
		}

		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

		public void setUserFullname(String userFullname) {
			this.userFullname = userFullname;
		}

		public void setUserQname(String userQname) {
			this.userQname = userQname;
		}

		public void setAdmin(boolean isAdmin) {
			this.isAdmin = isAdmin;
		}
		
	}
	
	private final SessionHandler session; 
	private final ResponseData responseData; 
	private final String frontPage;
	private final ErrorReporter errorReporter;
	private final TriplestoreDAO dao;
	
	public LoginUtil(String frontPage, SessionHandler session, ResponseData responseData, ErrorReporter errorReporter, TriplestoreDAO dao) {
		this.session = session;
		this.responseData = responseData;
		this.frontPage = frontPage;
		this.errorReporter = errorReporter;
		this.dao = dao;
	}
	
	public static boolean authorized() {
		return true;
	}

	public ResponseData processGet(HttpServletRequest req) throws Exception {
		if (session.isAuthenticatedFor("triplestore")) {
			return responseData.setRedirectLocation(frontPage);
		} else {
			return responseData.setViewName("login").setData("originalURL", req.getParameter("originalURL"));
		}
	}

	public ResponseData processPost(HttpServletRequest req) throws Exception {
		String username = req.getParameter("username");
		String password = req.getParameter("password");
		String originalURL = req.getParameter("originalURL");

		responseData.setViewName("login").setData("originalURL", originalURL).setData("username", username);

		try {
			AuthenticationResponse authentication = authenticateViaKotkaAPI(username, password);
			if (authentication.successful()) {
				authenticateSession(req, authentication);
				if (given(originalURL)) {
					return responseData.setRedirectLocation(originalURL);
				} else {
					return responseData.setRedirectLocation(frontPage);
				}
			} else {
				responseData.setData("error", authentication.getErrorMessage());
				return responseData;
			}
		} catch (Exception e) {
			errorReporter.report("Login for username " + username, e);
			responseData.setData("error", e.getMessage());
			return responseData;
		} 
	}

	private boolean given(String s) {
		return s != null && s.trim().length() > 0;
	}

	private void authenticateSession(HttpServletRequest req, AuthenticationResponse authentication) throws Exception {
		session.authenticateFor("triplestore");
		session.setUserId(authentication.getUserId());
		session.setUserName(authentication.getUserFullname());
		session.put("user_qname", authentication.getUserQname());
		
		int userFK = dao.getUserFK(authentication.getUserQname());
		session.put("user_fk", Integer.toString(userFK)); // Tämä sitä varten, että jos (ja kun) AddStatement ja AddStatementL käytöstä luovutaan, voidaan pistää userfk statement tauluun
		
		if (authentication.isForAdminUser()) {
			session.put("role", "admin");
		}
		req.getSession().setMaxInactiveInterval(60 * 60 *3);
	}

	private AuthenticationResponse authenticateViaKotkaAPI(String username, String password) throws Exception {
		HttpClientService client = null;
		try {
			client = new HttpClientService();
			HttpPost postRequest = new HttpPost("https://kotka.luomus.fi/user/remote");
			List <NameValuePair> params = new ArrayList <NameValuePair>();
			params.add(new BasicNameValuePair("username", username));
			params.add(new BasicNameValuePair("password", password));
			postRequest.setEntity(new UrlEncodedFormEntity(params));
			
			JSONObject response = client.contentAsJson(postRequest);
			return handleAuthenticationResponse(response);
		} finally {
			if (client != null) client.close();
		}
	}

	private AuthenticationResponse handleAuthenticationResponse(JSONObject response) {
		// {"success":false,"message":"Invalid credentials","code":-3}
		// {"success":true,"identity":{"qname":"MA.5","username":"eopiirai","userGroups":["linuxlogin","hatikka","Computer Admins","mutikkalogin","Data Department Staff"],"email":"esko.piirainen@helsinki.fi","lastname":"Piirainen","firstname":"Esko Olavi","fullname":"Piirainen, Esko Olavi"}}
		// {"identity":{"MA.role":"MA.admin","links":[{"rel":"self","href":"/view?uri=http://id.luomus.fi/MA.5"}],"uri":"http://id.luomus.fi/MA.5","MA.emailAddress":["esko.piirainen@helsinki.fi"],"MA.description":null,"rdf:type":"MA.person","MA.yearOfBirth":"1981","MA.fullName":"Esko Piirainen","MA.hatikkaLoginName":"Esko.Piirainen@hatikka.fi","MA.inheritedName":"Piirainen","MA.givenNames":"Esko Olavi","MA.organisation":{"MOS.1016":"University of Helsinki, Finnish Museum of Natural History, General Services Unit, IT Team"},"MA.firstJoined":{"timezone":"Europe/Helsinki","timezone_type":3,"date":"2011-06-01 00:00:00"},"MA.preferredName":"Esko","MA.LTKMLoginName":"eopiirai"},"success":true}
		if (!response.getBoolean("success")) {
			AuthenticationResponse authenticationResponse = new AuthenticationResponse(false);
			authenticationResponse.setErrorMessage(response.getString("message"));
			return authenticationResponse;
		} else {
			AuthenticationResponse authenticationResponse = new AuthenticationResponse(true);
			JSONObject identity = response.getObject("identity");
			authenticationResponse.setUserId(identity.getString("MA.LTKMLoginName"));
			authenticationResponse.setUserQname(identity.getString("qname"));
			authenticationResponse.setUserFullname(identity.getString("MA.fullName"));
			setAdminStatus(authenticationResponse, identity);
			return authenticationResponse;
		}
	}

	private void setAdminStatus(AuthenticationResponse authenticationResponse, JSONObject identity) {
		if ("MA.admin".equals(identity.getString("MA.role"))) {
			authenticationResponse.setAdmin(true);
		}
	}

}
