package com.goodloop.jerbil;

import java.io.File;

import org.junit.Test;

public class BuildCssTest {

	@Test
	public void testDoTask() throws Exception {		
		JerbilConfig jc = new JerbilConfig();
		jc.styleCompiler = "lessc $input webroot/style/$output";
		jc.styleSrcDir = new File("/home/daniel/winterwell/website/src/style");
		
		BuildCss bc = new BuildCss(jc.styleCompiler, jc.getStyleSrcDir());
		bc.setWorkingDir(jc.getStyleSrcDir().getParentFile().getParentFile());
		bc.doTask();
	}

}
