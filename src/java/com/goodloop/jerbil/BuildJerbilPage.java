
package com.goodloop.jerbil;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.Dep;
import com.winterwell.utils.IReplace;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.SimpleTemplateVars;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils2;

/**
 * This is where the text -> html magic happens
 * @testedby {@link BuildJerbilPageTest} 
 */
public class BuildJerbilPage {

	static final Pattern KV = Pattern.compile("^([A-Za-z0-9\\-_]+):(.*)$", Pattern.MULTILINE);
	private static final String LOGTAG = "jerbil";
	private File src;
	private File dir;
	private File out;
	private File template;
	private Map<String, String> baseVars = new ArrayMap();
	private String srcText;
	
	JerbilConfig config;
	
	private Map<String, String> vars;
	
	/**
	 * 
	 * @param src e.g. pages/mypage.md
	 * @param out e.g. webroot/mypage.html
	 * @param template
	 */
	public BuildJerbilPage(File src, File out, File template) {
		this(src, null, out, template);
	}

	public BuildJerbilPage(File src, String srcText, File out, File template) {		
		this.src = src;
		this.srcText = srcText;
		this.dir = src==null? null : src.getParentFile();
		this.out = out;
		this.template = template;
		setConfig(Dep.get(JerbilConfig.class));
	}

	public File getOut() {
		return out;
	}

	@Override
	public String toString() {
		return "BuildJerbilPage [src=" + src + ", out=" + out + ", template=" + template + "]";
	}

	void run() {
		Log.d(LOGTAG, "Build: "+src+" -> "+out+" ...");
		String html = FileUtils.read(template).trim();
		// check the template
		checkTemplate(html);
		
		String page = srcText==null? FileUtils.read(src) : srcText;
		page = page.trim();
		
		// NB: leave html as-is
		boolean applyMarkdown = ! (FileUtils.getType(src).equals("html") || FileUtils.getType(src).equals("htm"));
		html = run2_render(applyMarkdown, page, html, baseVars);
				
		File outDir = out.getParentFile();
		if (outDir != null) outDir.mkdirs(); // NB: for a single input file, outdir may be null
		FileUtils.write(out, html);
		Log.i(LOGTAG, "Made "+out);
		
		// HACK make a pdf too?
		run2_pdf();
	}

	private void run2_pdf() {
		if (config.makePdfPattern == null) return;
		if ("all".equals(config.makePdfPattern)) {
			// no need to check - we're converting all
		} else {
			String[] ps = config.makePdfPattern.split(",\\w*");
			final File fout = out;
			String match = Containers.first(Arrays.asList(ps), p -> FileUtils.globMatch(p, fout));
			if (match==null) {
				return;
			}
		}
		// make it
		File pdf = FileUtils.changeType(out, "pdf");
		RenderPDF rpdf = new RenderPDF(out, pdf);
		rpdf.setOptions(config.makePdfOptions);
		rpdf.run();
//		try (Proc proc = WebUtils.renderToPdf_usingChrome(out, pdf, config.makePdfOptions)) {
//			proc.waitFor();	
//		}				
		Log.d(LOGTAG, "Made "+pdf);
	}

	/**
	 * 
	 * @param applyMarkdown Only false for top-level html files
	 * @param srcPage Text contents
	 * @param html
	 * @return
	 */
	public String run2_render(boolean applyMarkdown, String srcPage, String templateHtml, Map var) {
		var = new HashMap(var); // copy to avoid side-effects
		// vars
		// ...$title (done before looking at the local vars so they could override it)
		if (src != null && ! var.containsKey("title")) {
			var.put("title", StrUtils.toTitleCasePlus(FileUtils.getBasename(src)));
		}
		long modtime = src==null? new Time().getTime() : src.lastModified();
		if ( ! var.containsKey("date")) {			
			var.put("date", new Time(modtime).format("d MMM yyyy"));
		}
		if ( ! var.containsKey("modtime")) {
			var.put("modtime", new Time(modtime).toString());
		}
		var.put("nocontents", ""); //
		
		// normalise quotes -- but not `s which are markdown markers
		if (config.smartyQuotes) {
			// TODO
		} else {
			srcPage = srcPage.replace("`", "!BACKTICK!"); // HACK preserve `s for <code>
			srcPage = StrUtils.normalisePunctuation(srcPage);
			srcPage = srcPage.replace("!BACKTICK!", "`");
		}
		
		// Strip out variables
		srcPage = chopSetVars(srcPage, var);
		// ...allow templates to set default vars
		templateHtml = chopSetVars(templateHtml, var);
		// override markdown key:value header values? use-case: csv
		if (vars!=null) {
			var.putAll(vars);
		}
		
		// Fill in section references in the body
		String srcPageNoSections = run3_fillSections(srcPage, var);
		assert ! SECTION.matcher(srcPageNoSections).find() : srcPageNoSections;
		
		// apply markdown
		String srcPageHtml = srcPageNoSections;
		if (applyMarkdown) {
			Markdown markdown = Dep.get(Markdown.class);
			// NB set lazily to allow that config might change
			markdown.sectionDivs = config.sectionDivs;
			if (config.noHeaderLinks!=null) markdown.noHeaderLinks = config.noHeaderLinks;
			// Hack: allow page level sectionDivs override
			if (var != null && var.containsKey("sectionDivs")) {
				Object sd = var.get("sectionDivs");
				markdown.sectionDivs = MathUtils.isNumber(sd)? (int) MathUtils.toNum(sd) 
						: (Utils.yes(sd)? 2 : 0); 
			}
			srcPageHtml = markdown.render(srcPageNoSections);
		}
		
		// put contents into the template
		String html = templateHtml.replace("$contents", srcPageHtml);
		
		// insert vars in (this allows for vars in template or page)
		html = insertVariables(html, var, applyMarkdown);				
		
		// Fill in of section references in the template
		// NB: these don't get treated as markdown
		html = run3_fillSections(html, var);

		// Jerbil version marker
		html = addJerbilVersionMarker(html);
		
		return html;
	}

