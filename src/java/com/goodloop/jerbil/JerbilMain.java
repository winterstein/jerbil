package com.goodloop.jerbil;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.logging.Level;

import com.winterwell.bob.Bob;
import com.winterwell.bob.tasks.Classpath;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Environment;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.io.FileEvent;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.WatchFiles;
import com.winterwell.utils.io.WatchFiles.IListenToFileEvents;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.app.FileServlet;
import com.winterwell.web.app.HttpServletWrapper;
import com.winterwell.web.app.JettyLauncher;
import com.winterwell.web.fields.SField;



/**
 * Command line entry point for Jerbil:
 * 
 * java -cp jerbil.jar:lib/* Jerbil
 * 
 * @author daniel
 *
 */
public class JerbilMain {

	private static final String LOGTAG = "Jerbil";
	private static BuildJerbilWebSite b;
	private static GitCheck gitCheck;

	/**
	 * Watch for edits and keep rebuilding!
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Environment.get().put(new SField("jerbil.version"), JerbilConfig.VERSION);		

		JerbilConfig config;
		try {
			config = getConfig(args);
		} catch(Throwable ex) {
			Log.e(LOGTAG, ex);
			showHelp(null);		
			return;
		}
		// help?
		if (args.length==1 && ("--help".equals(args[0]) || "-help".equals(args[0]))) {
			showHelp(config);
			return;
		}

		// update?
		if (config.update) {
			doUpdateJar();
			// exit
			return;
		}
		
		if (config.projectdir==null) {
			// fail helpfully
			showHelp(config);
			System.err.println("Run in a Jerbil website project directory -- or with the path to one as a parameter.");
			System.exit(-1);
		}
		if ( ! config.projectdir.exists()) {
			System.err.println("The project directory (or file) "+config.projectdir+" does not exist?!");
			System.exit(-1);
		}
		
		init(config);
		
		// build
		b = new BuildJerbilWebSite(config);
		if (config.inputFile==null) {
			// normal case - build the whole site
			b.run();
		} else {
			// just build one file (command line utility use-case)
			b.singleFile = true;
			b.doTask3_oneFile(config.inputFile);
		}
		// exit?
		if (config.exit) {
			// inform the user
			if (config.server) Log.w("jerbil.exit", "NOT starting a server - exit=true beats server=true");
			if (config.gitcheck!=null) Log.w("jerbil.exit", "NOT polling for git updates - exit=true beats gitcheck=true");
			return;
		}
		// NB: dont skip repeat builds (otherwise watch-for-updates doesnt work)
		Bob.getSingleton().getConfig().clean = true;
		
		// run a web server? TODO maybe move this earlier, so pdf gen can use it?
		if (config.server) {
			runServer(config);
		}
		
		// watch for file edits
		runWatcher(config);
		// watch for git edits
		if (config.gitcheck!=null) {
			gitCheck = new GitCheck(config.projectdir, config.gitcheck);
			gitCheck.start();
		}
		
		if (config.server && config.preview) {
			WebUtils2.display(WebUtils.URI("http://localhost:"+config.port));
		}
		// spin the main thread
		while(true) {
			Utils.sleep(10000);
		}
	}

	private static void init(JerbilConfig config) {
		Markdown markdown = new Markdown();
		Dep.set(Markdown.class, markdown);
//		// out in color? (build issues, Jul 2021, meh, retry in a bit)
//		if (Utils.OSisUnix() && GuiUtils.isInteractive()) {
//			SystemOutLogListener ll = Containers.firstClass(Log.getListeners(), SystemOutLogListener.class);
//			if (ll!=null) {
//				ll.setUseColor(true);
//			}
//		}
	}

	private static void showHelp(JerbilConfig config) {
		System.out.println("");
		System.out.println("Jerbil website builder, version "+JerbilConfig.VERSION+" "+Utils.or(getMyJar(),""));
		System.out.println("----------------------------------------");
		System.out.println("");
		ConfigBuilder cb;
		try {
			cb = ConfigFactory.get().getConfigBuilder(JerbilConfig.class);	
		} catch(Throwable ex) { // paranoia
			if (config==null) config = new JerbilConfig();
			cb = new ConfigBuilder(config);
		}		
		String optionsMessage = cb.getOptionsMessage("[path to website directory] If no path is given, defaults to the current directory");
		System.out.println(optionsMessage);
	}

	static void doUpdateJar() {
		Log.d(LOGTAG, "update Jerbil...");
		FakeBrowser fb = new FakeBrowser();
		fb.setMaxDownload(50); // 50mb?!
		File bobJar = fb.getFile("https://www.winterwell.com/software/downloads/jerbil-all.jar");
		System.out.println("Jerbil jar downloaded to:");
		System.out.println(bobJar);
		File myJar = getMyJar();
		if (myJar!=null && bobJar.length() > 0) {
			FileUtils.copy(myJar, FileUtils.changeType(myJar, ".jar.old"));
			FileUtils.move(bobJar, myJar);
			Log.d(LOGTAG, "Fresh Jerbil moved to "+myJar);
		}
	}

	private static File getMyJar() {
		String classpath = System.getProperty("java.class.path");
		Classpath cp = new Classpath(classpath);
		List<File> old = Containers.filter(cp.getFiles(), f -> f.getName().equals("jerbil-all.jar"));
		if (old.size() == 1) return old.get(0);
		
		List<File> olds = Containers.filter(cp.getFiles(), f -> f.getName().equals("jerbil.jar"));
		if (olds.size() == 1) return olds.get(0);
		
		return null;
	}

	private static void runServer(JerbilConfig config) {
		File webroot = b.getWebroot();
		JettyLauncher jl = new JettyLauncher(webroot, config.port);
		jl.setWebXmlFile(null);
		jl.setCanShutdown(false);
		jl.setup();				
		FileServlet fileServer = new FileServlet(webroot);
		// servlets
		jl.addServlet("/manifest", SimpleManifestServlet.class);
		if (config.editor) {
			jl.addServlet("/pages/*", PageEditorServlet.class);
			FileServlet edfileServer = new FileServlet(new File("/home/daniel/winterwell/jerbil/web")); // HACK FIXME
			edfileServer.setChopServlet(true);
			jl.addServlet("/editor/*", edfileServer);
		}
		jl.addServlet("/*", fileServer);		
		
		Log.report("web", "...Launching Jetty web server on port "+config.port, Level.INFO);
		
		jl.run();		
	}

	/**
	 * Where is the project
	 * @param args
	 * @return Can be null
	 */
	private static JerbilConfig getConfig(String[] args) {
		ConfigFactory cf = ConfigFactory.get().setArgs(args);
		ConfigBuilder cb = cf.getConfigBuilder(JerbilConfig.class);
		JerbilConfig config = cb.get();
		
		List<String> leftoverArgs = cb.getRemainderArgs();
		File dir = config.projectdir!=null? config.projectdir : getConfig2_dir(leftoverArgs);
		if (dir==null) {
			throw new InvalidParameterException("No projectDir, and the current directory is not a Jerbil project as it has no "+config.webroot);
		}
		if (dir.isFile()) {
			// oh - just process a file
			config.inputFile = dir;
			config.projectdir = FileUtils.getWorkingDirectory();
			if (config.noHeaderLinks==null) config.noHeaderLinks = true; 
			if ( ! config.getWebRootDir().isDirectory()) {
				Log.d(LOGTAG, "Just an input file and no webroot directory --> do a local md to html conversion");
				config.webroot = ".";
				config.exit = true;
				assert config.getWebRootDir().getAbsoluteFile().equals(config.projectdir.getAbsoluteFile());
			}
		} else {
			config.projectdir = dir;
			// add dir's config properties, which could have been missed by the "global" files above
			File f1 = new File(dir, "config/jerbil.properties").getAbsoluteFile();
			File f2 = new File(dir, "config/"+cf.getMachine()+".properties").getAbsoluteFile();
			for(File f : new File[] {f1,f2}) {
				if (f.exists()) {
					cb = new ConfigBuilder(config);
					config = cb
							.set(f)
							.setFromMain(args) // args walways win
							.get();
				}
			}
		}
		Log.d("init", "Config:	"+config);
		Dep.set(JerbilConfig.class, config);
		return config;	
	}

