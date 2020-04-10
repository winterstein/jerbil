package com.goodloop.jerbil;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.web.WebUtils;

/**
 * Utility class for calling flexmark
 * @author daniel
 *
 */
public class Markdown {

	public static String render(String page) {
		MutableDataSet options = new MutableDataSet();		
		
        // uncomment to set optional extensions
        options.set(
        		Parser.EXTENSIONS, Arrays.asList(
        			TablesExtension.create(), 
        			StrikethroughExtension.create(),
        			AnchorLinkExtension.create(),
        			WikiLinkExtension.create()
        			,JerbilLinkResolverExtension.create()
        		));

        // uncomment to convert soft-breaks to hard breaks
        //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        options.set(HtmlRenderer.GENERATE_HEADER_ID, true);
        
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        // You can re-use parser and renderer instances
        Node document = parser.parse(page);
        String html = renderer.render(document);  // "<p>This is <em>Sparta</em></p>\n"
  
        // NB: must countdown, otherwise youd have to deal with spotting the divs it itself inserts
        for(int hi=6; hi!=0; hi--) {
	        int indx = 0;   
	        while(true) {
	        	int i = html.indexOf("<h"+hi, indx);
	        	if (i==-1) break;
	        	int tagEnd = html.indexOf("</h"+hi, i);
	        	String h = WebUtils.stripTags(html.substring(i, tagEnd));
	        	String cn = StrUtils.toCanonical(h).replaceAll("\\s+", "-");
	        	// where does this section end? when it hist the next header
	        	int endOfSection = sectionEnd(html, hi, tagEnd+4);
	        	String div = "<div class='h"+hi+"-section "+cn+"'>";
	        	html = html.substring(0, i)+div+html.substring(i, endOfSection)+"</div><!-- ./"+cn+" -->\n"
	        			+html.substring(endOfSection);
	        	indx = i + div.length() + 1;
	        }
        }
//          System.out.println(html);
        return html;
	}

	/**
	 * Find where this section ends -- which is when the next h tag (of same or higher rank) occurs,
	 * or end of the page.
	 * 
	 * @param html
	 * @param hi
	 * @param openingHTagEnd
	 * @return
	 */
	private static int sectionEnd(String html, int hi, int openingHTagEnd) {
		int earliest = html.length();
		for(int hi2 = hi; hi2 > 0; hi2--) {
			int j = html.indexOf("<h"+hi2, openingHTagEnd);
			if (j!=-1 && j<earliest) earliest = j;
		}
		for(int hi2 = hi; hi2 > 0; hi2--) {
			Pattern p = Pattern.compile("<\\w[^>]+class=[^>]+start-h"+hi2);
			Matcher m = p.matcher(html);
			if (m.find(openingHTagEnd)) {
				int j = m.start(); // html.indexOf(".start-h"+hi2, openingHTagEnd);
				if (j!=-1 && j<earliest) earliest = j;
			}
		}
		return earliest;
	}

	public static String renderWithoutWrapper(String string) {
		// ??for speed: check if its plain text?
		String mds = render(string);
		mds = mds.replaceFirst("^<[^>]+>", "");
		mds = mds.replaceFirst("</[a-zA-Z]+>$", "");
		return mds.trim();
	}

}
