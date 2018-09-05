/**
 * Author : Jayant Kumar
 */
package au.edu.anu.portal.portlets.sakaiconnector;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletSession;
import javax.portlet.PortletURL;
import javax.portlet.ReadOnlyException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ValidatorException;
import javax.servlet.http.HttpServletRequest;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import au.edu.anu.portal.portlets.sakaiconnector.helper.SakaiWebServiceHelper;
import au.edu.anu.portal.portlets.sakaiconnector.logic.SakaiWebServiceLogic;
import au.edu.anu.portal.portlets.sakaiconnector.models.Site;
import au.edu.anu.portal.portlets.sakaiconnector.support.CollectionsSupport;
import au.edu.anu.portal.portlets.sakaiconnector.support.HttpSupport;
import au.edu.anu.portal.portlets.sakaiconnector.support.OAuthSupport;
import au.edu.anu.portal.portlets.sakaiconnector.utils.Constants;
import au.edu.anu.portal.portlets.sakaiconnector.utils.Messages;



public class PortletDispatcher extends GenericPortlet{
	
	// pages
	private String viewUrl;
	private String editUrl;
	private String proxyUrl;
	private String errorUrl;
	private String configUrl;
	
	// params
	private String key;
	private String secret;
	private String endpoint;
	private String adminUsername;
	private String adminPassword;
	private String loginUrl;
	private String scriptUrl;
	private String allowedTools;
	
	
	private String portletHeight;
	private String portletTitle;
	private String remoteSiteId;
	private String remoteToolId;
	
	//attribute mappings
	private String attributeMappingForUsername;
	
	// local
	private boolean replayForm;
	private boolean isValid;
	
	//caches
	private Cache cache;
	CacheManager cm ;
	PortletSession session ;
	public void init(PortletConfig config) throws PortletException {	   
	   super.init(config);
	   log.info("init() i1");
	   cm = CacheManager.newInstance();
	   log.info("CacheManager initilized i2");
	   //get pages
	   viewUrl = config.getInitParameter("viewUrl");
	   editUrl = config.getInitParameter("editUrl");
	   proxyUrl = config.getInitParameter("proxyUrl");
	   errorUrl = config.getInitParameter("errorUrl");
	   configUrl = config.getInitParameter("configUrl");
	   log.info("url "+ viewUrl +" : "+editUrl+" : "+proxyUrl+" : "+errorUrl+" : "+configUrl);
	   //get config params
	   attributeMappingForUsername = config.getInitParameter("portal.attribute.mapping.username");
	   log.info("attributeMappingForUsername i3 " + attributeMappingForUsername);
	   //setup cache, use factory method to ensure singleton
//	   CacheManager.create();
//	   cache = CacheManager.getInstance().getCache(Constants.CACHE_NAME);
	   cache = cm.getCache(Constants.CACHE_NAME);
	   log.info("Cache initilized i4");
	}
	
	/**
	 * Delegate to appropriate PortletMode.
	 */
	protected void doDispatch(RenderRequest request, RenderResponse response) throws PortletException, IOException {
		log.debug("doDispatch()");
		
		//set global config
		getGlobalConfiguration(request);
		log.info("inside doDispatch "+request.getPortletMode().toString());
		if (StringUtils.equalsIgnoreCase(request.getPortletMode().toString(), "CONFIG")) {
			doConfig(request, response);
		}
		else {
			super.doDispatch(request, response);
		}
	}
	
	/**
	 * Process any portlet actions. 
	 */
	
	public void processAction(ActionRequest request, ActionResponse response) {
		log.debug("processAction()");
		
		//check mode and delegate
		if (StringUtils.equalsIgnoreCase(request.getPortletMode().toString(), "CONFIG")) {
			processConfigAction(request, response);
		} else if (StringUtils.equalsIgnoreCase(request.getPortletMode().toString(), "EDIT")) {
			processEditAction(request, response);
		} else {
			log.error("No handler for PortletMode: " + request.getPortletMode().toString());
		}
	}
	