	static Pattern SECTION = Pattern.compile(
			"<section\\s+src=['\"]([\\S'\"]+)['\"]\\s*(/>|>(.*?)</section\\s*>)", Pattern.CASE_INSENSITIVE+Pattern.DOTALL);
	
	/**
	 * NB: this is idempotent
	 * @param html
	 * @param var
	 * @return
	 */
	String run3_fillSections(String html, Map var) {		
//		for(int depth=0; depth<10; depth++) {
		final String fhtml = html;
		String html2 = StrUtils.replace(fhtml, SECTION, new IReplace() {
			@Override
			public void appendReplacementTo(StringBuilder sb, Matcher match) {
				// src=
				String insertSrc = match.group(1);
				// tag contents?
				String sectionContents;
				if (match.group().endsWith("/>")) {
					// self closing
					sectionContents = "";
				} else {
					// need a close
					String m2 = match.group(2);
					sectionContents = m2.substring(1); // chop the > from the front of m2
					Matcher chopOff = Pattern.compile("</section\\s*>$", Pattern.CASE_INSENSITIVE).matcher(sectionContents);
					if (chopOff.find()) {
						sectionContents = sectionContents.substring(0, chopOff.start());
					}
					sectionContents = sectionContents.trim();
				}				
				String shtml = run4_fillSection(insertSrc, var, sectionContents);
				sb.append(shtml);
			}
		});
		return html2;
	}
	
	/**
	 * 
	 * @param insertSrc The "file reference" to load
	 * @param var
	 * @param sectionContents local contents of the section tag, which can set variables or provide body content
	 * @return Note: has wrapper `section` tags
	 */
	String run4_fillSection(String insertSrc, Map var, String sectionContents) {
		File file = resolveRef(insertSrc);
		String ftype = FileUtils.getType(file);
		String sectionSrc = FileUtils.read(file);
		
		boolean srcIsHtml = ftype.endsWith("html") || ftype.endsWith("htm");
		Map varmap2 = new ArrayMap(var); // copy vars then modify (so we don't pass one sections vars into another section)
		
		if (srcIsHtml) {
			// src is html or a template
			// NB: applyMarkdown is false as md -> html is done once for the top-level page
			String shtml = run2_render(false, sectionContents, sectionSrc, varmap2);
			return shtml;
		}
		// src is a markdown file -- run it through a dummy template
		// ...chop contents into vars / contents					
		sectionContents = chopSetVars(sectionContents, varmap2);
		// Note: applyMarkdown false here, because markdown is applied later by run2_render
		// (is that best??)
		String sectionHtml = run2_render(false, sectionSrc+sectionContents, "<section>$contents</section>", varmap2);
		// strip the wrapper <section> bits? 
		// Hm: it is a valid html tag BUT we need to avoid messing up markdown rendering 
		String sectionHtml2 = sectionHtml.substring(9, sectionHtml.length() - 10);
		return sectionHtml2;		
	}

	protected String chopSetVars(String srcPage, Map varmap) {
		Matcher varm = KV.matcher(srcPage);
		int prev=0;
		while(varm.find()) {
			// +2 to allow for \r\n endings (untested)
			if (varm.start() > prev+2) break;
			String k = varm.group(1);
			String v = varm.group(2).trim();
			varmap.put(k, v);
			prev = varm.end();
		}			
		srcPage = srcPage.substring(prev).trim();
		return srcPage;
	}

