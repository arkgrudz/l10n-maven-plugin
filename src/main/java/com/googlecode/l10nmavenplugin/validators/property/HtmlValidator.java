/*******************************************************************************
 * Copyright (c) 2012 Romain Quinio (http://code.google.com/p/l10n-maven-plugin)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package com.googlecode.l10nmavenplugin.validators.property;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.googlecode.l10nmavenplugin.format.Formatter;
import com.googlecode.l10nmavenplugin.format.InnerResourcesFormatter;
import com.googlecode.l10nmavenplugin.log.L10nValidatorLogger;
import com.googlecode.l10nmavenplugin.model.L10nReportItem;
import com.googlecode.l10nmavenplugin.model.L10nReportItem.Severity;
import com.googlecode.l10nmavenplugin.model.L10nReportItem.Type;
import com.googlecode.l10nmavenplugin.model.Property;
import com.googlecode.l10nmavenplugin.model.PropertyImpl;
import com.googlecode.l10nmavenplugin.validators.L10nValidator;
import com.googlecode.l10nmavenplugin.validators.PropertiesKeyConventionValidator;

/**
 * Validator to perform XHTML validation of a property and delegates to {@link SpellCheckValidator} for text nodes.
 * 
 * The XML schema to be used is configurable, with most common schema being pre-defined:
 * <ul>
 * <li>XHTML 1.0-transitional</li>
 * <li>XHTML 1.0-strict</li>
 * <li>XHTML5, based on http://www.xmlmind.com/xhtml5_resources.shtml</li>
 * </ul>
 * 
 * @see {@link com.googlecode.l10nmavenplugin.validators.family.HtmlTagCoherenceValidator}
 * @since 1.0
 * @author romain.quinio
 * 
 */
public class HtmlValidator extends PropertiesKeyConventionValidator implements L10nValidator<Property> {

  /**
   * Template for inserting text resource content before XHTML validation. Need to declare HTML entities that are non
   * default XML ones. Also the text has to be
   * inside a div, as plain text is not allowed directly in body.
   */
  public static final String XHTML_TEMPLATE = "<!DOCTYPE html [ " + "<!ENTITY nbsp \"&#160;\"> "
      + "<!ENTITY copy \"&#169;\"> " + "<!ENTITY cent \"&#162;\"> "
      + "<!ENTITY pound \"&#163;\"> " + "<!ENTITY yen \"&#165;\"> " + "<!ENTITY euro \"&#8364;\"> "
      + "<!ENTITY sect \"&#167;\"> "
      + "<!ENTITY reg \"&#174;\"> " + "<!ENTITY trade \"&#8482;\"> " + "<!ENTITY ndash \"&#8211;\"> " + "]> "
      + "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
      + "<head><title /></head><body><div>{0}</div></body></html>";

  public static final File XHTML5 = new File("xhtml5.xsd");

  public static final File XHTML1_TRANSITIONAL = new File("xhtml1-transitional.xsd");

  public static final File XHTML1_STRICT = new File("xhtml1-strict.xsd");

  private static final File[] PREDEFINED_XSD = new File[] { XHTML5, XHTML1_TRANSITIONAL, XHTML1_STRICT };

  /**
   * Detecting custom data-* attributes
   * 
   * TODO should handle " or ' in attribute value ...
   * 
   * @see <a
   *      href="http://dev.w3.org/html5/spec/global-attributes.html#embedding-custom-non-visible-data-with-the-data-attributes">W3C
   *      HTML5</a>
   */
  private static final String DATA_ATTRIBUTE_REGEX = "data-[-a-z0-9_:\\.]+=(\"[^\"]*\"|'[^\']*')";

  protected static final Pattern DATA_ATTRIBUTE_PATTERN = Pattern.compile(DATA_ATTRIBUTE_REGEX);

  private static final String MULTIPLE_LIS_AT_ROOT_REGEXP = "^(<li>.*</li>\\s*)+$";

  protected static final Pattern MULTIPLE_LIS_AT_ROOT_PATTERN = Pattern.compile(MULTIPLE_LIS_AT_ROOT_REGEXP);

  /**
   * The validator for HTML resources
   */
  private Validator xhtmlValidator;

  private L10nValidator<Property> spellCheckValidator;

  private SAXParser parser;

  private final Formatter formattingParametersExtractor;

  private final InnerResourcesFormatter innerResourceFormatter;

