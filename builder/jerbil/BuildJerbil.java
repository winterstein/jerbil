package jerbil;


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
		setVersion("0.7.2-June2020"); //JerbilConfig.VERSION);
		setMakeFatJar(true);
	}

}
