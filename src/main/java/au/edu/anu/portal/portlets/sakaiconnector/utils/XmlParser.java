/**
 *   Author : Jayant Kumar
 */
package au.edu.anu.portal.portlets.sakaiconnector.utils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import au.edu.anu.portal.portlets.sakaiconnector.models.Page;
import au.edu.anu.portal.portlets.sakaiconnector.models.Site;
import au.edu.anu.portal.portlets.sakaiconnector.models.SiteList;
import au.edu.anu.portal.portlets.sakaiconnector.models.SitePageList;
import au.edu.anu.portal.portlets.sakaiconnector.models.Tool;

/**
 * A collection of methods to parse XML into various lists and objects
 * Ideally these should be genericised to a generic parsing algorithm
 * 

 *
 */

public class XmlParser {
	
	/**
	 * Parse the given XML into a list of Sites. This comes from getSitesUserCanAccess.
	 * @param xml
	 * @return
	 */
	public static List<Site> parseListOfSites(String xml){
		
		if(log.isDebugEnabled()) {
			log.debug(xml);
		}
		
		Serializer serializer = new Persister();
		List<Site> sites = new ArrayList<Site>();
		
		//validate
		if(!isValid(serializer, SiteList.class, xml)){
			log.error("Invalid SiteList: " + xml);
			return sites;
		}
		
		//parse
		try {
			SiteList list = serializer.read(SiteList.class, xml);
			sites = list.getSites();
			if(log.isDebugEnabled()) {
				log.debug("Num sites found: " + sites.size());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return sites;
	}
	
	/**
	 * Parse the given XML into a list of Tools. This comes from getPagesAndToolsForSite.
	 * @param xml
	 * @return
	 */
	public static List<Tool> parseListOfPages(String xml){
		
		if(log.isDebugEnabled()) {
			log.debug(xml);
		}
		
		
		Serializer serializer = new Persister();
		List<Tool> tools = new ArrayList<Tool>();
		
		//validate
		if(!isValid(serializer, SitePageList.class, xml)){
			log.error("Invalid SitePageList: " + xml);
			return tools;
		}
		
		//parse
		try {
			SitePageList sitePageList = serializer.read(SitePageList.class, xml);
			List<Page> pages = sitePageList.getPageList().getPages();
			for(Page p: pages) {
				tools.addAll(p.getTools());
			}
			if(log.isDebugEnabled()) {
				log.debug("Num tools found: " + tools.size());
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return tools;
	}
	
	
	/**
	 * Check if the given XML validates to the given class type
	 * @param serializer
	 * @param clazz
	 * @param xml
	 * @return
	 * @throws Exception 
	 */
	private static boolean isValid(Serializer serializer, Class<?> clazz, String xml){
		
		if(StringUtils.isBlank(xml)){
			return false;
		}
		
		try {
			if(serializer.validate(clazz, xml)) {
				return true;
			}
		} catch (Exception e) {
			log.error("Unable to validate xml: " + e.getClass().getName() + ": " + e.getMessage());
		}
		return false;
	}
	private static Log log = LogFactoryUtil.getLog(XmlParser.class);
}
