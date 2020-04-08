package com.goodloop.jerbil;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import com.winterwell.utils.Dep;
import com.winterwell.utils.io.FileUtils;

public class BuildJerbilWebSiteTest {

	@Test
	public void testDoTask3_oneFile_DWjobContract() {
		JerbilConfig config = new JerbilConfig();
		config.setProjectdir(new File(FileUtils.getWinterwellDir(), "Useful-Legal-Docs-for-a-UK-StartUp"));
		Dep.set(JerbilConfig.class, config);
		
		BuildJerbilWebSite bjw = new BuildJerbilWebSite(config);
		
		File csvFile = new File(config.getPagesDir(), "job-contracts/staff.csv");
		bjw.doTask3_CSV(csvFile);
	}

}
