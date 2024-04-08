package com.goodloop.jerbil;

import java.io.File;

import org.junit.Test;

import com.winterwell.utils.Dep;
import com.winterwell.utils.DepContext;
import com.winterwell.utils.io.FileUtils;

public class BuildJerbilWebSiteTest {


	@Test 
	public void testSmokeTestDocProject() {
		DepContext context = Dep.setContext("DocProject");
		
		JerbilConfig config = new JerbilConfig();
		config.setProjectdir(new File(FileUtils.getWinterwellDir(), "doc"));
		Dep.set(JerbilConfig.class, config);
		Dep.set(Markdown.class, new Markdown());
		
		BuildJerbilWebSite bjw = new BuildJerbilWebSite(config);
		bjw.run();
		
		// assert context behaves as we think it does 
		JerbilConfig jc = Dep.get(JerbilConfig.class);
		// clean-up
		context.close();
		
		assert jc == config;
		JerbilConfig jcold = Dep.has(JerbilConfig.class)? Dep.get(JerbilConfig.class) : null;
		assert jcold != config;
	}
	
//	@Test // convenience for a filtered run - not a test
	public void testDoTask3_oneFile_DWjobContract() {
		JerbilConfig config = new JerbilConfig();
		config.makePdfPattern="**/job-contracts/*.html,**/directors/*.html";
		config.setProjectdir(new File(FileUtils.getWinterwellDir(), "Useful-Legal-Docs-for-a-UK-StartUp"));
		Dep.set(JerbilConfig.class, config);
		
		BuildJerbilWebSite bjw = new BuildJerbilWebSite(config);
		
		File csvFile = new File(config.getPagesDir(), "job-contracts/staff.csv");
		bjw.doTask3_CSV(csvFile);
	}


//	@Test convenience for a filtered run - not a test
	public void testDoTask3_oneFile_directorsContract() {
		JerbilConfig config = new JerbilConfig();
		config.makePdfPattern="**/job-contracts/*.html,**/directors/*.html";
//		config.useJS = true;
		config.setProjectdir(new File(FileUtils.getWinterwellDir(), "Useful-Legal-Docs-for-a-UK-StartUp"));
		Dep.set(JerbilConfig.class, config);
		
		BuildJerbilWebSite bjw = new BuildJerbilWebSite(config);
		
		File csvFile = new File(config.getPagesDir(), "directors/directors-contract.csv");
		assert csvFile.isFile();
		bjw.doTask3_CSV(csvFile);
	}
}