  /**
   * Initialize using default XML schema
   * 
   * @param xhtmlSchema
   * @param logger
   */
  public HtmlValidator(L10nValidatorLogger logger, L10nValidator<Property> spellCheckValidator, String[] htmlKeys,
      Formatter formattingParametersExtractor, InnerResourcesFormatter innerResourceFormatter) {
    this(XHTML1_TRANSITIONAL, logger, spellCheckValidator, htmlKeys, formattingParametersExtractor,
        innerResourceFormatter);
  }

  /**
   * Initialize using XML schema
   * 
   * @param xhtmlSchema
   * @param logger
   */
  public HtmlValidator(File xhtmlSchema, L10nValidatorLogger logger, L10nValidator<Property> spellCheckValidator,
      String[] htmlKeys, Formatter formattingParametersExtractor, InnerResourcesFormatter innerResourceFormatter) {
    super(logger, htmlKeys);
    this.spellCheckValidator = spellCheckValidator;
    this.formattingParametersExtractor = formattingParametersExtractor;
    this.innerResourceFormatter = innerResourceFormatter;

    try {
      // SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      // Need to use XERCES so that XHTML5 schema passes validation
      SchemaFactory factory = new XMLSchemaFactory();
      factory.setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_FULL_CHECKING, false);

      Schema schema = null;
      if (xhtmlSchema.exists()) {
        // Load custom schema
        schema = factory.newSchema(xhtmlSchema);
      }
      else {
        // Try to load a pre-defined schemas from classpath
        URL schemaURL = this.getClass().getClassLoader().getResource(xhtmlSchema.getName());

        if (schemaURL == null) {
          logger.getLogger().error(
              "Could not load XML schema from file <" + xhtmlSchema.getAbsolutePath() + "> and <" +
                  xhtmlSchema.getName()
                  + "> is not a default schema either (" + Arrays.toString(PREDEFINED_XSD) + "), thus defaulting to " +
                  XHTML1_TRANSITIONAL.getName());
          schemaURL = this.getClass().getClassLoader().getResource(XHTML1_TRANSITIONAL.getName());
        }
        schema = factory.newSchema(schemaURL);
      }
      xhtmlValidator = schema.newValidator();

      // Initialize SAX parser
      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      parser = saxParserFactory.newSAXParser();

    }
    catch (SAXException e) {
      logger.getLogger().error("Could not initialize HtmlValidator", e);

    }
    catch (ParserConfigurationException e) {
      logger.getLogger().error("Could not initialize HtmlValidator", e);
    }
  }

  /**
   * Validate HTML text using XHTML validator.
   * 
   * <ul>
   * <li>Performs a MessageFormat if resource is parametric.</li>
   * <li>Removes HTML5 data-* attributes, as this is a limitation to express these using W3C XML schema.</li>
   * <li>Then wraps the resource into an XHTML document body and validate with JAXP.</li>
   * </ul>
   * 
   * @param key
   * @param message
   * @param propertyName
   * @return Number of errors
   */
  public int validate(Property property, List<L10nReportItem> reportItems) {
    int nbErrors = 0;
    if (xhtmlValidator != null) {
      String formattedMessage = property.getMessage();
      ReportingErrorHandler handler = new ReportingErrorHandler(property, formattedMessage, reportItems, logger);
      try {
        formattedMessage = applyWorkArroundForMultipleLIsIfNecessary(formattedMessage);

        if (formattingParametersExtractor.isParametric(formattedMessage)) {
          formattedMessage = formattingParametersExtractor.defaultFormat(formattedMessage);
        }
        else { // In any case replace '' by ' (resource without parameters but called with fmt:param)
          formattedMessage = formattedMessage.replaceAll("''", "'");
        }
        if (innerResourceFormatter != null && innerResourceFormatter.hasInnerResources(formattedMessage)) {
          formattedMessage = innerResourceFormatter.defaultFormat(formattedMessage);
        }

        // HACK Remove custom data-* attributes, as thay can't easily be validated by a schema.
        formattedMessage = formattedMessage.replaceAll(DATA_ATTRIBUTE_REGEX, "");
        String xhtml = MessageFormat.format(XHTML_TEMPLATE, formattedMessage);
        Source source = new StreamSource(new ByteArrayInputStream(xhtml.getBytes("UTF-8")));

        xhtmlValidator.setErrorHandler(handler);
        xhtmlValidator.validate(source);

        // If XHTML validation was successful, validate spellcheck
        if (spellCheckValidator != null) {
          SpellCheckValidationHandler saxHandler = new SpellCheckValidationHandler(property, reportItems);
          try {
            parser.parse(new InputSource(new StringReader(xhtml)), saxHandler);
            nbErrors += saxHandler.getNbErrors();

          }
          catch (SAXException e) {
            logger.getLogger().error("SAXException while parsing [" + formattedMessage + "]", e);
          }
          catch (IOException e) {
            logger.getLogger().error(e);
          }
        }

      }
      catch (IllegalArgumentException e) {
        // Catch MessageFormat errors in case of malformed message
        handler.report(e, Type.MALFORMED_PARAMETER, "Formatting error: ", property, formattedMessage, reportItems);
      }
      catch (SAXException e) {
        handler.report(e, Type.HTML_VALIDATION, "XHTML validation fatal error: ", property, formattedMessage,
            reportItems);
      }
      catch (IOException e) {
        handler.report(e, Type.HTML_VALIDATION, "XHTML validation fatal error: ", property, formattedMessage,
            reportItems);
      }
      finally {
        nbErrors += handler.getNbErrors();
      }
    }
    return nbErrors;
  }

  private String applyWorkArroundForMultipleLIsIfNecessary(String formattedMessage) {
    String result = formattedMessage;
    String trimmed = StringUtils.trim(formattedMessage);
    if (MULTIPLE_LIS_AT_ROOT_PATTERN.matcher(trimmed).matches()) {
      result = "<ul>" + formattedMessage + "</ul>";
    }
    return result;
  }

  public void setSpellCheckValidator(L10nValidator<Property> spellCheckValidator) {
    this.spellCheckValidator = spellCheckValidator;
  }

  /**
   * SAX parser to extract text inside XHTML elements.
   * 
   */
  private class SpellCheckValidationHandler extends DefaultHandler {

    private final Property property;

    private final List<L10nReportItem> reportItems;

    private int nbErrors = 0;

    public SpellCheckValidationHandler(Property property, List<L10nReportItem> reportItems) {
      this.property = property;
      this.reportItems = reportItems;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      String text = new String(ch, start, length);
      Property htmlTextProperty = new PropertyImpl(property.getKey(), text, property.getPropertiesFile());
      // Delegate to spellCheck validator
      nbErrors += spellCheckValidator.validate(htmlTextProperty, reportItems);
    }

    public int getNbErrors() {
      return nbErrors;
    }
  }

  /**
   * Handler of all XHTML validation errors
   * 
   */
  private static class ReportingErrorHandler implements ErrorHandler {

    private static final int NB_ERROR_MAX_CHAR = 140;

    private final List<L10nReportItem> reportItems;

    private int nbErrors = 0;

    private final Property property;

    private final L10nValidatorLogger logger;

    private final String formattedMessage;

    public ReportingErrorHandler(Property property, String formattedMessage, List<L10nReportItem> reportItems,
        L10nValidatorLogger logger) {
      this.reportItems = reportItems;
      this.formattedMessage = formattedMessage;
      this.logger = logger;
      this.property = property;
    }

    public void warning(SAXParseException e) {
      report(e, Type.HTML_VALIDATION, "XHTML validation warning: ", property, formattedMessage, reportItems);
    }

    public void error(SAXParseException e) {
      report(e, Type.HTML_VALIDATION, "XHTML validation error: ", property, formattedMessage, reportItems);
    }

    public void fatalError(SAXParseException e) throws SAXParseException {
      // Stop validation
      throw e;
    }

    /**
     * Common handling of validation exceptions
     * 
     * @param e
     * @param type
     * @param property
     * @param formattedMessage
     * @param reportItems
     */
    public void report(Exception e, Type type, String errorText, Property property, String formattedMessage,
        List<L10nReportItem> reportItems) {
      if (Severity.ERROR.equals(type.getSeverity())) {
        nbErrors++;
      }
      L10nReportItem reportItem = new L10nReportItem(type, errorText +
          StringUtils.abbreviate(e.getMessage(), NB_ERROR_MAX_CHAR), property, formattedMessage);
      reportItems.add(reportItem);
      logger.log(reportItem);
    }

    public int getNbErrors() {
      return nbErrors;
    }

  }

  public boolean shouldValidate(Property property) {
    return matches(property.getKey());
  }

}
