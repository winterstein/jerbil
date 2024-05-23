package com.goodloop.jerbil;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.html.LinkResolver;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.html.renderer.ResolvedLink;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;

/**
 * Resolves link references
 * 
 * @author daniel
 *
 */
class JerbilLinkResolver implements LinkResolver {

	JerbilConfig config = Dep.get(JerbilConfig.class);
	
	Map<String,String> urlForLink = new HashMap();
	
	@Override
	public ResolvedLink resolveLink(Node arg0, LinkResolverContext arg1, ResolvedLink arg2) {		
//		Document doc = arg1.getDocument();
//		String target = arg2.getTarget();
//		LinkType lt = arg2.getLinkType();
		String url = arg2.getUrl(); // e.g. Publishers-How-to-install-Good-Loop-on-your-site
		Log.d("linkresolver", url);
		// protocol or abs path?
		if (url.startsWith("http") || url.startsWith("/") || url.startsWith("#")) {
			return arg2;
		}
		// file type? then leave it alone (apart from .md)
		String ftype = FileUtils.getType(url);
		if (ftype!=null && ! ftype.isEmpty() && ! "md".contains(ftype)) {
			return arg2;
		}
		// resolve if we can
		String url2 = resolveLink2(url);
		if (url2 != null) {
			ResolvedLink resolved = arg2.withUrl(url2);
			return resolved;
		}		
		return arg2;
	}

	public String resolveLink2(String url) {		
		List<File> match = findFilesFromRef(url);
		if (match.isEmpty()) {
			Log.d("linkresolver", Printer.str(Arrays.asList(url))+" - no match");
			return null;
		}
		String rpath;
		try {
			rpath = FileUtils.getRelativePath(match.get(0), config.getPagesDir());
		} catch (IllegalArgumentException e) {
			// It's odd but OK to reference a webroot file
			rpath = FileUtils.getRelativePath(match.get(0), config.getWebRootDir());
		}
		
		File htmlpath = FileUtils.changeType(new File(rpath), "html");			
		String url2 = "/"+htmlpath; // otherwise we get the full-web-path handled as a relative path
		// NB: relative paths would be nice - but would require knowing the relative path for the doc (a faff to get here)
		if ( ! url2.endsWith(".html")) {
			url2 += ".html"; // NB: if no type, changeType doesnt add??
		}
		Log.d("linkresolver", Printer.str(Arrays.asList(url))+" -> "+match.get(0));
		return url2;
	}

	List<File> findFilesFromRef(String url) {
		String canonUrl = canon(url);
		File[] dirs = new File[] {
			config.getTemplatesDir(),
			config.getPagesDir(),
			config.getWebRootDir()
		};
		for(File dir : dirs) {
			if (dir==null || ! dir.exists()) continue;
			List<File> match = FileUtils.find(dir, f -> canonUrl.equals(canon(f.getName())));
			if ( ! match.isEmpty()) return match;
		}		
		return Collections.EMPTY_LIST;
	}

	/**
	 * 
	 * @param url
	 * @return strip filetype and canonicalise eg case
	 */
	private String canon(String url) {
		String bn = FileUtils.getBasename(url);
		return StrUtils.toCanonical(bn).replaceAll("\\s", "");
	}
	
	
}