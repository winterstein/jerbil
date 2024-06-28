package com.goodloop.jerbil;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.CSVSpec;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.web.WebPage;

/**
 * Build a Jerbil website.
 * .txt and .md files in the pages directory are converted into .html in the webroot directory.
 * @author daniel
 * @testedby BuildJerbilWebSiteTest
 */
public class BuildJerbilWebSite extends BuildTask {
	
	JerbilConfig config;	
	File pages;
	File webroot;
	boolean singleFile;
	
	public File getWebroot() {
		return webroot;
	}
	
	public File getProjectDir() {
		return config.projectdir;
	}
	
	public BuildJerbilWebSite(File projectDir) {
		this(new JerbilConfig().setProjectdir(projectDir));
	}
	
	public BuildJerbilWebSite(JerbilConfig config) {
		this.config = config;
		pages = config.getPagesDir();
		webroot = config.getWebRootDir();
	}
	
	@Override
	protected void doTask() throws Exception {
		assert pages != null : this;
		if ( ! pages.isDirectory()) {
			throw new IllegalArgumentException(pages+" is not a directory");
		}
		doTask2(pages);
		
		// css compilation?
		if ( ! Utils.isBlank(config.styleCompiler)) {
			doTask2_css();
		}
		Log.report(LOGTAG, "Website build done :)", Log.SUCCESS);
	}
	
	private void doTask2_css() throws Exception {				
		File srcdir = config.getStyleSrcDir();
		BuildCss bc = new BuildCss(config.styleCompiler, srcdir);
		bc.setWorkingDir(config.getWebRootDir());
		bc.doTask();
		bc.close();
	}

	/**
	 * dir pages
	 * */
	private void doTask2(File dir) {
		if ( ! dir.isDirectory()) {
			throw Utils.runtime(new IOException("Not a directory: "+dir));
		}
		for(File f : dir.listFiles()) {
			if (f.isDirectory()) {
				doTask2(f);
				continue;
			}
			if ( ! f.isFile()) {
				// huh?
				continue;
			}
			
			boolean ok = filterFile(f);
			if ( ! ok) continue;
			
			doTask3_oneFile(f);
		}
	}

	/**
	 * See {@link JerbilConfig#filter}
	 * @param f
	 * @return usually true for "do this"
	 */
	private boolean filterFile(File f) {
		String filter = config.getFilter();
		if (Utils.isBlank(filter)) return true;
		String[] ps = filter.split(",\\w*");
		if (ps.length==1) {
			
		}
		String match = Containers.first(Arrays.asList(ps), p -> FileUtils.globMatch(p, f));
		if (match==null) {
			return false;
		}
		return true;
	}

	void doTask3_oneFile(File f) {
		// "mail merge"?
		if ( f.getName().endsWith(".csv")) {
			doTask3_CSV(f);
			return;
		}
		// ignore templates
		if (FileUtils.changeType(f, "").getName().equals("template")) {
			Log.i(LOGTAG, "Skip processing on "+f);
			return;
		}
		// a stray binary or other file? just copy it
		String type = FileUtils.getType(f).toLowerCase();
		// see https://fileinfo.com/filetypes/text
		if ( ! type.isEmpty() && ! "txt md markdown text html htm rtf wiki me 1st ascii asc eml".contains(type)) {
			Log.d(LOGTAG, "Copy as-is "+f);
			String relpath = FileUtils.getRelativePath(f, pages);		
			File out = new File(webroot, relpath);
			out.getParentFile().mkdirs();
			FileUtils.copy(f, out);
			return;
		}

		// Process a file!
		File out = getOutputFileForSource(f);
		
		File template = getTemplate(out);
		assert template != null : "No html template?! "+webroot;
		
		BuildJerbilPage bjp = new BuildJerbilPage(f, out, template);
		Map<String, String> vars = config.var;
		bjp.setBaseVars(vars);
		bjp.run();		
	}

	void doTask3_CSV(File csvFile) {
		CSVSpec spec = new CSVSpec();
		try (CSVReader r = new CSVReader(csvFile,spec)) {
			r.setNumFields(-1);
			// the 1st line MUST be column headers
			String[] header = r.next();
			// TODO case etc flexible header handling (as SoGive's csv code does)
			// 2 stage templating?
			File mdtemplateFile = getSrcTemplate(csvFile);
			String mdtemplate;
			if (mdtemplateFile != null) {
				mdtemplate = FileUtils.read(mdtemplateFile);
				Log.d(LOGTAG, ".csv "+csvFile+" - Using template file "+mdtemplateFile);
			} else {
				mdtemplate = ""; // blank
				Log.d(LOGTAG, ".csv "+csvFile+" - blank template");
			}
			
			for (String[] row : r) {
				if (row.length==0) continue;
				doTask4_CSV_row(csvFile, header, mdtemplate, row, r.getRowNumber());
			}
		} catch(Exception ex) {
			Log.e(ex+" from "+csvFile); 
			// TODO better error handling in Jerbil??
		}
	}

