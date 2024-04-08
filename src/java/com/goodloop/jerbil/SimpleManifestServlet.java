package com.goodloop.jerbil;

import com.winterwell.utils.Dep;
import com.winterwell.web.ajax.JSend;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.WebRequest;

public class SimpleManifestServlet implements IServlet {

	@Override
	public void process(WebRequest state) throws Exception {
		JerbilConfig jc = Dep.get(JerbilConfig.class);
		JSend jsend = new JSend(jc);
		jsend.send(state);
	}

}