	protected File resolveRef(String insert) {
		// TODO a better security check: not below webroot!
		if (insert.contains("..")) {
			throw new SecurityException("Illegal section src in "+src+": "+insert);
		}
		
		// 1. a template file??
		
		// 2. a local file?
		File fi = new File(dir, insert);
		if (fi.isFile()) return fi;
		
		// 3. a sloppy reference
		JerbilLinkResolver jlr = new JerbilLinkResolver();
		List<File> files = jlr.findFilesFromRef(insert);
		if ( ! files.isEmpty()) {
			return files.get(0);			
		}
		
		// 4. a webroot file
		File outDir = out.getParentFile();
		
		final File webroot = config.getWebRootDir(); // stop when we leave webroot
		while(outDir!=null) {
			File wf = new File(outDir, insert);
			if (wf.isFile()) {
				return wf;
			}
			outDir = outDir.getParentFile();
			if ( ! FileUtils.contains(webroot, outDir)) {
				break;
			}
		}
		
		throw Utils.runtime(new FileNotFoundException(
				insert+" referenced in "+src
				+" "));
	}

	private String addJerbilVersionMarker(String html) {
		if (config.noJerbilMarker) return html;
		String v = config.VERSION; // huh? > Environment.get().get(new SField("jerbil.version"));
		String markerInfo = "<meta name='generator' content='Jerbil v"+v+"' />\n";
		if (html.contains("meta name='generator'")) {
			return html; // avoid dupes
		}
		html = html.replace("</head>", markerInfo+"</head>");
		html = html.replace("</HEAD>", markerInfo+"</HEAD>");
		return html;
	}

	String insertVariables(String plainTextOrHtml, Map<String,Object> var, boolean applyMarkdown) {
		// TODO key: value at the top of file -> javascript jerbil.key = value variables
		// TODO files -> safely restricted file access??
		plainTextOrHtml = plainTextOrHtml.replace("$generator", "Jerbil version "+JerbilConfig.VERSION);
		plainTextOrHtml = plainTextOrHtml.replace("$webroot", ""); // TODO if dir is a sub-dir of webroot, put in a local path here, e.g. ".." 
		
		// copy defaults
		HashMap var2 = new HashMap(var);
		for(String k : var.keySet()) {
			if ( ! k.startsWith("default-") && ! k.startsWith("Default-")) continue;
			Object v = var.get(k);
			k = k.substring(8);
			if ( ! var2.containsKey(k)) {
				var2.put(k, v);
			}
		}
		var = var2;

		// convert var values to Markdown
		if (applyMarkdown) {
			Markdown markdown = Dep.get(Markdown.class);
			Map<String, Object> mdvars = Containers.applyToValues(
					v -> v instanceof String? markdown.renderWithoutWrapper((String)v) : v, var);
			var = mdvars;
		}
		
		SimpleTemplateVars stv = new SimpleTemplateVars(var);
		if ( ! Utils.isBlank(config.numberFormat)) {
			stv.setNumberFormat(new DecimalFormat(config.numberFormat));
		}
		stv.setUseJS(config.useJS);
		stv.setUseJSLite( ! config.useJS);
		String html2 = stv.process(plainTextOrHtml);
	
		// any missed vars?
		List<String> missed = stv.findVars(html2);
		if ( ! missed.isEmpty()) {
			if (config.strict) {						
				throw new IllegalArgumentException("(strict mode) Missed $variables in "+src+" "+missed);
			} else {
				Log.w(LOGTAG, "Missed $variables in "+src+" "+missed);
			}
		}
		
		return html2;
	}

	/**
	 * 
	 * @param v
	 * @return html safe and regex safe
	 */
	String escapeValue(Object v) {
		if (v==null) return "";
		String _vs = Printer.toString(v);
		// NB: md doesnt handle £s -s and puts in annoying tags
//		String _vs0 = Markdown.render(_vs).trim(); // This will handle html entities
//		// pop the outer tag that Markdown puts in
//		if (_vs.startsWith("<p>")) _vs = StrUtils.substring(_vs, 3, -4);		
//		String _vs1 = WebUtils2.attributeEncode(_vs);
		
		String _vs2 = WebUtils2.htmlEncodeWithUrlProtection(_vs);
		_vs = _vs2;
		_vs = Matcher.quoteReplacement(_vs);
		return _vs;
	}

	private void checkTemplate(String html) {
		if ( ! html.contains("$contents") && ! html.contains("$nocontents")) {
			if (src.getName().endsWith(".csv")) {
				return; // dont enforce this for csv 
			}
			throw new IllegalStateException("The template file MUST contain the $contents or $nocontents variable: "+template);
		}
	}

	/**
	 * Base vars can be over-ridden by values in the markdown file
	 * @param vars
	 */
	public void setBaseVars(Map<String, String> vars) {
		baseVars = new HashMap(vars); // paranoid copy
	}

	/**
	 * vars do NOT get overridden
	 * @param vars
	 */
	public void setVars(Map<String, String> vars) {
		this.vars = new HashMap(vars);
	}

	/**
	 * 
	 * @deprecated Normally set via Dep
	 * @param jc
	 */
	public void setConfig(JerbilConfig jc) {
		this.config = jc;
	}
	
	
}
