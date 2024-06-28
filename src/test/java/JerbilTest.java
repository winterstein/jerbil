import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.goodloop.jerbil.JerbilMain;
import com.winterwell.utils.io.FileUtils;


public class JerbilTest {

	@Test
	public void testMain() throws IOException {
		File sogive = new File(FileUtils.getWinterwellDir(), "SoGive");
		assert sogive.isDirectory() : sogive;
		JerbilMain.main(new String[]{sogive.getAbsolutePath()});
	}

}
