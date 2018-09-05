/**
 *   Author : Jayant Kumar
 */
package au.edu.anu.portal.portlets.sakaiconnector.support;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.signature.OAuthSignatureMethod;

/**
 * A set of OAuth methods
 * 
  *
 */

public class OAuthSupport {

	/**
	 * Charset to encode params with 
	 */
	private final static String CHARSET= "UTF-8";
	
	
	/**
	 * Sign a property Map with OAuth.
	 * @param url		the url where the request is to be made
	 * @param props		the map of properties to sign
	 * @param method	the HTTP request method, eg POST
	 * @param key		the oauth_consumer_key
	 * @param secret	the shared secret
	 * @return
	 */
	public static Map<String,String> signProperties(String url, Map<String,String> props, String method, String key, String secret) {
        
        if (key == null || secret == null) {
            log.error("Error in signProperties - key and secret must be specified");
            return null;
        }

        OAuthMessage oam = new OAuthMessage(method, url,props.entrySet());
        OAuthConsumer cons = new OAuthConsumer("about:blank",key, secret, null);
        OAuthAccessor acc = new OAuthAccessor(cons);
        try {
            oam.addRequiredParameters(acc);
            if(log.isDebugEnabled()){
            	log.debug("Base Message String\n"+OAuthSignatureMethod.getBaseString(oam)+"\n");
            }

            List<Map.Entry<String, String>> params = oam.getParameters();
    
            Map<String,String> headers = new HashMap<String,String>();
            for (Map.Entry<String,String> p : params) {
            	//as per the spec, all params must be encoded
            	String param = URLEncoder.encode(p.getKey(), CHARSET);
            	String value = p.getValue();
                String encodedValue = value != null ? URLEncoder.encode(value, CHARSET) : "";
            	headers.put(param, encodedValue);
            }
            return headers;
        } catch (OAuthException e) {
            log.error(e.getClass() + ":"+ e.getMessage());
            return null;
        } catch (IOException e) {
            log.error(e.getClass() + ":"+ e.getMessage());
            return null;
        } catch (URISyntaxException e) {
            log.error(e.getClass() + ":"+ e.getMessage());
            return null;
        }
    
    }
	
	private static Log log = LogFactoryUtil.getLog(OAuthSupport.class);
}
