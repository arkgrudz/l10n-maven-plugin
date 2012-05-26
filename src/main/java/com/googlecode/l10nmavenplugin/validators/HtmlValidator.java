/*******************************************************************************
 * Copyright (c) 2012 Romain Quinio (http://code.google.com/p/l10n-maven-plugin)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package com.googlecode.l10nmavenplugin.validators;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.xml.sax.SAXException;

import com.googlecode.l10nmavenplugin.log.L10nValidatorLogger;
import com.googlecode.l10nmavenplugin.validators.L10nReportItem.Severity;
import com.googlecode.l10nmavenplugin.validators.L10nReportItem.Type;

/**
 * Performs XHTML validation of a property.
 * 
 * @author romain.quinio
 */
public class HtmlValidator implements L10nValidator {

  /**
   * Template for inserting text resource content before XHTML validation. Need to declare HTML entities that are non default XML
   * ones. Also the text has to be inside a div, as plain text is not allowed directly in body.
   */
  private static final String XHTML_TEMPLATE = "<!DOCTYPE html [ " + "<!ENTITY nbsp \"&#160;\"> " + "<!ENTITY copy \"&#169;\"> "
      + "<!ENTITY cent \"&#162;\"> " + "<!ENTITY pound \"&#163;\"> " + "<!ENTITY yen \"&#165;\"> "
      + "<!ENTITY euro \"&#8364;\"> " + "<!ENTITY sect \"&#167;\"> " + "<!ENTITY reg \"&#174;\"> "
      + "<!ENTITY trade \"&#8482;\"> " + "<!ENTITY ndash \"&#8211;\"> " + "]> " + "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
      + "<head><title /></head><body><div>{0}</div></body></html>";

  private static final String[] DEFAULT_XHTML_XSD = new String[] { "xhtml1-transitional.xsd", "xhtml1-strict.xsd", "xhtml5.xsd" };

  /**
   * The validator for HTML resources
   */
  private Validator xhtmlValidator;

  private L10nValidatorLogger logger;

  /**
   * Initialize using default XML schema
   * 
   * @param xhtmlSchema
   * @param logger
   */
  public HtmlValidator(L10nValidatorLogger logger) {
    this(new File(DEFAULT_XHTML_XSD[0]), logger);
  }

  /**
   * Initialize using XML schema
   * 
   * @param xhtmlSchema
   * @param logger
   */
  public HtmlValidator(File xhtmlSchema, L10nValidatorLogger logger) {
    this.logger = logger;

    try {
      // SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      // Need to use XERCES so that XHTML5 schema passes validation
      SchemaFactory factory = new XMLSchemaFactory();
      factory.setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_FULL_CHECKING, false);

      Schema schema = null;
      if (xhtmlSchema.exists()) {
        // Load custom schema
        schema = factory.newSchema(xhtmlSchema);
      } else {
        // Try to load a pre-defined schemas from classpath
        URL schemaURL = this.getClass().getClassLoader().getResource(xhtmlSchema.getName());

        if (schemaURL == null) {
          logger.getLogger().error(
              "Could not load XML schema from file <" + xhtmlSchema.getAbsolutePath() + "> and <" + xhtmlSchema.getName()
                  + "> is not a default schema either (" + Arrays.toString(DEFAULT_XHTML_XSD) + "), thus defaulting to "
                  + DEFAULT_XHTML_XSD[0]);
          schemaURL = this.getClass().getClassLoader().getResource(DEFAULT_XHTML_XSD[0]);
        }
        schema = factory.newSchema(schemaURL);
        xhtmlValidator = schema.newValidator();
      }

    } catch (SAXException e) {
      logger.getLogger().error("Could not iniialize HtmlValidator", e);
    }
  }

  /**
   * Validate HTML text using XHTML validator.
   * 
   * Performs a MessageFormat if needed.
   * 
   * @param key
   * @param message
   * @param propertyName
   * @return Number of errors
   */
  public int validate(String key, String message, String propertiesName, List<L10nReportItem> reportItems) {
    int nbErrors = 0;
    if (xhtmlValidator != null) {
      String formattedMessage = message;
      try {
        if (ParametricMessageValidator.isParametric(key, formattedMessage, propertiesName)) {
          formattedMessage = ParametricMessageValidator.defaultFormat(formattedMessage);
        } else { // In any case replace '' by ' (resource without parameters but called with fmt:param)
          formattedMessage = formattedMessage.replaceAll("''", "'");
        }
        String xhtml = MessageFormat.format(XHTML_TEMPLATE, formattedMessage);
        Source source = new StreamSource(new ByteArrayInputStream(xhtml.getBytes("UTF-8")));
        xhtmlValidator.validate(source);

      } catch (IllegalArgumentException e) {
        // Catch MessageFormat errors in case of malformed message
        nbErrors++;
        handleException(e, Type.MALFORMED_PARAMETER, propertiesName, key, message, formattedMessage, reportItems);
      } catch (SAXException e) {
        nbErrors++;
        handleException(e, Type.HTML_VALIDATION, propertiesName, key, message, formattedMessage, reportItems);
      } catch (IOException e) {
        nbErrors++;
        handleException(e, Type.HTML_VALIDATION, propertiesName, key, message, formattedMessage, reportItems);
      }
    }
    return nbErrors;
  }

  private void handleException(Exception e, Type type, String propertiesName, String key, String message,
      String formattedMessage, List<L10nReportItem> reportItems) {
    L10nReportItem reportItem = new L10nReportItem(Severity.ERROR, type, "XHTML validation error: "
        + StringUtils.abbreviate(e.getMessage(), 140), propertiesName, key, message, formattedMessage);
    reportItems.add(reportItem);
    logger.log(reportItem);
  }

  public int report(Set<String> propertiesNames, List<L10nReportItem> reportItems) {
    throw new UnsupportedOperationException();
  }

}