	private void doTask4_CSV_row(File csvFile, String[] header, String mdtemplate, String[] row, int rowNum) {
		assert mdtemplate != null;
		// turn a row into a map of key:value variables
		HashMap<String,String> map = new HashMap();
		for (int i = 0; i < header.length; i++) {
			String hi = header[i];
			if (Utils.isBlank(hi)) continue;
			hi = hi.trim();
			hi = hi.replaceAll("\\s+", "_"); // no whitespace in variable names
			hi = hi.replaceAll("\\W+", ""); // no punctuation
			String ri = row[i].trim();
			map.put(hi, ri);
		}
		
		
		// HACK
		if (csvFile.getName().equals("contact-details.csv")) {
			return;
		}
		// rm blanks
		for(Object k : map.keySet().toArray()) {
			if (Utils.isBlank(map.get(k))) {
				map.remove(k);
			}
		}
		
		// HACK names etc
		hack_specialVars(map);				
		
		// ...normal
		File out = getOutputFileForSource(csvFile);				
		out = FileUtils.changeType(out, rowNum+FileUtils.safeFilename(row[0].trim(), false)+".html");		
		File template = getTemplate(out);
		assert template != null : "No html template?! "+webroot;
		
		// ...run
		BuildJerbilPage bjp = new BuildJerbilPage(csvFile, mdtemplate, out, template);
		// combine base vars and csv vars
		Map<String, String> vars = new HashMap(config.var);
		vars.putAll(map);		
		bjp.setVars(vars);
		// run
		bjp.run();
	}
	
	

	private void hack_specialVars(HashMap<String, String> map) {
		String name = Utils.or((String) map.get("worker"), " ");
		if (name.startsWith("X")) {
			return; // skip
		}
		CSVReader r = new CSVReader(new File(config.getPagesDir(), "/job-contracts/contact-details.csv"));
		r.setNumFields(-1);
		Iterable<Map<String, String>> maps = r.asListOfMaps();
		Map<String, String> details = null;
		for (Map<String, String> m : maps) {
			String ms = Printer.toString(m);
			if (ms.contains(name)) {
				details = m;
				break;
			}
		}
	
		if (details!=null) {
			map.put("workerEmail", details.get("Email address"));
			String address = details.get("Home address");
			map.put("workerAddress", address);
			if (address.contains("London"))
			{
				map.put("city", "London");
				map.put("jurisdiction", "England and Wales");
				map.put("jurisdictionAdj", "English");
				map.put("scotlandStyle", "display:hidden;");
			} else if (address.contains("Edinburgh") || address.contains("EH") || name.contains("Eve")) {
				map.put("witness", "## Witness\n\n"
				+"Note: The witness is not a party to the contract, and does not have to read or agree with it.\n\n"
				+"> As a witness, I confirm that the signature above was made by the Employee "+map.get("worker")+" agreeing to this contract.\n\n" 
				+"<div style='height:100px;'></div>\n\n"
				+"### Full name, home address and occupation of witness\n\n");
			} else {							
				Log.e(LOGTAG, "What country?! "+name+address);
			}
		} else {
			Log.e(LOGTAG, "Who?! "+name);
		}
		String bs = (String) map.get("baseSalary");
		if (bs!=null && ! bs.startsWith("£")) {
			map.put("baseSalary", "£"+bs);
		}
		String ds = (String) map.get("discretionarySalary");
		if ( ! Utils.isBlank(ds) && ! ds.startsWith("£")) {
			map.put("discretionarySalary", "**£"+ds+"**");
		}
		if ( ! map.containsKey("discretionarySalary")) {
			map.put("discretionarySalary", "£0");
		}
		// WW

		map.put("withWinterwell","");
	}

	protected File getOutputFileForSource(File f) {
		File out;
		try {
			String relpath = FileUtils.getRelativePath(f, pages);		
			out = new File(webroot, relpath);
		} catch (IllegalArgumentException notInPages) {
			// allow for "user said _this_ file"
			if (f.equals(config.inputFile)
					&& ! "html".equals(FileUtils.getType(f))) {
				out = f;
			} else {
				throw notInPages;
			}
		}
		out = FileUtils.changeType(out, "html");
		return out;
	}

	/**
	 * Checks:
	 * 
	 * output-directory
	 * templates
	 * webroot
	 * 
	 * @param outputFile
	 * @return template.html
	 */
	protected File getTemplate(File outputFile) {
		// local
		File dir = outputFile.isDirectory()? outputFile : outputFile.getAbsoluteFile().getParentFile();
		while(true) {
			File tf = new File(dir, "template.html");
			if (tf.exists()) return tf;
			// recurse?			
			if (dir.equals(webroot)) {
				break;
			}
			dir = dir.getParentFile();
			assert FileUtils.contains(webroot, dir) : dir;
		}
		
		// templates folder?
		if (config.getTemplatesDir()!=null) {
			File tf = new File(config.getTemplatesDir(), "template.html");
			if (tf.exists()) return tf;
		}
		// default
		File dflt = new File(webroot, "template.html");
		if (dflt.isFile()) {
			return dflt;
		}
		// HACK - make a stub
		if (singleFile) {
			try {
				File temp = File.createTempFile("template", ".html");
				WebPage wp = new WebPage();
				wp.append("$contents");
				FileUtils.write(temp, wp.toString());
				return temp;
			} catch(IOException ex) {
				throw Utils.runtime(ex);
			}
		}
		return dflt;
	}

	/**
	 * A src template is a markdown file used to process .csv
	 * @param outputFile
	 * @return can be null
	 */
	protected File getSrcTemplate(File csvFile) {
		assert csvFile != null;
		File dir = csvFile.getParentFile();
		// allow a few options
		File tf1 = FileUtils.changeType(csvFile, "md");
		File tf2 = FileUtils.changeType(csvFile, "txt");
		assert dir != null : csvFile;
		File tf3 = new File(dir, "template.md");
		File tf4 = new File(dir, "template.html");		
		List<File> templates = Containers.filter(Arrays.asList(tf1,tf2,tf3,tf4), f -> f.isFile());
		if (Utils.isEmpty(templates)) {
			return null;		
		}
		if (templates.size() > 1) {
			Log.w(LOGTAG, "Multiple source templates available (only the first will be used) for "+csvFile+": "+templates);
		}
		return templates.get(0);
	}
	
	

}
