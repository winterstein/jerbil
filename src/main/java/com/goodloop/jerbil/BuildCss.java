package com.goodloop.jerbil;

import java.io.File;
import java.io.IOException;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.ProcessTask;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.TUnit;

public class BuildCss extends BuildTask {

		
	private String styleCompiler;
	private File styleSrcDir;
	private File workingDir;

	/**
	 * 
	 * @param workingDir Usually the webroot dir
	 */
	public void setWorkingDir(File workingDir) {
		this.workingDir = workingDir;
	}
	
	public BuildCss(String styleCompiler, File styleSrcDir) {
		this.styleCompiler = styleCompiler;
		this.styleSrcDir= styleSrcDir;
		if (styleSrcDir==null) {
			throw new IllegalArgumentException("No styleSrcDir set for styleCompiler");
		}
	}

	@Override
	protected void doTask() throws Exception {
		if ( ! styleSrcDir.isDirectory()) {
			throw Utils.runtime(new IOException("No such directory! styleCompiler requested for styleSrcDir: "+styleSrcDir));
		}
		for(File f : styleSrcDir.listFiles()) {
			ProcessTask proc = null;
			try {
				// hack
				if ( ! f.getName().endsWith("less")) continue;
				if ( ! f.getName().contains("main")) continue;
				
				String cmd = styleCompiler.replace("$input", f.getAbsolutePath());
				cmd = cmd.replace("$output", FileUtils.changeType(f, "css").getName());
				proc = new ProcessTask(cmd);
				proc.setDirectory(workingDir);
				proc.setMaxTime(TUnit.MINUTE.dt);
				Log.d(LOGTAG, "dir: "+workingDir+" command: "+cmd);
				proc.run();
			} finally {
				FileUtils.close(proc);
			}
		}
	}
	
}
