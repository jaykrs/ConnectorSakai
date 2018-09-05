/**
 *   Author : Jayant Kumar
 */
package au.edu.anu.portal.portlets.sakaiconnector.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * An object that holds information about a site, retrieved from the web service call
 * 
 *
 */

@NoArgsConstructor
@Data
@Root(name="item")
public class Site {

	@Element(name="siteId")
	private String id;
	
	@Element(name="siteTitle")
	private String title;
	
}
