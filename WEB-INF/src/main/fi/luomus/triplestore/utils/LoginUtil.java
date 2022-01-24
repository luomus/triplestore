package fi.luomus.triplestore.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.session.SessionHandler;
import fi.luomus.commons.utils.Utils;
import fi.luomus.lajiauth.model.AuthenticationEvent;
import fi.luomus.lajiauth.model.AuthenticationSource;
import fi.luomus.lajiauth.model.UserDetails;
import fi.luomus.lajiauth.service.LajiAuthClient;

public class LoginUtil  {

	private static class AuthenticationResult {

		private final boolean success;
		private String errorMessage;
		private String userId;
		private String userFullname;
		private String userQname;
		private String personToken;
		private String next;
		private Set<String> roles;

		public AuthenticationResult(boolean success) {
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

		public AuthenticationResult setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
			return this;
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

		public String getNext() {
			return next;
		}

		public void setNext(String next) {
			this.next = next;
		}

		public String getPersonToken() {
			return personToken;
		}

		public void setPersonToken(String personToken) {
			this.personToken = personToken;
		}

		public Set<String> getRoles() {
			return roles;
		}

		public void setRoles(Set<String> roles) {
			if (roles == null) roles = Collections.emptySet();
			this.roles = roles;
		}

	}

	private final Map<String, String> frontpageForRoles;
	private final ErrorReporter errorReporter;
	private final Config config;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Set<String> allowedRoles;

	public LoginUtil(Map<String, String> frontpageForRoles, Config config, ErrorReporter errorReporter, Set<String> allowedRoles) {
		this.config = config;
		this.frontpageForRoles = frontpageForRoles;
		this.errorReporter = errorReporter;
		this.allowedRoles = allowedRoles;
	}

	public ResponseData processGet(HttpServletRequest req, SessionHandler session, ResponseData responseData) throws Exception {
		if (session.isAuthenticatedFor(config.systemId())) {
			return responseData.setRedirectLocation(getFrontPage(session));
		}
		String next = req.getParameter("next");
		if (next == null) next = "";
		setLajiAuthLinks(next, responseData);
		return responseData.setViewName("login");
	}

	private String getFrontPage(SessionHandler session) {
		@SuppressWarnings("unchecked")
		Set<String> roles = (Set<String>) session.getObject("roles");
		if (roles == null || roles.isEmpty()) {
			session.invalidate();
			throw new IllegalStateException();
		}
		return frontpageForRoles.get(roles.iterator().next());
	}

	private void setLajiAuthLinks(String next, ResponseData responseData) throws URISyntaxException {
		LajiAuthClient client = getLajiAuthClient();
		URI hakaURI = client.createLoginUrl(next).authenticationSource(AuthenticationSource.HAKA).build();
		URI virtuURI = client.createLoginUrl(next).authenticationSource(AuthenticationSource.VIRTU).build();
		URI lajifiURI = client.createLoginUrl(next).authenticationSource(AuthenticationSource.LOCAL).build();

		responseData.setData("hakaURI", hakaURI.toString());
		responseData.setData("virtuURI", virtuURI.toString());
		responseData.setData("lajifiURI", lajifiURI.toString());
		responseData.setData("lajiAuthLoginURI", client.createLoginUrl(next).build());
	}

	public ResponseData processPost(HttpServletRequest req, SessionHandler session, ResponseData responseData) throws Exception {
		String lajiAuthToken = req.getParameter("token");
		responseData.setViewName("login");
		setLajiAuthLinks("", responseData);
		AuthenticationResult authentication = authenticateViaLajiAuthentication(lajiAuthToken);
		try {
			if (authentication.successful()) {
				authenticateSession(session, authentication);
				if (nextGiven(authentication)) {
					return responseData.setRedirectLocation(config.baseURL() + authentication.getNext());
				}
				String frontpage = frontpageForRoles.get(authentication.getRoles().iterator().next());
				return responseData.setRedirectLocation(frontpage);
			}
			responseData.setData("error", authentication.getErrorMessage());
			return responseData;
		} catch (Exception e) {
			errorReporter.report("Login data " + Utils.debugS(lajiAuthToken), e);
			responseData.setData("error", "Something went wrong: " + e.getMessage());
			return responseData;
		}
	}

	private boolean nextGiven(AuthenticationResult authentication) {
		String next = authentication.getNext();
		if (!given(next)) return false;
		if (next.equals("/")) return false;
		return true;
	}

	private void authenticateSession(SessionHandler session, AuthenticationResult authentication) throws Exception {
		session.authenticateFor(config.systemId());
		session.setUserId(authentication.getUserId());
		session.setUserName(authentication.getUserFullname());
		session.put("user_qname", authentication.getUserQname());
		session.put("person_token", authentication.getPersonToken());
		session.setObject("roles", authentication.getRoles());
		session.setTimeout(60 * 42);
	}

	private AuthenticationResult authenticateViaLajiAuthentication(String token) throws Exception {
		AuthenticationEvent authorizationInfo = null;
		try {
			LajiAuthClient client = getLajiAuthClient();
			authorizationInfo = client.getAndValidateAuthenticationInfo(token);
			// Validation throws exception if something is wrong; Authentication has been successful:

			return authenticationResultFromLajiAuth(authorizationInfo, token);
		} catch (Exception e) {
			if (authorizationInfo != null) {
				errorReporter.report("Erroreous LajiAuth login for " + Utils.debugS(token, objectMapper.writeValueAsString(authorizationInfo)), e);
			} else {
				errorReporter.report("Unsuccesful LajiAuth login for " + token, e);
			}
			AuthenticationResult authenticationResult = new AuthenticationResult(false);
			authenticationResult.setErrorMessage(e.getMessage());
			return authenticationResult;
		}
	}

	private AuthenticationResult authenticationResultFromLajiAuth(AuthenticationEvent authenticationEvent, String token) {
		AuthenticationResult authenticationResponse = new AuthenticationResult(true);
		UserDetails userDetails = authenticationEvent.getUser();
		if (!validForSystem( userDetails)) return new AuthenticationResult(false).setErrorMessage("Required permissions to to use this system are missing.");
		authenticationResponse.setPersonToken(token);
		authenticationResponse.setRoles(userDetails.getRoles());

		// For easy testing of different roles
		//Set<String> overridingRoles = Utils.set("MA.taxonEditorUserDescriptionWriterOnly");
		//authenticationResponse.setRoles(overridingRoles);

		authenticationResponse.setUserId(userDetails.getQname().get());
		authenticationResponse.setUserQname(userDetails.getQname().get());
		authenticationResponse.setUserFullname(userDetails.getName());
		authenticationResponse.setNext(authenticationEvent.getNext());
		return authenticationResponse;
	}

	private boolean validForSystem(UserDetails userDetails) {
		if (!userDetails.getQname().isPresent()) return false;
		if (!hasAllowedRole(userDetails)) return false;
		return true;
	}

	private boolean hasAllowedRole(UserDetails userDetails) {
		for (String allowedRole : allowedRoles) {
			if (userDetails.getRoles().contains(allowedRole)) {
				return true;
			}
		}
		return false;
	}

	private LajiAuthClient getLajiAuthClient() throws URISyntaxException {
		return new LajiAuthClient(config.get("SystemQname"), new URI(config.get("LajiAuthURL")));
	}

	private boolean given(String s) {
		return s != null && s.trim().length() > 0;
	}
}
