/*******************************************************************************
 * Copyright (c) 2012 Romain Quinio (http://code.google.com/p/l10n-maven-plugin)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package com.googlecode.l10nmavenplugin.model;

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Test;

public class BundlePropertiesFileTest {

  @Test
  public void testBundleName() {
    PropertiesFile file = new BundlePropertiesFile("Bundle_en.properties", null);
    assertEquals("Bundle_en.properties", file.getFileName());
    assertEquals("Bundle", file.getBundleName());
  }

  @Test
  public void testLanguageLocale() {
    PropertiesFile file = new BundlePropertiesFile("Bundle_en.properties", null);
    assertEquals(Locale.ENGLISH, file.getLocale());
  }

  @Test
  public void testLanguageAndCountryLocale() {
    PropertiesFile file = new BundlePropertiesFile("Bundle_zh_CN.properties", null);
    assertEquals(Locale.SIMPLIFIED_CHINESE, file.getLocale());
  }

  @Test
  public void testLanguageAndCountryAndVariantLocale() {
    PropertiesFile file = new BundlePropertiesFile("Bundle_zh_CN_var.properties", null);
    assertEquals(new Locale("zh", "CN", "var"), file.getLocale());
  }

  @Test
  public void rootPropertyShouldHaveNoLocale() {
    PropertiesFile file = new BundlePropertiesFile("Bundle.properties", null);
    assertEquals("Bundle", file.getBundleName());
    assertNull(file.getLocale());
  }

}