	/**
	 * Helper to process CONFIG mode actions
	 * @param request
	 * @param response
	 */
	private void processConfigAction(ActionRequest request, ActionResponse response) {
		log.debug("processConfigAction()");
		
		boolean success = true;
		PortletPreferences prefs = request.getPreferences();
		
		//get params and validate 
		try {
			prefs.setValue("key", request.getParameter("key"));
			prefs.setValue("secret", request.getParameter("secret"));
			prefs.setValue("endpoint", request.getParameter("endpoint"));
			prefs.setValue("adminUsername", request.getParameter("adminUsername"));
			prefs.setValue("adminPassword", request.getParameter("adminPassword"));
			prefs.setValue("loginUrl", request.getParameter("loginUrl"));
			prefs.setValue("scriptUrl", request.getParameter("scriptUrl"));
			prefs.setValue("allowedTools", request.getParameter("allowedTools"));
			prefs.setValue("portletTitle", request.getParameter("portletTitle"));
			
		} catch (ReadOnlyException e) {
			success = false;
			response.setRenderParameter("errorMessage", Messages.getString("error.form.readonly.error"));
			log.error(e);
		}
		
		//save them
		if(success) {
			try {
				prefs.store();
				response.setPortletMode(PortletMode.VIEW);
			} catch (ValidatorException e) {
				response.setRenderParameter("errorMessage", e.getMessage());
				log.error(e);
			} catch (IOException e) {
				response.setRenderParameter("errorMessage", Messages.getString("error.form.save.error"));
				log.error(e);
			} catch (PortletModeException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Helper to process EDIT mode actions
	 * @param request
	 * @param response
	 */
	private void processEditAction(ActionRequest request, ActionResponse response) {
		log.debug("processEditAction()");

		replayForm = false;
		isValid = false;
		
		//get prefs and submitted values
		
		session = request.getPortletSession();
	
		HttpServletRequest realRequest = PortalUtil.getHttpServletRequest(request);
		HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(realRequest);
		
		PortletPreferences prefs = request.getPreferences();
//		PortletSession session = request.getPortletSession();
		log.info(prefs.getValue("endpoint", "NV1"));
		log.info(prefs.getValue("portletTitle", "NV2"));
		
		log.info(request.getParameter("remoteToolId"));
		
		if(null != originalRequest.getParameter("portletHeight")){portletHeight = originalRequest.getParameter("portletHeight");}
		if(null != originalRequest.getParameter("portletTitle")){portletTitle = StringEscapeUtils.escapeHtml(StringUtils.trim(originalRequest.getParameter("portletTitle")));}
		if(null != originalRequest.getParameter("remoteSiteId")){remoteSiteId = originalRequest.getParameter("remoteSiteId");}
		if(null != ParamUtil.getString(request, "remoteToolId") && ParamUtil.getString(request, "remoteToolId").length() > 0){remoteToolId = ParamUtil.getString(request, "remoteToolId");}
		
		log.info("******");
		log.info(portletHeight);
		log.info(portletTitle);
		log.info(remoteSiteId);
		log.info(remoteToolId);
/*		
		portletHeight = null!=originalRequest.getParameter("portletHeight")?originalRequest.getParameter("portletHeight"):(String) session.getAttribute("portletHeight", PortletSession.PORTLET_SCOPE);
		portletTitle = StringEscapeUtils.escapeHtml(StringUtils.trim(null!=originalRequest.getParameter("portletTitle")?originalRequest.getParameter("portletTitle"):(String) session.getAttribute("portletTitle", PortletSession.PORTLET_SCOPE)));
		remoteSiteId = null!=originalRequest.getParameter("remoteSiteId")?originalRequest.getParameter("remoteSiteId"):(String) session.getAttribute("remoteSiteId", PortletSession.PORTLET_SCOPE);
		remoteToolId = null!=originalRequest.getParameter("remoteToolId")?originalRequest.getParameter("remoteToolId"):(String) session.getAttribute("remoteToolId", PortletSession.PORTLET_SCOPE);
		
		if(null != portletHeight){session.setAttribute("portletHeight", portletHeight, PortletSession.PORTLET_SCOPE);}
		if(null != portletTitle){session.setAttribute("portletTitle", portletTitle, PortletSession.PORTLET_SCOPE);}
		if(null != remoteSiteId){session.setAttribute("portletHeight", remoteSiteId, PortletSession.PORTLET_SCOPE);}
		if(null != remoteToolId){session.setAttribute("portletHeight", remoteToolId, PortletSession.PORTLET_SCOPE);}
		else{remoteToolId = "sakai.announcements";}
*/		
		//catch a blank remoteSiteId and replay form
		if(StringUtils.isBlank(remoteSiteId)) {
			replayForm = true;
			response.setRenderParameter("portletTitle", portletTitle);
			response.setRenderParameter("portletHeight", portletHeight);
			return;
		}
		
		//catch a blank remoteToolId and replay form
		if(StringUtils.isBlank(remoteToolId)) {
			replayForm = true;
			response.setRenderParameter("portletTitle", portletTitle);
			response.setRenderParameter("portletHeight", portletHeight);
			response.setRenderParameter("remoteSiteId", remoteSiteId);
			return;
		}
		
		//portlet title could be blank, set to default
		//if(StringUtils.isBlank(portletTitle)){
		//	portletTitle=Constants.PORTLET_TITLE_DEFAULT;
		//}
		
		//form ok so validate
		try {
			prefs.setValue("portletHeight", portletHeight);
			
			//only set title if it is not blank
			if(StringUtils.isNotBlank(portletTitle)){
				prefs.setValue("portletTitle", portletTitle);
			}
			
			prefs.setValue("remoteSiteId", remoteSiteId);
			prefs.setValue("remoteToolId", remoteToolId);
		} catch (ReadOnlyException e) {
			replayForm = true;
			response.setRenderParameter("errorMessage", Messages.getString("error.form.readonly.error"));
			log.error(e);
			return;
		}
		
		//save them
		try {
			prefs.store();
			isValid=true;
		} catch (ValidatorException e) {
			replayForm = true;
			response.setRenderParameter("errorMessage", e.getMessage());
			log.error(e);
			return;
		} catch (IOException e) {
			replayForm = true;
			response.setRenderParameter("errorMessage", Messages.getString("error.form.save.error"));
			log.error(e);
			return;
		}
		
		//if ok, invalidate cache and return to view
		if(isValid) {
			log.info(prefs.getValue("remoteSiteId", "NV-3"));
			try {
				response.setPortletMode(PortletMode.VIEW);
			} catch (PortletModeException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Custom mode handler for CONFIG view
	 */
	protected void doConfig(RenderRequest request, RenderResponse response) throws PortletException, IOException {
		log.debug("doConfig()");
				
		//set global settings into scope and dispatch
		request.setAttribute("key", key);
		request.setAttribute("secret", secret);
		request.setAttribute("endpoint", endpoint);
		request.setAttribute("adminUsername", adminUsername);
		request.setAttribute("adminPassword", adminPassword);
		request.setAttribute("loginUrl", loginUrl);
		request.setAttribute("scriptUrl", scriptUrl);
		request.setAttribute("allowedTools", allowedTools);
		request.setAttribute("portletTitle", getTitle(request));
		
		dispatch(request, response, configUrl);
	}

	
	/**
	 * Render the main view
	 */
	protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
		log.debug("doView()");
		
		//get data
		Map<String,String> launchData = getLaunchData(request, response);
		
		//catch - errors already handled
		if(launchData == null) {
			return;
		}
		
		//setup the params, serialise to a URL
		StringBuilder proxy = new StringBuilder();
		proxy.append(request.getContextPath());
		proxy.append(proxyUrl);
		proxy.append("?");
		proxy.append(HttpSupport.serialiseMapToQueryString(launchData));
		
		request.setAttribute("proxyContextUrl", proxy.toString());
		request.setAttribute("preferredHeight", getPreferredPortletHeight(request));
		
		dispatch(request, response, viewUrl);
	}	
		
	/**
	 * Render the edit page, invalidates any cached data
	 */
	protected void doEdit(RenderRequest request, RenderResponse response) throws PortletException, IOException {
		log.debug("doEdit()");
		
		//check the global Sakai configuration is set
		if(StringUtils.isBlank(adminUsername) || StringUtils.isBlank(adminPassword) || StringUtils.isBlank(loginUrl) || StringUtils.isBlank(scriptUrl) || StringUtils.isBlank(allowedTools)) {
			log.error("Sakai configuration incomplete or missing. Please configure this portlet.");
			doError("error.no.sakai.config", "error.heading.general", request, response);
			return;
		}
		
		//setup the web service bean
		SakaiWebServiceLogic logic = new SakaiWebServiceLogic();
		logic.setAdminUsername(adminUsername);
		logic.setAdminPassword(adminPassword);
		logic.setLoginUrl(loginUrl);
		logic.setScriptUrl(scriptUrl);
		request.setAttribute("logic", logic);
		
		//setup remote userId
		String remoteUserId = getRemoteUserId(request, logic);
		if(StringUtils.isBlank(remoteUserId)) {
			log.error("No user info was returned from remote server.");
			doError("error.no.remote.data", "error.heading.general", request, response);
			return;
		}
		
		request.setAttribute("eid", getAuthenticatedUsername(request));
		request.setAttribute("remoteUserId", remoteUserId);

		
		// get list of sites
		List<Site> sites = getRemoteSitesForUser(request, logic);
		if(sites.isEmpty()){
			log.error("No sites were returned from remote server.");
			doError("error.no.remote.data", "error.heading.general", request, response);
			return;
		}
		request.setAttribute("remoteSites", sites);
		
		//set list of allowed tool registrations
		request.setAttribute("allowedToolIds", Arrays.asList(StringUtils.split(allowedTools, ':')));
	
		//do we need to replay the form? This could be due to an error, or we need to show the lists again.
		//if so, use the original request params
		//otherwise, use the preferences
		if(replayForm) {
			
			String portletTitle = request.getParameter("portletTitle");
			String portletHeight = request.getParameter("portletHeight");
			String remoteSiteId = request.getParameter("remoteSiteId");
			String remoteToolId = request.getParameter("remoteToolId");
			
			if(StringUtils.isBlank(portletTitle)){
				portletTitle = getPreferredPortletTitle(request);
			}
			
			if(StringUtils.isBlank(portletHeight)){
				portletHeight = String.valueOf(getPreferredPortletHeight(request));
			}
			
			if(StringUtils.isBlank(remoteSiteId)){
				remoteSiteId = getPreferredRemoteSiteId(request);
			}
			
			if(StringUtils.isBlank(remoteToolId)){
				
				//if the request site and preference site are equal, use the preference
				//do not do this in any other case though since we are changing the site value and the tool list needs to be reset.
				String preferredRemoteSiteId = getPreferredRemoteSiteId(request);
				if(StringUtils.equals(remoteSiteId, preferredRemoteSiteId)) {
					remoteToolId = getPreferredRemoteToolId(request);
				}
			}
			
			request.setAttribute("preferredPortletHeight", portletHeight);
			request.setAttribute("preferredPortletTitle", portletTitle);
			request.setAttribute("preferredRemoteSiteId", remoteSiteId);
			request.setAttribute("preferredRemoteToolId", remoteToolId);
			request.setAttribute("errorMessage", request.getParameter("errorMessage"));
		} else {
			request.setAttribute("preferredPortletHeight", getPreferredPortletHeight(request));
			request.setAttribute("preferredPortletTitle", getPreferredPortletTitle(request));
			request.setAttribute("preferredRemoteSiteId", getPreferredRemoteSiteId(request));
			request.setAttribute("preferredRemoteToolId", getPreferredRemoteToolId(request));
			
			//invalidate the cache for this item as it may change (only need to do this once per edit page view)
			evictFromCache(getPortletNamespace(response));
		}
		
		//cancel url
		request.setAttribute("cancelUrl", getPortletModeUrl(response, PortletMode.VIEW));
		
		
		dispatch(request, response, editUrl);
	}
	
	/**
	 * Get the current user's details, exposed via portlet.xml
	 * @param request
	 * @return Map<String,String> of info
	 */
	@SuppressWarnings("unchecked")
	private Map<String,String> getUserInfo(RenderRequest request) {
		return (Map<String,String>)request.getAttribute(PortletRequest.USER_INFO);
	}
	
	
	/**
	 * Gets the unique namespace for this portlet
	 * @param response
	 * @return
	 */
	private String getPortletNamespace(RenderResponse response) {
		return response.getNamespace();
	}
	
	/**
	 * Setup the Map of params for the request
	 * @param request
	 * @param response
	 * @return Map of params or null if any required data is missing
	 */
	private Map<String,String> getLaunchData(RenderRequest request, RenderResponse response) {
		
		//check the global Basic LTI configuration is set
		if(StringUtils.isBlank(key) || StringUtils.isBlank(secret) || StringUtils.isBlank(endpoint) || StringUtils.isBlank(scriptUrl)) {
			log.error("Basic LTI configuration incomplete or missing. Please configure this portlet.");
			doError("error.no.basiclti.config", "error.heading.general", request, response);
			return null;
		}
		
		//check the global Sakai configuration is set
		if(StringUtils.isBlank(adminUsername) || StringUtils.isBlank(adminPassword) || StringUtils.isBlank(loginUrl) || StringUtils.isBlank(scriptUrl)) {
			log.error("Sakai configuration incomplete or missing. Please configure this portlet.");
			doError("error.no.sakai.config", "error.heading.general", request, response);
			return null;
		}
		
		//launch map
		Map<String,String> params;
		
		//check cache, otherwise form up all of the data
		String cacheKey = getPortletNamespace(response);
		params = retrieveFromCache(cacheKey);
		if(params == null) {
		
			//init for new data
			params = new HashMap<String,String>();
			
			//get site prefs
			String preferredRemoteSiteId = getPreferredRemoteSiteId(request);
			if(StringUtils.isBlank(preferredRemoteSiteId)) {
				doError("error.no.config", "error.heading.config", getPortletModeUrl(response, PortletMode.EDIT), request, response);
				return null;
			}
		
			//get tool prefs
			String preferredRemoteToolId = getPreferredRemoteToolId(request);
			if(StringUtils.isBlank(preferredRemoteToolId)) {
				doError("error.no.config", "error.heading.config", getPortletModeUrl(response, PortletMode.EDIT), request, response);
				return null;
			}
		
			//setup the web service bean
			SakaiWebServiceLogic logic = new SakaiWebServiceLogic();
			logic.setAdminUsername(adminUsername);
			logic.setAdminPassword(adminPassword);
			logic.setLoginUrl(loginUrl);
			logic.setScriptUrl(scriptUrl);
		
			//get remote userId
			String remoteUserId = getRemoteUserId(request, logic);
			if(StringUtils.isBlank(remoteUserId)) {
				doError("error.no.remote.data", "error.heading.general", request, response);
				return null;
			}
			
			//setup full endpoint
			params.put("endpoint_url", endpoint + preferredRemoteToolId);
			
		
			//required fields
			params.put("user_id", getAuthenticatedUsername(request));
			params.put("lis_person_name_given", null);
			params.put("lis_person_name_family", null);
			params.put("lis_person_name_full", null);
			params.put("lis_person_contact_email_primary", null);
			params.put("resource_link_id", getPortletNamespace(response));
			params.put("context_id", preferredRemoteSiteId);
			params.put("tool_consumer_instance_guid", key);
			params.put("lti_version","LTI-1p0");
			params.put("lti_message_type","basic-lti-launch-request");
			params.put("oauth_callback","about:blank");
			params.put("basiclti_submit", "Launch Endpoint with BasicLTI Data");
			params.put("user_id", remoteUserId);
		
			//additional fields
			params.put("remote_tool_id", preferredRemoteToolId);
			
			//cache the data, must be done before signing
			updateCache(cacheKey, params);
		}
		
		if(log.isDebugEnabled()) {
			log.debug("Parameter map before OAuth signing");
			CollectionsSupport.printMap(params);
		}
		
		//sign the properties map
		params = OAuthSupport.signProperties(params.get("endpoint_url"), params, "POST", key, secret);

		if(log.isDebugEnabled()) {
			log.warn("Parameter map after OAuth signing");
			CollectionsSupport.printMap(params);
		}
		
		return params;
	}
	
	
	/**
	 * Get the preferred portlet height if set, or default from Constants
	 * @param request
	 * @return
	 */
	private int getPreferredPortletHeight(RenderRequest request) {
	      PortletPreferences pref = request.getPreferences();
	      return Integer.parseInt(pref.getValue("portletHeight", String.valueOf(Constants.PORTLET_HEIGHT_DEFAULT)));
	}
	
	/**
	 * Get the preferred portlet title if set, or default from Constants
	 * @param request
	 * @return
	 */
	private String getPreferredPortletTitle(RenderRequest request) {
		PortletPreferences pref = request.getPreferences();
		return pref.getValue("portletTitle", Constants.PORTLET_TITLE_DEFAULT);
	}
	
	/**
	 * Get the preferred remote site id, or null if they have not made a choice yet
	 * @param request
	 * @return
	 */
	private String getPreferredRemoteSiteId(RenderRequest request) {
		PortletPreferences pref = request.getPreferences();
		return pref.getValue("remoteSiteId", null);
	}
	
	/**
	 * Get the preferred remote tool id, or null if they have not made a choice yet
	 * @param request
	 * @return
	 */
	private String getPreferredRemoteToolId(RenderRequest request) {
		PortletPreferences pref = request.getPreferences();
		return pref.getValue("remoteToolId", null);
	}
	
	/**
	 * Get the current username
	 * @param request
	 * @return
	 */
	private String getAuthenticatedUsername(RenderRequest request) {
		Map<String,String> userInfo = getUserInfo(request);
		log.info("getAuthenticatedUsername1 "+userInfo.get(attributeMappingForUsername));
		log.info("getAuthenticatedUsername2 "+userInfo.get("username"));
		//return userInfo.get("username");
		return userInfo.get(attributeMappingForUsername);
	}
	
	/**
	 * Get the remote userId for this user, either from session or from remote service
	 * @param request
	 * @param logic
	 * @return
	 */
	private String getRemoteUserId(RenderRequest request, SakaiWebServiceLogic logic){
		
		String remoteUserId = (String) request.getPortletSession().getAttribute("remoteUserId");
		log.info("remoteuserID1 "+remoteUserId);
		if(StringUtils.isBlank(remoteUserId)) {
			remoteUserId = SakaiWebServiceHelper.getRemoteUserIdForUser(logic, getAuthenticatedUsername(request));
			log.info("remoteuserID2 "+remoteUserId);
			request.getPortletSession().setAttribute("remoteUserId", remoteUserId);
		}
		
		return remoteUserId;
	}
	
	/**
	 * Get the list of remote sites for this user
	 * @param logic
	 * @return
	 */
	private List<Site> getRemoteSitesForUser(RenderRequest request, SakaiWebServiceLogic logic){
		return SakaiWebServiceHelper.getAllSitesForUser(logic, getAuthenticatedUsername(request));
	}
	
	
	/**
	 * Override GenericPortlet.getTitle() to use the preferred title for the portlet instead
	 */
	@Override
	protected String getTitle(RenderRequest request) {
		return getPreferredPortletTitle(request);
	}
	
	/**
	 * Get the global config that is setup in the config mode
	 * @param request
	 */
	private void getGlobalConfiguration(RenderRequest request){
		
		PortletPreferences pref = request.getPreferences();
		
		key = pref.getValue("key", null);
		log.info("key "+key);
		secret = pref.getValue("secret", null);
		log.info("secret "+secret);
		endpoint = pref.getValue("endpoint", null);
		log.info("endpoint "+endpoint);
		adminUsername = pref.getValue("adminUsername", null);
		log.info("adminUsername "+adminUsername);
		adminPassword = pref.getValue("adminPassword", null);
		log.info("adminPassword "+adminPassword);
		loginUrl = pref.getValue("loginUrl", null);
		log.info("loginUrl "+loginUrl);
		scriptUrl = pref.getValue("scriptUrl", null);
		log.info("scriptUrl "+scriptUrl);
		allowedTools = pref.getValue("allowedTools", null);
		log.info("allowedTools "+allowedTools);
		
	}
	
	
	/**
	 * Helper to handle error messages
	 * @param messageKey	Message bundle key
	 * @param headingKey	optional error heading message bundle key, if not specified, the general one is used
	 * @param request
	 * @param response
	 */
	private void doError(String messageKey, String headingKey, RenderRequest request, RenderResponse response){
		doError(messageKey, headingKey, null, request, response);
	}
	
	/**
	 * Helper to handle error messages
	 * @param messageKey	Message bundle key
	 * @param headingKey	optional error heading message bundle key, if not specified, the general one is used
	 * @param link			if the message text is to be linked, what is the href?
	 * @param request
	 * @param response
	 */
	private void doError(String messageKey, String headingKey, String link, RenderRequest request, RenderResponse response){
		
		//message
		request.setAttribute("errorMessage", Messages.getString(messageKey));
		
		//optional heading
		if(StringUtils.isNotBlank(headingKey)){
			request.setAttribute("errorHeading", Messages.getString(headingKey));
		} else {
			request.setAttribute("errorHeading", Messages.getString("error.heading.general"));
		}
		
		if(StringUtils.isNotBlank(link)){
			request.setAttribute("errorLink", link);
		}
		
		//dispatch
		try {
			dispatch(request, response, errorUrl);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	/**
	 * Dispatch to a JSP or servlet
	 * @param request
	 * @param response
	 * @param path
	 * @throws PortletException
	 * @throws IOException
	 */
	protected void dispatch(RenderRequest request, RenderResponse response, String path)throws PortletException, IOException {
		response.setContentType("text/html"); 
		PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher(path);
		dispatcher.include(request, response);
	}
	
	/**
	 * Helper to evict an item from a cache. If we visit the edit mode, we must evict the current data. It will be re-cached later.
	 * @param cacheKey	the id for the data in the cache
	 */
	private void evictFromCache(String cacheKey) {
		cache.remove(cacheKey);
		log.debug("Evicted data in cache for key: " + cacheKey);
	}

	/**
	 * Helper to retrieve data from the cache
	 * @param key
	 * @return
	 */
	private Map<String,String> retrieveFromCache(String key) {
		Element element = cache.get(key);
		if(element != null) {
			Map<String,String> data = (Map<String,String>) element.getObjectValue();
			log.debug("Fetching data from cache for key: " + key);
			return data;
		} 
		return null;
	}
	
	
	/**
	 * Helper to update the cache
	 * @param cacheKey	the id for the data in the cache	
	 * @param data		the data to be assocaited with that key in the cache
	 */
	private void updateCache(String cacheKey, Map<String,String> data){
		cache.put(new Element(cacheKey, data));
		log.debug("Added data to cache for key: " + cacheKey);
	}
	
	
	/**
	 * Helper to get the URL to take us to a portlet mode.
	 * This will end up in doDispatch.
	 * 
	 * @param response
	 * @return
	 */
	private String getPortletModeUrl(RenderResponse response, PortletMode mode) {

		PortletURL url = response.createRenderURL();
	    try {
	    	url.setPortletMode(mode);
		} catch (PortletModeException e) {
			log.error("Invalid portlet mode: " + mode);
			return null;
		}
	    
		return url.toString();
	}
	
	
	public void destroy() {
		log.info("destroy()");
		CacheManager.getInstance().shutdown();
	}
	
	private static Log log = LogFactoryUtil.getLog(PortletDispatcher.class);
	
}
