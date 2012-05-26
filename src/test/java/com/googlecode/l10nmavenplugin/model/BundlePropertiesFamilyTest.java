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

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

public class BundlePropertiesFamilyTest {

  private Collection<PropertiesFile> propertiesFiles;

  @Before
  public void setUp() {
    propertiesFiles = new ArrayList<PropertiesFile>();
  }

  @Test
  public void testStandardBundle() {
    propertiesFiles.add(new BundlePropertiesFile("bundle.properties", null));
    propertiesFiles.add(new BundlePropertiesFile("bundle_EN_GB.properties", null));
    propertiesFiles.add(new BundlePropertiesFile("bundle_FR.properties", null));
    PropertiesFamily propertiesFamily = new BundlePropertiesFamily(propertiesFiles);

    assertEquals("bundle", propertiesFamily.getBaseName());
    assertNotNull(propertiesFamily.getRootPropertiesFile());
    assertEquals("bundle.properties", propertiesFamily.getRootPropertiesFile().getFileName());
  }

  @Test
  public void testNoRootBundle() {
    propertiesFiles.add(new BundlePropertiesFile("bundle_EN_GB.properties", null));
    propertiesFiles.add(new BundlePropertiesFile("bundle_FR.properties", null));
    PropertiesFamily propertiesFamily = new BundlePropertiesFamily(propertiesFiles);

    assertEquals("bundle", propertiesFamily.getBaseName());
    assertNull(propertiesFamily.getRootPropertiesFile());
  }
}
