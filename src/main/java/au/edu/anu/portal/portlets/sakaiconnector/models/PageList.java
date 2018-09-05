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
 * A convenience object to hold a list of pages retrieved from the web service call, when the XML is parsed.
 * 
 *
 */

@NoArgsConstructor
@Data
@Root(name="pages")
public class PageList {

	@ElementList(name="page",inline=true)
	private List<Page> pages;

	public List<Page> getPages() {
		return pages;
	}

	public void setPages(List<Page> pages) {
		this.pages = pages;
	}
	
}
