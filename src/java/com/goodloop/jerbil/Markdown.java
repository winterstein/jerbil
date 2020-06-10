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
 * 
 * 			// ?? or https://github.com/atlassian/commonmark-java

 * 
 * @author Mark, Daniel
 * @testedby MarkdownTest
 */
public class Markdown {

	int sectionDivs;
	
	public String render(String page) {
		MutableDataSet options = new MutableDataSet();		
		
        // uncomment to set optional extensions
        options.set(
        		Parser.EXTENSIONS, Arrays.asList(
        			TablesExtension.create(), 
        			StrikethroughExtension.create(),
        			AnchorLinkExtension.create(),
        			WikiLinkExtension.create(),
        			JerbilLinkResolverExtension.create()
        		));

        // uncomment to convert soft-breaks to hard breaks
        //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        options.set(HtmlRenderer.GENERATE_HEADER_ID, true);
        
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        // You can re-use parser and renderer instances
        Node document = parser.parse(page);
        String html = renderer.render(document);  // "<p>This is <em>Sparta</em></p>\n"
  
        if (sectionDivs <= 0) {
        	return html;
        }
        
        // Use-case: e.g. to create container guts will full-bleed backgrounds
        // Create section divs from headers
        // NB: must countdown, otherwise youd have to deal with spotting the divs it itself inserts
        for(int hi=sectionDivs; hi!=0; hi--) {
	        int indx = 0;   
	        while(true) {
	        	int i = html.indexOf("<h"+hi, indx);
	        	if (i==-1) break;
	        	int tagEnd = html.indexOf("</h"+hi, i);
	        	String h = WebUtils.stripTags(html.substring(i, tagEnd));
	        	String cn = StrUtils.toCanonical(h).replaceAll("\\s+", "-");
	        	// where does this section end? when it hist the next header
	        	int endOfSection = sectionEnd(html, hi, tagEnd+4);
	        	String div = "<div class='h"+hi+"-section "+cn+"'><div class='section-body'>\n";
	        	String html2 = html.substring(0, i)+div+html.substring(i, endOfSection)+"\n</div></div><!-- ./"+cn+" -->\n"
	        			+html.substring(endOfSection);
	        	html = html2;
	        	indx = i + div.length() + 1;
	        }
        }

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
	private int sectionEnd(String html, int hi, int openingHTagEnd) {
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

	/**
	 * render without a wrapping div
	 * @param string
	 * @return fragment of html
	 */
	public String renderWithoutWrapper(String string) {
		// ??for speed: check if its plain text?
		String mds = render(string);
		mds = mds.replaceFirst("^<[^>]+>", "");
		mds = mds.replaceFirst("</[a-zA-Z]+>$", "");
		return mds.trim();
	}

}
