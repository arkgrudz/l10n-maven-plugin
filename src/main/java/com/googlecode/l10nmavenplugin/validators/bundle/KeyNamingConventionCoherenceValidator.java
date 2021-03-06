/*******************************************************************************
 * Copyright (c) 2012 Romain Quinio (http://code.google.com/p/l10n-maven-plugin)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package com.googlecode.l10nmavenplugin.validators.bundle;

import java.util.List;

import com.googlecode.l10nmavenplugin.log.L10nValidatorLogger;
import com.googlecode.l10nmavenplugin.model.L10nReportItem;
import com.googlecode.l10nmavenplugin.model.PropertiesFamily;
import com.googlecode.l10nmavenplugin.validators.AbstractL10nValidator;
import com.googlecode.l10nmavenplugin.validators.L10nValidator;

/**
 * Detects incoherence of properties key naming
 * 
 * TODO
 * 
 * Ex: WARN if a resource type/view is defined only once, context that differs only with lowercase/uppercase, ...
 * 
 * Could also detect if key patterns passed in plugin configuration would not be mutually exclusive (html key should not match url key !)
 * 
 * @since ??
 * @author romain.quinio
 */
public class KeyNamingConventionCoherenceValidator extends AbstractL10nValidator implements L10nValidator<PropertiesFamily> {

  public KeyNamingConventionCoherenceValidator(L10nValidatorLogger logger) {
    super(logger);
  }

  public int validate(PropertiesFamily propertiesFamily, List<L10nReportItem> reportItems) {
    return 0;
  }

  public boolean shouldValidate(PropertiesFamily propertiesFamily) {
    // Always validate
    return true;
  }

}
