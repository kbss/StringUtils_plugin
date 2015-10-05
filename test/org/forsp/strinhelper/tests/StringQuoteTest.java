package org.forsp.strinhelper.tests;

import org.forsp.strinhelper.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StringQuoteTest {

	private StringUtils searcher;

	@Before
	public void init() {
		searcher = new StringUtils();
	}

	@Test
	public void test() {
		searcher = new StringUtils();
		String text1 = "\"text1 \\\"T\\\"\"";
		String text2 = "\"te\\\"A\\\"\"";
		String text3 = "\"other text\"";
		String text4 = "\"\"";

		String test = String.format("%s,%s, %s,%s", text1, text2, text3, text4);
		System.out.println(test);
		System.out.println(test.length());
		assertValid(searcher.getStringBound(test, 1), test, text1);
		assertValid(searcher.getStringBound(test, 11), test, text1);
		assertValid(searcher.getStringBound(test, 15), test, text2);
		assertValid(searcher.getStringBound(test, 27), test, text3);
		assertValid(searcher.getStringBound(test, 39), test, text4);
		Assert.assertNull(searcher.getStringBound(test, 13));
		Assert.assertNull(searcher.getStringBound(test, -1));
		Assert.assertNull(searcher.getStringBound(null, 1));
		Assert.assertNull(searcher.getStringBound(" ", 2));

		String text5 = "\"^(\\+|\\\")\"";
		String text7 = "\"\\\"(\\s+|)\\+(\\s+|)\\\\\"";
		assertValid(searcher.getStringBound(text5, 5), text5);
		assertValid(searcher.getStringBound(text7, 15), text7);
	}

	@Test
	public void allPositionTest() {
		String text = "(\\s+|)\\+(\\s+|)";
		String testText = String.format("%s%s%s", '"', text, '"');
		for (int i = 1; i < text.length(); i++) {
			assertValid(searcher.getStringBound(testText, i), testText, testText);
		}
	}

	private void assertValid(int[] bound, String expectedText) {
		assertValid(bound, expectedText, expectedText);
	}

	private void assertValid(int[] bound, String testText, String expectedText) {
		Assert.assertNotNull(bound);
		String actualText = testText.substring(bound[0], bound[1]);
		Assert.assertNotNull(actualText);
		Assert.assertFalse("Should not be empty", "".equals(actualText));
		Assert.assertEquals(expectedText, actualText);
	}
}
