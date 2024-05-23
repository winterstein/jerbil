package com.goodloop.jerbil;


import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;

/**
 * The latest Jerbil bundle can be downloaded from
 * https://www.winterwell.com/software/downloads/jerbil-all.jar
 * 
 * See npm jerbil-cms 
 * 
 * @author daniel
 *
 */
public class BuildJerbil extends BuildWinterwellProject {

	public BuildJerbil() {
		super("jerbil");
		setIncSrc(true);
		setMainClass("Jerbil");
		setVersion("1.2.6"); // copy JerbilConfig.VERSION
		setMakeFatJar(true);
		setScpToWW(false); // NB: test first
//		setScpToWW(true); // NB: test first
	}

	
	@Override
	public List<BuildTask> getDependencies() {
		List<BuildTask> deps = super.getDependencies();
		
		MavenDependencyTask mdt = new MavenDependencyTask();
		mdt.addDependency("com.microsoft.playwright", "playwright", "1.28.1");

		mdt.addDependency("org.eclipse.jetty.toolchain", "jetty-jakarta-servlet-api", "5.0.2");
		
		// Flxemark Maven just downloads a bunch of _empty_ jars!
//		MavenDependencyTask mdt = new MavenDependencyTask();
//		mdt.addDependency("com.vladsch.flexmark","flexmark-all","0.62.2");
		
		deps.add(mdt);
		
		return deps;
	}
}
