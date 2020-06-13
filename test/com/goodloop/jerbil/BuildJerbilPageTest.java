package com.goodloop.jerbil;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Test;

import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.web.WebUtils;

public class BuildJerbilPageTest {

	@Test
	public void testEscapeValue() {
		init();
		BuildJerbilPage bjp = new BuildJerbilPage(new File(""), "dummy", new File(""), new File(""));
		Map io = new ArrayMap(
			"Just some vanilla text :)", "Just some vanilla text :)",
//			"Hello **world**", "Hello <strong>world</strong>", No md support
			-10, "-10",
			"a & b", "a &amp; b",
			"https://example.com/foo?bar=7&x=2", "https://example.com/foo?bar=7&x=2",
			"£5", "&pound;5" // ??
		);
		for(Object i : io.keySet()) {
			Object o = io.get(i);
			assert bjp.escapeValue(i).equals(o) : bjp.escapeValue(i);
		}	
	}
	
	@Test
	public void testResolveRef() {
		init();
		JerbilConfig jc = Dep.get(JerbilConfig.class);
		
		BuildJerbilPage bjp = new BuildJerbilPage(new File(jc.getPagesDir(), "mypage.md"), new File(jc.getWebRootDir(), "mypage.html"), new File(jc.getWebRootDir(), "template.html"));
				
		File r = bjp.resolveRef("footer.html");		
		assert r.toString().endsWith("webroot/footer.html");
		
		File sub = bjp.resolveRef("mysubpage.md");
		assert sub.equals(new File(jc.getPagesDir(), "mysubpage.md")) : sub;
		
		File sub2 = bjp.resolveRef("mysubpage");
		assert sub2.equals(new File(jc.getPagesDir(), "mysubpage.md")) : sub2;
	}

	
	private void init() {
		if (Dep.has(JerbilConfig.class)) return;
		
		JerbilConfig jc = new JerbilConfig();
		jc.projectdir = new File("example");
		Dep.set(JerbilConfig.class, jc);
		jc.getPagesDir().mkdirs();
		jc.getWebRootDir().mkdirs();
		
		Dep.set(Markdown.class, new Markdown());
	}


	@Test
	public void testRun() {
		init();
		JerbilConfig jc = Dep.get(JerbilConfig.class);
		
		BuildJerbilPage bjp = new BuildJerbilPage(new File(jc.getPagesDir(), "mypage.md"), new File(jc.getWebRootDir(), "mypage.html"), new File(jc.getWebRootDir(), "template.html"));		
		bjp.run();
		
		WebUtils.display(bjp.getOut());
	}

}
