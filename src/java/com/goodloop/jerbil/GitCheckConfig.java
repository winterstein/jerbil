package com.goodloop.jerbil;

import com.winterwell.utils.io.Option;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

public class GitCheckConfig {

	@Override
	public String toString() {
		return "GitCheckConfig [dt=" + dt + ", command=" + command + "]";
	}

	@Option
	public Dt dt = new Dt(20, TUnit.SECOND);
	
	@Option(description = "If set, run this command after any git pull that results in an update")
	public String command;
		
}
