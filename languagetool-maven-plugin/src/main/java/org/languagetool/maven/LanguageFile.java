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


/**
 * The POJO to hold the checking configuration.
 *
 * @author <a href="mailto:v.maubert@code-troopers.com">vmaubert</a>
 */
public class LanguageFile {
  /**
   * The target file
   *
   * @parameter
   * @required
   */
  private String target;

  /**
   * The small name of the language (examples : "fr", "de"...)
   *
   * @parameter
   */
  private String language = "";


  /**
   * Returns the target file where the result of the merging should be saved.
   *
   * @return The target file.
   */
  public String getTarget() {
    return target;
  }

  public String getLanguage() {
    return language;
  }
}