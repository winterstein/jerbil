package com.goodloop.jerbil;

import org.junit.BeforeClass;
import org.junit.Test;

import com.winterwell.utils.Dep;
import com.winterwell.utils.web.WebUtils;

public class MarkdownTest {

	@BeforeClass
	public static void init() {
		if ( ! Dep.has(JerbilConfig.class)) {
			Dep.set(JerbilConfig.class, new JerbilConfig());
		}
	}
	
	@Test
	public void testRenderWithout_keepUl() {
		Markdown md = new Markdown();
		String string = "Developer work includes:\n"
				+ "\n"
				+ "- Software development.\n"
				+ "- Testing.\n"
				+ "- Input into software and product design.\n"
				+ "- Work on technical operations and infrastructure.\n"
				+ "- Reporting on KPIs and other relevant metrics.\n"
				+ "- Providing technical support as appropriate.";
		String htmlFragment = md.renderWithoutWrapper(string);
		assert htmlFragment.contains("<ul>") : htmlFragment;
		assert htmlFragment.contains("</ul>") : htmlFragment;
	}


	@Test
	public void testRender_inlineCode() {
		Markdown md = new Markdown();
		String string = "Hello `code world` :)";
		String htmlFragment = md.renderWithoutWrapper(string);
		assert htmlFragment.contains("<code>") : htmlFragment;
		assert htmlFragment.equals("Hello <code>code world</code> :)") : htmlFragment;
	}


	@Test
	public void testRender_codeBlock() {
		Markdown md = new Markdown();
		String string = "Hello\n\n```\ncode\nworld\n```\n";
		String htmlFragment = md.renderWithoutWrapper(string);
		assert htmlFragment.contains("<code>") : htmlFragment;
		assert htmlFragment.contains("<code>code\nworld\n</code>") : htmlFragment;
	}
	
	
	@Test
	public void testSectionDivs_emptySectionClose() {			
		Markdown md = new Markdown();
		md.sectionDivs = 2;
		String s = md.render("\n## Foo\n\nbar\n\n/##\n\n## Benefits\n\n/##\n\nFoo\n");
		System.out.println(s);
	}
	
	@Test
	public void testRenderCols() {
		Markdown md = new Markdown();
		{	// simple
			String mdt = "\n<div class='row'>\n<div class='col'>\nC1\n</div>\n<div class='col'>\nC2\n</div>\n</div>\n";
			String html = md.render(mdt);
			System.out.println(html);
		}
		{	// paragraphs
			String mdt = "\n<div class='row'>\n<div class='col'>\n\nC1 p1\n\nC1 p2\n</div>\n<div class='col'>\nC2\n</div>\n</div>\n";
			String html = md.render(mdt);
			System.out.println(html);
			WebUtils.display(html);
		}
		{	// headers
			String mdt = "\n<div class='row'>\n<div class='col'>\n\nC1 p1\n\nC1 p2\n</div>\n<div class='col'>\nC2\n</div>\n</div>\n";
			String html = md.render(mdt);
			System.out.println(html);
			WebUtils.display(html);
		}
	}
	
	
	@Test
	public void testHeaderEndMarker() {
		Markdown md = new Markdown();
		md.sectionDivs = 2;
		{	// h2
			String mdt = "\n## Hello\n\nsome text\n\n/##\n\nfoo\n";
			String html = md.render(mdt);
			System.out.println(html);
			assert ! html.contains("/##");
		}
	}
	
	@Test
	public void testRenderWithout() {
		Markdown md = new Markdown();
		{
			String mdt = "Hello World";
			String html = md.render(mdt);
			String htmlBit = md.renderWithoutWrapper(mdt);
//			System.out.println(html);
//			System.out.println(htmlBit);
			assert htmlBit.equals("Hello World") : htmlBit;
		}
	}

}
