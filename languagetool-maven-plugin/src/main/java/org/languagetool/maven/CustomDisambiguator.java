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


import org.apache.maven.plugin.logging.Log;
import org.languagetool.AnalyzedSentence;
import org.languagetool.tagging.disambiguation.Disambiguator;
import org.languagetool.tagging.disambiguation.rules.DisambiguationPatternRule;
import org.languagetool.tagging.disambiguation.rules.DisambiguationRuleLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Load the disambiguators {resourcePath}/languageTool/disambiguator.xml
 * and {resourcePath}/languageTool/XX/disambiguator.xml (XX the short name of the selected language)
 *
 * The first disambiguator can add some exceptions for every language
 * The second disambiguator can add some exceptions for the selected language
 *
 * @author <a href="mailto:v.maubert@code-troopers.com">vmaubert</a>
 */
public class CustomDisambiguator implements Disambiguator {


  public static final String DISAMBIGUATION_FILE = "disambiguator.xml";
  private List<DisambiguationPatternRule> disambiguationRules;
  private final String shortNameLanguage;
  private String resourcePath;
  private Log log;


  public CustomDisambiguator(String shortNameLanguage, String resourcePath, Log log) {
    this.shortNameLanguage = shortNameLanguage;
    this.resourcePath = resourcePath;
    this.log = log;
  }

  @Override
  public final AnalyzedSentence disambiguate(AnalyzedSentence input)
      throws IOException {

    AnalyzedSentence sentence = input;
    if (disambiguationRules == null) {
      //Exceptions for every language
      disambiguationRules = loadDisambiguationRules(resourcePath + DISAMBIGUATION_FILE);

      //Exceptions for the selected language
      List<DisambiguationPatternRule> rulesLocale = loadDisambiguationRules(resourcePath + shortNameLanguage + File.separator + DISAMBIGUATION_FILE);
      disambiguationRules.addAll(rulesLocale);
    }
    for (final DisambiguationPatternRule patternRule : disambiguationRules) {
      sentence = patternRule.replace(sentence);
    }

    return sentence;
  }

  private List<DisambiguationPatternRule> loadDisambiguationRules(String filePath) {
    InputStream inputStream = null;
    try {
      final DisambiguationRuleLoader ruleLoader = new DisambiguationRuleLoader();
      inputStream = new FileInputStream(filePath);
      return ruleLoader.getRules(inputStream);

    } catch (FileNotFoundException e) {
      log.info("Optional diambiguator file not found : " + filePath + ". This file is useful if you want to add some exceptions.");
    } catch (Exception e) {
      throw new RuntimeException("Problems with loading disambiguation file: " + filePath, e);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          // nothing to do
        }
      }
    }

    return Collections.emptyList();

  }

}