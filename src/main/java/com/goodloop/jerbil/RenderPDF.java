package com.goodloop.jerbil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.PdfOptions;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.Margin;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Mutable;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;

/**
 * Playwright API docs -- see e.g. 
 * 
 * https://playwright.dev/docs/api/class-page
 * https://playwright.dev/docs/network
 * 
 * @testedby {@link RenderPDFTest}
 * @author daniel
 *
 */
public class RenderPDF {

	private static final String LOGTAG = null;
	private File html;
	private File pdf;
	private String url;

	public RenderPDF(File html, File pdf) {
		this.html = html;
		this.pdf = pdf;
	}


	/**
	 * Does this work??!!
	 * @param url
	 * @param pdf
	 */
	public RenderPDF(String url, File pdf) {
		this.url = url;
		this.pdf = pdf;
	}

	
	public void setOptions(String makePdfOptions) {
		// TODO Auto-generated method stub

	}

	public void run() {
		boolean done = false;
		Exception err = null;
		try (Playwright playwright = Playwright.create()) {
//			Tracing tracing; Did not record anything useful :(
			// try a few browsers
			List<BrowserType> browserTypes = Arrays.asList(playwright.chromium(), playwright.webkit(), playwright.firefox());
			for (BrowserType browserType : browserTypes) {
				try (Browser browser = browserType.launch()) {
					BrowserContext context = browser.newContext();
//					tracing = context.tracing();
//					StartOptions startOpt = new StartOptions();
//					startOpt.screenshots = true;
//					startOpt.sources = true;
//					tracing.start(startOpt);
					done = run2_doIt(browser);
					if (done) { 
//						StopOptions so = new StopOptions();
//						File ft = File.createTempFile("temp", ".trace.zip");
//						Printer.out(ft.getCanonicalPath());
//						so.setPath(ft.toPath());
//						tracing.stop(so);
						break; // done - stop
					}
//		          page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshot-" + browserType.name() + ".png")));
				} catch (Exception e) {
					Log.e(e); // go onto the next browser
					err = e;
				}
			}
		}
		if (done) return;
		if (err != null) throw Utils.runtime(err);
		throw new FailureException("pdf not made?! "+html);		
	}

	private boolean run2_doIt(Browser browser) throws FileNotFoundException, IOException {
		BrowserContext context = browser.newContext();
		Page page = context.newPage();
		if (url==null) {
			url = WebUtils.getFileUrl(html);
		}
		// Looks like this works!!
		Mutable.Dble total = new Mutable.Dble();
		Consumer<Response> cresp = response -> {
			try {
//				Printer.out(response.url());
				int bl = response.body().length;
//				Printer.out(bl);
				total.value += bl;
			} catch(Throwable ex) {
				Log.d(LOGTAG, response.url()+" -> "+ex.toString());
			}
		};
//		Consumer<Request> creq = req -> {
//			Printer.out(req.url()+" ... ");
//		};
//		page.onRequest(creq);
		page.onResponse(cresp);
		
		page.navigate(url);
		page.waitForLoadState();

		Log.d(LOGTAG, "Total data fetched: "+total);
		
		PdfOptions pdfOptions = new PdfOptions();
		Margin margin = new Margin();
		margin.left = "1cm";
		margin.top = "1cm";
		margin.bottom = "1cm";
		margin.right = "1cm";
		pdfOptions.setMargin(margin);
		byte[] bytes = page.pdf(pdfOptions);
		FileOutputStream fouts = new FileOutputStream(pdf);
		fouts.write(bytes);
		fouts.close();
		return true;
	}

}
