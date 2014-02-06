/* LanguageTool, a natural language style checker
 * Copyright (C) 2014 Vincent Maubert(http://www.languagetool.org)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package org.languagetool.maven;


import org.apache.commons.lang.StringEscapeUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.patterns.PatternRule;
import org.languagetool.rules.patterns.PatternRuleLoader;
import org.languagetool.tools.StringTools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:v.maubert@code-troopers.com">vmaubert</a>
 */
public class CheckLanguageFile {
  public static final String LT_RESOURCES_PATH = "languageTool" + File.separator;
  protected static Pattern prefixPattern = Pattern.compile(".*_(.*)\\..*");
  private final LanguageFile languageFile;
  private final String resourcesPath;
  private final Log log;
  private boolean ignoreEmptyKey;

  public CheckLanguageFile(LanguageFile languageFile, final String resourcesPath, final Log log, boolean ignoreEmptyKey) {
    this.languageFile = languageFile;
    this.resourcesPath = resourcesPath + File.separator;
    this.ignoreEmptyKey = ignoreEmptyKey;
    this.log = log;
  }

  public List<String> check() throws MojoExecutionException, MojoFailureException {
    List<String> errorsList = new ArrayList<String>();
    String propertyFileName = languageFile.getTarget();
    InputStream inputStream = null;
    String filePath = resourcesPath + propertyFileName;
    try {
      //Load the language file
      inputStream = new FileInputStream(filePath);

      String locale = extractLocaleFromLanguageTool(languageFile);
      JLanguageTool langTool = getLanguageTool(locale);
      Properties props = new Properties();
      try {
        props.load(inputStream);
      } catch (IOException e) {
        throw new MojoExecutionException("Could not load Properties", e);
      }

      for (String key : props.stringPropertyNames()) {

        String line = props.getProperty(key);
        if (line.isEmpty() && !ignoreEmptyKey) {
          errorsList.add(key + " Empty key !!! ");
        }
        line = formatLine(line);
        List<RuleMatch> matches = null;
        try {
          matches = langTool.check(line);
        } catch (IOException e) {
          throw new MojoExecutionException("LanguageTool error, could not check the line : " + line, e);
        }

        for (RuleMatch match : matches) {
          String error = getErrorToString(line, key, match);
          errorsList.add(error);
        }
      }

    } catch (FileNotFoundException e) {
      throw new MojoExecutionException("Could not find the properties file " + resourcesPath + propertyFileName, e);
    } finally {
      closeInputStream(inputStream);
    }

    return errorsList;
  }

  private JLanguageTool getLanguageTool(final String shortNameLanguage) throws MojoExecutionException {
    final Language language = Language.getLanguageForShortName(shortNameLanguage);

    String rulesFilePath = resourcesPath + LT_RESOURCES_PATH + shortNameLanguage + File.separator + "rules.xml";

    JLanguageTool langTool = null;
    InputStream inputStream = null;
    try {
      CustomDisambiguator customDisambiguator = new CustomDisambiguator(language.getShortName(), resourcesPath + LT_RESOURCES_PATH, log);
      language.setCustomDisambiguator(customDisambiguator);
      langTool = new JLanguageTool(language);


      langTool.activateDefaultPatternRules();

      // We load the rules for the language. These rules are in the file rules_XX.xml
      final PatternRuleLoader ruleLoader = new PatternRuleLoader();
      inputStream = new FileInputStream(rulesFilePath);
      List<PatternRule> patternRules = ruleLoader.getRules(inputStream, rulesFilePath);

      for (PatternRule rule : patternRules) {
        //Hack for disable a rule
        if (rule.getMessage().equals("disable")) {
          langTool.disableRule(rule.getId());
        } else {
          langTool.addRule(rule);
        }
      }

      String pathGlobalFile = resourcesPath + LT_RESOURCES_PATH + "ignore.txt";
      addIgnoredWordsContainsInFile(pathGlobalFile, langTool);

      String pathLocaleFile = resourcesPath + LT_RESOURCES_PATH + shortNameLanguage + File.separator + "ignore.txt";
      addIgnoredWordsContainsInFile(pathLocaleFile, langTool);

    } catch (FileNotFoundException e) {
      log.info("Optional file not found " + rulesFilePath + ". This file is useful if you want to add some rules.");
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      closeInputStream(inputStream);
    }

    return langTool;
  }

  private void addIgnoredWordsContainsInFile(String filePath, JLanguageTool langTool) throws MojoExecutionException {
    List<String> ignoredWords = new ArrayList<String>();
    InputStream inputStream = null;
    try {
      inputStream = new FileInputStream(filePath);

      Scanner scanner = new Scanner(inputStream, "utf-8");
      while (scanner.hasNextLine()) {
        final String line = scanner.nextLine();
        final boolean isComment = line.startsWith("#");
        if (isComment) {
          continue;
        }
        ignoredWords.add(line);
      }

      if (!ignoredWords.isEmpty()) {
        langTool.addIgnoreWords(ignoredWords);
      }
    } catch (FileNotFoundException e) {
      log.info("Optional file not found " + filePath + ". This file is useful if you want to add some words in the dictionary.");
    } finally {
      closeInputStream(inputStream);
    }

  }

  private static void closeInputStream(InputStream inputStream) {
    if (inputStream != null) {
      try {
        inputStream.close();
      } catch (IOException e) {
        // nothing to do
      }
    }
  }

  static String extractLocaleFromLanguageTool(final LanguageFile languageFile) {
    if (languageFile.getLanguage().isEmpty()) {
      return languageFile.getLanguage();
    }

    final Pattern compile = CheckLanguageFile.prefixPattern;
    final Matcher matcher = compile.matcher(languageFile.getTarget());
    if (matcher.matches()) {
      if (matcher.groupCount() > 0) {
        return matcher.group(1);
      }
    }
    return "";
  }

  private String formatLine(String line) {
    line = StringTools.filterXML(line);
    line = StringEscapeUtils.unescapeJava(line);
    return line;
  }


  private String getErrorToString(String line, String key, RuleMatch match) {
    StringBuilder builder = new StringBuilder(key);
    builder.append("=");
    builder.append(line);
    builder.append("\n Rule : ");
    builder.append(match.getRule().getId());
    builder.append(", Potential error: ")
        .append(match.getMessage());


    if (match.getEndColumn() > match.getColumn()) {
      builder.append(" [")
          .append(line.substring(match.getColumn() - 1, match.getEndColumn() - 1));
      builder.append("] ");
    }

    List<String> suggestedReplacements = match.getSuggestedReplacements();
    if (!suggestedReplacements.isEmpty()) {
      builder.append("Suggested correction: ")
          .append(suggestedReplacements);
    }
    return builder.toString();
  }
}