	/**
	 * 
	 * @param leftoverArgs
	 * @return 1st argument (which might be a file not a directory!), or current directory, or ask, or null
	 */
	private static File getConfig2_dir(List<String> leftoverArgs) {
		if ( ! leftoverArgs.isEmpty()) {
			File dir =new File(leftoverArgs.get(0));
			return dir;
		}		
		// are we in a Jerbil dir?
		File wd = FileUtils.getWorkingDirectory();
		if (new File(wd, JerbilConfig.DEFAULT_WEBROOT).isDirectory() || new File(wd, "config/jerbil.properties").isFile()) {
			return wd;
		}			
		// Ask
		File dir = GuiUtils.selectFile("Pick website project's base directory", null, new FileFilter() {				
			@Override
			public boolean accept(File pathname) {
				return pathname!=null && pathname.isDirectory();
			}
		});
		return dir;
	}

	protected static void runWatcher(JerbilConfig config) throws IOException {		
		Log.d("Watching "+b.getProjectDir()+" (pages, style, templates) for edits");		
		{
			WatchFiles watch = new WatchFiles();			
			watch.addDir(config.getPagesDir());		
			if (config.getStyleSrcDir()!=null) {
				watch.addDir(config.getStyleSrcDir());
			}
			if (config.getTemplatesDir()!=null) {
				watch.addDir(config.getTemplatesDir());
			}
			watch.addListener(new IListenToFileEvents() {
				@Override
				public void processEvent(FileEvent pair2) {
					Log.d(LOGTAG, "\n\n"+pair2+"\n\n");
					b.run();
				}			
			});						
			Thread watchThread = new Thread(watch);
			watchThread.setName("watch-"+b.getProjectDir().getName());
			watchThread.start();
		}
		{	// templates
			WatchFiles watch = new WatchFiles();
			// TODO a better filter for safety
			watch.addDir(new File(b.getProjectDir(), config.webroot));							
			watch.addListener(new IListenToFileEvents() {
				@Override
				public void processEvent(FileEvent fe) {
					if (fe.file.toString().contains("template")) {
						b.run();
					}
				}			
			});						
			Thread watchThread = new Thread(watch);
			watchThread.setName("watch-"+b.getProjectDir().getName()+" template");
			watchThread.start();
		}	
	}
}
