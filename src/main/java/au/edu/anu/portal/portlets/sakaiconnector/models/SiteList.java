/**
 *   Author : Jayant Kumar
 */
package au.edu.anu.portal.portlets.sakaiconnector.models;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * A convenience object to hold a list of Sites retrieved from the web service call, when the XML is parsed.
 * Contains a single property, the list of Sites.
 * 
  *
 */

@NoArgsConstructor
@Data
@Root(name="list")
public class SiteList {

	@ElementList(name="item",inline=true)
	private List<Site> sites;

	public List<Site> getSites() {
		return sites;
	}

	public void setSites(List<Site> sites) {
		this.sites = sites;
	}
	
}
