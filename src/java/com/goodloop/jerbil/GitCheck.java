package com.goodloop.jerbil;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import com.winterwell.bob.tasks.GitTask;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Proc;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

/**
 * Regularly pull from git. Assumes that a separate file-watcher will handle updates.
 * @author daniel
 *
 */
public class GitCheck extends TimerTask {

	private static final String LOGTAG = "gitcheck";
	private File dir;
	private Timer timer;
	private Dt dt;
	private Throwable error;
	private GitCheckConfig config;

	public GitCheck(File projectdir, Dt dt) {
		this.dir = projectdir;
		this.dt = dt;
	}

	public void start() {
		timer = new Timer();
		timer.schedule(this, 10, dt.getMillisecs());
	}

	@Override
	public void run() {
		// are we in a git directory??
		if ( ! isGitDir()) {
			Log.e("gitcheck", dir+" is not a git managed directory. Stopping gitcheck.");
			timer.cancel();
			return;
		}
		try {			
			// Pull
			GitTask pull = new GitTask(GitTask.PULL, dir);
			pull.doTask();
			
			// process?
			String out = pull.getOutput();
			pull.close();
			error = null;	
			Log.d("gitcheck", out);
			if (out.contains("Already up-to-date")) {
				return;
			} else if (out.contains("Updating")) {
				onUpdate();
			}
		} catch(Throwable ex) {			
			error = ex;
			Log.e(LOGTAG, ex);
			// stop??
		}
	}

	private void onUpdate() {
		// run on-update command?
		if (config!=null && config.command!=null) {
			Proc proc = new Proc(config.command);
			Log.i(LOGTAG, "Run "+config.command);
			proc.start();
			proc.waitFor(TUnit.MINUTE.dt);									
			if ( ! Utils.isBlank(proc.getError())) {
				Log.w(LOGTAG, proc.getError());
			}
			Log.d(LOGTAG, proc.getOutput());
			proc.close();
		}
	}
	
	public static void main(String[] args) {
		// TODO an option for the frequency
		GitCheckConfig config = ConfigFactory.get().setArgs(args).getConfigBuilder(GitCheckConfig.class).setFromMain(args).get();		
		GitCheck gc = new GitCheck(FileUtils.getWorkingDirectory(), config.dt);
		gc.config = config;
		Printer.out(gc.config);
		gc.start();
	}

	public boolean isGitDir() {
		if (new File(dir, ".git").isDirectory()) return true;
		return false;
		
//		try {
//			Map<String, Object> info = GitTask.getLastCommitInfo(dir);
//			System.out.println(info);	
//			return true;
//		} catch(IllegalArgumentException ex) {			
//			return false;
//		}
	}

}
