package jerbil;

import java.io.File;
import java.util.List;

import com.goodloop.jerbil.JerbilConfig;
import com.winterwell.bob.BuildTask;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.io.FileUtils;

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
		setVersion(JerbilConfig.VERSION);
		setMakeFatJar(true);
	}

}
