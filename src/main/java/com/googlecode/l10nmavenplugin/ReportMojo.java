/*******************************************************************************
 * Copyright (c) 2012 Romain Quinio (http://code.google.com/p/l10n-maven-plugin)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package com.googlecode.l10nmavenplugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

import com.googlecode.l10nmavenplugin.report.L10nReportRenderer;
import com.googlecode.l10nmavenplugin.validators.L10nReportItem;

/**
 * Creates a report on l10n Properties files validation result.
 * 
 * Relies on {@link ValidateMojo} to build list of validation items, before rendering them via {@link L10nReportRenderer}. It uses
 * a subset of {@link ValidateMojo} configuration, as ignoreFailures and excludedKey are not applicable for a report.
 * 
 * @goal report
 * @phase site
 * @author romain.quinio
 */
public class ReportMojo extends AbstractMavenReport implements L10nValidationConfiguration {

  /**
   * Directory containing properties file to check
   * 
   * @parameter default-value="src\\main\\resources"
   * @since 1.2
   */
  private File propertyDir;

  /**
   * List of keys to match as text resources used from js. Default is ".js.".
   * 
   * @parameter
   * @since 1.2
   */
  private String[] jsKeys = new String[] { ".js." };

  /**
   * Declares how the client side resources are loaded in javascript.
   * 
   * @see {@link ValidateMojo}
   * 
   * @parameter default-value="true"
   * @since 1.3
   */
  private boolean jsDoubleQuoted = true;

  /**
   * List of keys to match as url resources. Default is ".url.".
   * 
   * @parameter
   * @since 1.2
   */
  private String[] urlKeys = new String[] { ".url." };

  /**
   * List of keys to match as html text resources. Default is ".text.".
   * 
   * @parameter
   * @since 1.2
   */
  private String[] htmlKeys = new String[] { ".text." };

  /**
   * XML Schema to use for html resource validation. Default value is to use XHTML1 transitional.
   * 
   * @parameter default-value="xhtml1-transitional.xsd"
   * @since 1.3
   */
  private File xhtmlSchema;

  /**
   * List of keys to match as non-html text resources. Default is ".title.".
   * 
   * @parameter
   * @since 1.2
   */
  private String[] textKeys = new String[] { ".title." };

  /**
   * Custom validation patterns.
   * 
   * @parameter
   * @since 1.3
   */
  private CustomPattern[] customPatterns = new CustomPattern[] {};

  /**
   * Maven site renderer, not used by this Mojo.
   */
  private Renderer siteRenderer;

  /**
   * The {@link org.apache.maven.reporting.MavenReportRenderer} to generate report
   */
  private L10nReportRenderer reportRenderer;

  /**
   * @parameter default-value="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * Directory where reports will go.
   * 
   * @parameter expression="${project.reporting.outputDirectory}"
   * @required
   * @readonly
   */
  private String outputDirectory;

  /**
   * Entry point for the plugin report goal
   * 
   * @param locale
   */
  @Override
  protected void executeReport(Locale locale) throws MavenReportException {
    List<L10nReportItem> reportItems = new ArrayList<L10nReportItem>();

    ValidateMojo validateMojo = new ValidateMojo(this);
    validateMojo.setLog(getLog());
    validateMojo.initialize();
    int nbErrors = 0;

    try {
      nbErrors = validateMojo.validateProperties(propertyDir, reportItems);

    } catch (MojoExecutionException e) {
      throw new MavenReportException("Could not exceute ValidateMojo", e);
    }

    reportRenderer = new L10nReportRenderer(getSink(), getBundle(locale));
    reportRenderer.setReportItems(reportItems);
    reportRenderer.setNbErrors(nbErrors);
    reportRenderer.render();
  }

  /**
   * Report description
   */
  public String getDescription(Locale locale) {
    return getBundle(locale).getString("report.dashboard.title.description");
  }

  /**
   * Report title
   */
  public String getName(Locale locale) {
    return getBundle(locale).getString("report.dashboard.title.name");
  }

  /**
   * Report file name
   */
  public String getOutputName() {
    return "l10n-report";
  }

  @Override
  protected String getOutputDirectory() {
    return outputDirectory;
  }

  @Override
  protected MavenProject getProject() {
    return project;
  }

  @Override
  protected Renderer getSiteRenderer() {
    return siteRenderer;
  }

  private ResourceBundle getBundle(Locale locale) {
    return ResourceBundle.getBundle("l10n-report", locale, this.getClass().getClassLoader());
  }

  public File getPropertyDir() {
    return propertyDir;
  }

  public void setPropertyDir(File propertyDir) {
    this.propertyDir = propertyDir;
  }

  public void setExcludedKeys(String[] excludedKeys) {
    // Ignored for reporting
  }

  public void setIgnoreFailure(boolean ignoreFailure) {
    // Ignored for reporting
  }

  public void setJsKeys(String[] jsKeys) {
    this.jsKeys = jsKeys;
  }

  public void setUrlKeys(String[] urlKeys) {
    this.urlKeys = urlKeys;
  }

  public void setHtmlKeys(String[] htmlKeys) {
    this.htmlKeys = htmlKeys;
  }

  public void setTextKeys(String[] textKeys) {
    this.textKeys = textKeys;
  }

  public void setCustomPatterns(CustomPattern[] customPatterns) {
    this.customPatterns = customPatterns;
  }

  public void setJsDoubleQuoted(boolean jsDoubleQuoted) {
    this.jsDoubleQuoted = jsDoubleQuoted;
  }

  public void setXhtmlSchema(File xhtmlSchema) {
    this.xhtmlSchema = xhtmlSchema;
  }

  public String[] getExcludedKeys() {
    // Exclusions should not be used in reporting
    return new String[] {};
  }

  public boolean getIgnoreFailure() {
    // No failure should be ignored in reporting
    return false;
  }

  public String[] getJsKeys() {
    return jsKeys;
  }

  public boolean getJsDoubleQuoted() {
    return jsDoubleQuoted;
  }

  public String[] getUrlKeys() {
    return urlKeys;
  }

  public String[] getHtmlKeys() {
    return htmlKeys;
  }

  public File getXhtmlSchema() {
    return xhtmlSchema;
  }

  public String[] getTextKeys() {
    return textKeys;
  }

  public CustomPattern[] getCustomPatterns() {
    return customPatterns;
  }
}