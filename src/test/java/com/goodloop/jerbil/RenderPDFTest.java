package com.goodloop.jerbil;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.web.WebPage;

public class RenderPDFTest {

	@Test
	public void testRun() throws IOException {
		File in = File.createTempFile("testRenderPDF", ".html");
		WebPage html = new WebPage();
		html.setTitle("Title is Foo");
		html.append("<h3>Hello World :)</h3>");
		FileUtils.write(in, html.toString());
		File out = File.createTempFile("testRenderPDF", ".pdf");
		RenderPDF r = new RenderPDF(in, out);
		r.run();
		assert out.isFile() && out.length() > 100;
//		WebUtils2.browseOnDesktop(out.toURI().toString()); uncomment to see
	}

	

	@Test
	public void testRunBBC() throws IOException {
		File out = File.createTempFile("testRenderPDF", ".pdf");
		RenderPDF r = new RenderPDF("https://bbc.co.uk", out);
		r.run();
		assert out.isFile() && out.length() > 100;
//		WebUtils2.browseOnDesktop(out.toURI().toString()); // uncomment to see
	}

}
