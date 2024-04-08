package com.goodloop.jerbil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.ScreenshotOptions;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Mutable;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.web.FakeBrowser;

/**
 * Playwright API docs -- see e.g. 
 * 
 * https://playwright.dev/docs/api/class-page
 * https://playwright.dev/docs/network
 * 
 * @author daniel
 *
 */
public class RenderPng {

	private static final String LOGTAG = null;
	private File html;
	private File png;
	private String url;
	private int w = 1200;
	private int h = 900;
	
	public void setWidth(int w) {
		this.w = w;
	}
	public void setHeight(int h) {
		this.h = h;
	}

	public RenderPng(File html, File png) {
		this.html = html;
		this.png = png;
	}


	/**
	 * Does this work??!!
	 * @param url
	 * @param pdf
	 */
	public RenderPng(String url, File pdf) {
		this.url = url;
		this.png = pdf;
	}

	
	public void setOptions(String makePdfOptions) {
		// TODO Auto-generated method stub

	}


	private BrowserContext call2_browserContext(Browser browser) {
//		if (mobile) {
//			// see https://github.com/microsoft/playwright/blob/main/packages/playwright-core/src/server/deviceDescriptorsSource.json
//			// iPhone11 -- best selling phone of 2019
//			NewContextOptions nco = new Browser.NewContextOptions()
//					.setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 12_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.4 Mobile/15E148 Safari/604.1")
//					.setScreenSize(414,896)
//					.setViewportSize(414,715)
//				    .setDeviceScaleFactor(2) // why??
//				    .setIsMobile(true)
//				    .setHasTouch(true);
////				    "defaultBrowserType": "webkit"
//			return browser.newContext(nco);
//		}
		NewContextOptions nco = new Browser.NewContextOptions()
				.setUserAgent(FakeBrowser.DEFAULT_USER_AGENT)
				.setScreenSize(w,h)
				.setViewportSize(w,h)
//			    .setDeviceScaleFactor(2)
			    .setIsMobile(false);
		return browser.newContext(nco); // vanilla		
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
					try(BrowserContext context = call2_browserContext(browser)) {
					
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

		Path screenshotPath = png.toPath().toAbsolutePath();					
		ScreenshotOptions so = new Page.ScreenshotOptions();
		so.setPath(screenshotPath);
//		ScreenshotScale s = ScreenshotScale.;
//		so.setScale(s);
//		if (clipToAd) {
//			ElementInfo ei = getAdSizeEstimate(format);
//			if (ei.width() == 0 || ei.height() == 0) {
//				Log.e(LOGTAG, "(skip clip) zero size! "+ei+" "+url);
//			} else {
//				so.setClip(ei.x(), ei.y(), ei.width(), ei.height());
		so.setClip(0,0,w,h);
		so.setFullPage(true);
//			}
//		}
		page.screenshot(so);		
//		PdfOptions pdfOptions = new PdfOptions();
//		Margin margin = new Margin();
//		margin.left = "1cm";
//		margin.top = "1cm";
//		margin.bottom = "1cm";
//		margin.right = "1cm";
//		pdfOptions.setMargin(margin);
//		byte[] bytes = page.pdf(pdfOptions);
//		FileOutputStream fouts = new FileOutputStream(png);
//		fouts.write(bytes);
//		fouts.close();
		return true;
	}

}
