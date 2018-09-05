/**
 * Author : Jayant Kumar
 */
package au.edu.anu.portal.portlets.sakaiconnector.logic;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.lang.StringUtils;

import au.edu.anu.portal.portlets.sakaiconnector.models.Site;
import au.edu.anu.portal.portlets.sakaiconnector.models.Tool;
import au.edu.anu.portal.portlets.sakaiconnector.support.WebServiceSupport;
import au.edu.anu.portal.portlets.sakaiconnector.utils.Constants;
import au.edu.anu.portal.portlets.sakaiconnector.utils.XmlParser;


/**
 * This class is a simple logic class that makes the required web service calls
 * for the Basic LTI portlet to get its information.
 * 
 * It users the WebServiceSupport class for the actual web service calls.
 * 
 * A remote sessionid is stored in the cache, and checked however if any service calls fail, it is invalidated to force a new one.
 * One point to remember here is that all sessions created are for the admin user, which then gets the data on behalf of the user.
 * Sessions are cached.
 * 
 *
 *
 */
public class SakaiWebServiceLogic {
	
	
	private String adminUsername;
	
	
	private String adminPassword;
	
	
	private String loginUrl;
	
	
	private String scriptUrl;
	
	private static final String METHOD_LOGIN="login";
	private static final String METHOD_GET_USER_ID="getUserId";
	private static final String METHOD_CHECK_SESSION="checkSession";
	private static final String METHOD_GET_ALL_SITES_FOR_USER="getAllSitesForUser";
	private static final String METHOD_GET_PAGES_AND_TOOLS_FOR_SITE="getPagesAndToolsForSite";

	private Cache cache;
	private static final String CACHE_KEY = "admin_remote_session_id"; //used for storing the session in the cache. Should not conflict with an eid (?!)

	
	/**
	 * Get the userId for a user.
	 * @return id or null if no response
	 */
	public String getRemoteUserIdForUser(final String eid) {
		
		
		Object[] data= new Object[2];
		data[0] = (Object)getSession();
		data[1] = (Object)eid;
		log.info("getRemoteUserIdForUser "+data[0] +" : "+data[1] +" "+getScriptUrl() + " : " +METHOD_GET_USER_ID + " "+data);
		return WebServiceSupport.call(getScriptUrl(), METHOD_GET_USER_ID, data);
	}
	
	/**
	 * Get the XML for a list of all sites for a user, transformed to a List of Sites
	 * @return
	 */
	public List<Site> getAllSitesForUser(final String eid) {
		Object[] data= new Object[2];
		data[0] = (Object)getSession();
		data[1] = (Object)eid;

		String xml = WebServiceSupport.call(getScriptUrl(), METHOD_GET_ALL_SITES_FOR_USER, data);		
		
		return XmlParser.parseListOfSites(xml);
	}
	
	/**
	 * Get the list of tools in a site.
	 * @param siteId	siteId
	 * @return
	 */
	public List<Tool> getToolsForSite(final String siteId, final String eid) {
		
		//setup data to send
		Object[] data= new Object[3];
		data[0] = (Object)getSession();
		data[1] = (Object)eid;
		data[2] = (Object)siteId;
		log.info("getToolsForSite "+data[0] +" " +data[1] +" "+data[2]);
		String xml = WebServiceSupport.call(getScriptUrl(), METHOD_GET_PAGES_AND_TOOLS_FOR_SITE, data);		
		log.info("result xml "+xml);
		return XmlParser.parseListOfPages(xml);
	}
	
	
	
	
	/**
	 * Get a new session for the admin user. Don't call this directly, use getSession() instead.
	 * @return
	 */
	private String getNewAdminSession() {
		String session = null;
		//setup data to send
		Object[] data= new Object[2];
		data[0] = (Object) getAdminUsername();
		data[1] = (Object)getAdminPassword();
		log.info("getNewAdminSession "+data[0] +" 2 "+data[1] +" "+getLoginUrl() +" "+METHOD_LOGIN);
		session = WebServiceSupport.call(getLoginUrl(), METHOD_LOGIN, data);
		log.info("session info"+session.toString());
		//cache it
		addSessionToCache(session);
		
		//and return it
		return session;
	}
	
	/**
	 * Check a given session is still active. Don't call this directly, use getSession() instead.
	 * @return
	 */
	private boolean isSessionActive(final String session) {
		
		//setup data to send
		Object[] data= new Object[1];
		data[0] = (Object) session;
		String results = WebServiceSupport.call(getScriptUrl(), METHOD_CHECK_SESSION, data);
		
		if(StringUtils.equals(results, session)) {
			return true;
		}
		
		return false;
	}
	
	
	
	
	
	public SakaiWebServiceLogic() {
		//setup cache via factory to create a singleton 
		CacheManager.create();
		cache = CacheManager.getInstance().getCache(Constants.CACHE_NAME);
	}
	
	/**
	 * Get local session, check it's still active, otherwise get a new one
	 * @return
	 */
	private String getSession() {
		
		//check session
		String session = getSessionFromCache();
		
		//if not in cache, get a new one
		if(StringUtils.isBlank(session)) {
			return getNewAdminSession();
		}
		
		//check it's still active
		if(!isSessionActive(session)){
			getNewAdminSession();
		}
		
		return session;
	}
	
	/**
	 * Get the session from the cache, otherwise return null. The session must be validated after this as it may have expired on the other end.
	 * Don't call this, call getSession instead as that method verifies the session is still active.
	 * @return	the session from the cache or null if it hasn't been created yet.
	 */
	private String getSessionFromCache() {
		
		Element element = cache.get(CACHE_KEY);
		if(element != null) {
			String session = (String) element.getObjectValue();
			log.debug("Fetching session from cache: " + session);
			return session;
		} 
		return null;
	}
	
	/**
	 * Helper to add an item to the session cache
	 * @param data
	 */
	private void addSessionToCache(String session){
		cache.put(new Element(CACHE_KEY, session));
		log.debug("Adding session to cache: " + session);
	}
	private static Log log = LogFactoryUtil.getLog(SakaiWebServiceLogic.class);


	public String getAdminUsername() {
		return adminUsername;
	}

	public void setAdminUsername(String adminUsername) {
		this.adminUsername = adminUsername;
	}

	public String getAdminPassword() {
		return adminPassword;
	}

	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}

	public String getLoginUrl() {
		return loginUrl;
	}

	public void setLoginUrl(String loginUrl) {
		this.loginUrl = loginUrl;
	}

	public String getScriptUrl() {
		return scriptUrl;
	}

	public void setScriptUrl(String scriptUrl) {
		this.scriptUrl = scriptUrl;
	}
}
