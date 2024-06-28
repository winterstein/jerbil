package com.goodloop.jerbil;

import java.io.File;

import org.junit.Test;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.time.TUnit;

public class GitCheckTest {

	@Test
	public void testIsGitDir() {
		GitCheck gc = new GitCheck(new File("/home"), TUnit.MINUTE.dt);
		assert ! gc.isGitDir();
		
		GitCheck gc2 = new GitCheck(FileUtils.getWorkingDirectory(), TUnit.MINUTE.dt);
		assert gc2.isGitDir();
	}

}
