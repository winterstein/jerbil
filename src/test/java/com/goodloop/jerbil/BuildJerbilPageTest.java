package com.goodloop.jerbil;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;

import org.junit.BeforeClass;
import org.junit.Test;

import com.winterwell.utils.Dep;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.web.WebUtils;

public class BuildJerbilPageTest {

	@Test
	public void testEscapeValue() {
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
	public void testKV() {
		Matcher m = BuildJerbilPage.KV.matcher("foo:bar\ndefault-colour: green\n");
		m.find();
		assertEquals("foo:bar", m.group());
		m.find();
		assertEquals("default-colour: green", m.group());
	}
	

	@Test
	public void testInlineCodeAndBackticks() {
		BuildJerbilPage bjp = new BuildJerbilPage(new File(""), "Hello `world`", new File(""), new File(""));
		String output = bjp.run2_render(true, "Hello `world`", "$contents", new ArrayMap());
		assert output.contains("Hello <code>world</code>");
	}
	
	
	@Test
	public void testFillSectionIsIdempotent() {
		BuildJerbilPage bjp = new BuildJerbilPage(new File(""), "dummy", new File(""), new File(""));
		Map var = new ArrayMap();
		String html = "Foo <section src='pageslice.html'>\nname: Alfred\n\nIpsum lorem</section>";
		String html2 = bjp.run3_fillSections(html, var);	
		String html3 = bjp.run3_fillSections(html2, var);
		assert html3.equals(html2);
	}
	
	@Test
	public void testResolveRef() {
		JerbilConfig jc = Dep.get(JerbilConfig.class);
		
		BuildJerbilPage bjp = new BuildJerbilPage(new File(jc.getPagesDir(), "mypage.md"), new File(jc.getWebRootDir(), "mypage.html"), new File(jc.getWebRootDir(), "template.html"));
				
		File r = bjp.resolveRef("footer.html");		
		assert r.toString().endsWith("webroot/footer.html");
		
		File sub = bjp.resolveRef("mysubpage.md");
		assert sub.equals(new File(jc.getPagesDir(), "mysubpage.md")) : sub;
		
		File sub2 = bjp.resolveRef("mysubpage");
		assert sub2.equals(new File(jc.getPagesDir(), "mysubpage.md")) : sub2;
	}

	
	@BeforeClass
	public static void init() {
		JerbilConfig jc = new JerbilConfig();
		jc.projectdir = new File("example");
		Dep.set(JerbilConfig.class, jc);
		
		jc.getPagesDir().mkdirs();
		jc.getWebRootDir().mkdirs();		
	
		Dep.set(Markdown.class, new Markdown());
	}


	@Test
	public void testRun() {
		JerbilConfig jc = Dep.get(JerbilConfig.class);
		
		BuildJerbilPage bjp = new BuildJerbilPage(new File(jc.getPagesDir(), "mypage.md"), new File(jc.getWebRootDir(), "mypage.html"), new File(jc.getWebRootDir(), "template.html"));		
		bjp.run();
		
		WebUtils.display(bjp.getOut());
	}


	@Test
	public void testRenderSection() {
		init();
		JerbilConfig jc = Dep.get(JerbilConfig.class);
		jc.sectionDivs = 2;
		
		File src = new File(jc.getPagesDir(), "sectionWithH2.md");
		File out = new File(jc.getWebRootDir(), "sectionWithH2.html");
		File template = new File(jc.getWebRootDir(), "minimal-template.html");
		BuildJerbilPage bjp = new BuildJerbilPage(src, out, template);		
		bjp.run();
		
		String rendered = FileUtils.read(bjp.getOut());
		
		WebUtils.display(bjp.getOut());
		
		assert StrUtils.toCanonical(rendered).equals(StrUtils.toCanonical( 
				"<html>\n" + 
				"<body>\n" + 
				"<p>Early blah</p>\n" + 
				"<div>\n" + 
				"	<i>Name: Test</i>\n" + 
				"<div class='h2-section my-header-in-a-section'><div class='section-body'>\n" + 
				"<h2><a href=\"#my-header-in-a-section\" id=\"my-header-in-a-section\">My Header In A Section</a></h2>\n" + 
				"<p>Ipsum lorem</p>\n" + 
				"\n" + 
				"</div></div><!-- ./my-header-in-a-section -->\n" + 
				"\n" + 
				"</div>\n" + 
				"<p>Later blah</p>\n" + 
				"\n" + 
				"</body>\n" + 
				"</html>"				
				)) : rendered;
		
	}

}
