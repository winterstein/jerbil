package com.goodloop.jerbil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.Option;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

public class JerbilConfig {
	
	public static final String VERSION = "0.8.1";

	static final String DEFAULT_WEBROOT =  "webroot";
	
	public JerbilConfig setProjectdir(File projectdir) {
		this.projectdir = projectdir;
		return this;
	}
	
	@Option(description="Apply DecimalFormat.java formatting to numerical variables e.g. `#,###.##` gives comma-separated 1,000s and 2 decimal places.")
	public String numberFormat;
	
	@Override
	public String toString() {
		try {
			return "JerbilConfig "+JSON.toString(Containers.objectAsMap(this));
		} catch(Throwable ex) {
			return super.toString();
		}
	}
	@Option(description="If true (the default) then Jerbil will run a simple web server. You can also use nginx or apache (or anything else) instead though.")
	public boolean server = true;

	@Option(description="The port to connect to. If you wish to use Jerbil as your primary server, then set this to 80. The standard setup is to use e.g. nginx instead.")
	public int port = 8282;
	
	@Option(description = "Root directory of the website project. Defaults to the current directory.")
	public File projectdir;	
	
	// TODO make this off by default
	@Option(description="If the site is in a git-managed directory, then regularly call git pull to keep it up to date. A no-config-required alternative to web-hooks.")
	public Dt gitcheck = new Dt(1, TUnit.MINUTE);
	
	@Option(description="If true, Jerbil will try to open a web browser for you.")
	public boolean preview = true;
	
	@Option(description="If true, Jerbil will exit after build -- this disables the server and git-check.")
	public boolean exit = false;
	
	@Option
	public Map<String,String> var = new HashMap();

	@Option
	public String webroot = DEFAULT_WEBROOT;
	
	@Option
	public String pages = "pages";
	
	@Option(description = "If set, a folder of source style files, e.g. .less files to turn into .css. Use with styleCompiler") 
	File styleSrcDir;
	
	@Option(description = "If set, a command to compile the style files. e.g. `lessc $input style/$output`" 
			+"\n   $input and $output will be replaced with the input file (full path), and filename.css")
	public String styleCompiler;

	public File getPagesDir() {
		return new File(projectdir, pages);
	}
	
	@Option
	public boolean noJerbilMarker;

	@Option
	public boolean editor = true;

	@Option(description="Download a fresh copy of Jerbil itself (but you'll have to copy the jar into place)")
	public boolean update;

	@Option(description="If you just wish to process a single file. See also -filter", tokens="-i,-inputFile")
	public File inputFile;
	
	@Option(description="Usually unset. Glob pattern if you just wish to process just a set of files eg \"*nda*\"")
	public String filter;

	@Option(description="If true, support js in templates, e.g. ${foo? 'bar':''}")
	public boolean useJS;

	@Option(description="(experimental) Glob pattern eg \"*contract*.txt\" for files to convert into pdf. If unset (the default), then pdfs are not made.")
	public String makePdfPattern;
	
	// HACK share certificates
	@Option(description="(experimental) Extra options to pass to the pdf generator")
	public String makePdfOptions="--include-background";


	@Option(description="If set, create divs to enclose the sections implicitly created by headers h1, h2 ...upto this number.\n"
			+"You can also set this in individual page source files, using a sectionDivs: boolean|int parameter.")
	public int sectionDivs;
	
	public File getWebRootDir() {
		return new File(projectdir, webroot);
	}

	public File getStyleSrcDir() {
		if (styleSrcDir==null) return null;
		if (styleSrcDir.isAbsolute()) return styleSrcDir;
		File srcdir = new File(projectdir, styleSrcDir.toString());		
		return srcdir;
	}

	public File getTemplatesDir() {
		File td = new File(projectdir, "templates");
		return td.isDirectory()? td : null;
	}
}
