package com.goodloop.jerbil;

import static org.junit.Assert.*;

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
	public void testRenderWithout() {
		Markdown md = new Markdown();
		{
			String mdt = "Hello World";
			String html = md.render(mdt);
			String htmlBit = md.renderWithoutWrapper(mdt);
//			System.out.println(html);
//			System.out.println(htmlBit);
			assert htmlBit.equals("Hello World");
		}
	}

}
