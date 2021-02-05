/**
 * P6Spy
 *
 * Copyright (C) 2002 P6Spy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.p6spy.engine.spy.appender;

import com.p6spy.engine.spy.BasicFormatterImpl;
import com.p6spy.engine.spy.Formatter;
import com.p6spy.engine.spy.P6SpyOptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Davi Távora
 * @since 02/2021
 */
public class PrettySqlFormat extends CustomLineFormat {

  private static final MessageFormattingStrategy FALLBACK_FORMATTING_STRATEGY = new MultiLineFormat();

  private final Formatter formatter = new BasicFormatterImpl();

  @Override
  public String formatMessage(final int connectionId, final String now, final long elapsed, final String category, final String prepared, final String sql, final String url) {

    String customLogMessageFormat = P6SpyOptions.getActiveInstance().getCustomLogMessageFormat();

    if (customLogMessageFormat == null) {
      return FALLBACK_FORMATTING_STRATEGY.formatMessage(connectionId, now, elapsed, category, prepared, sql, url);
    }

    return customLogMessageFormat
      .replaceAll(Pattern.quote(CONNECTION_ID), Integer.toString(connectionId))
      .replaceAll(Pattern.quote(CURRENT_TIME), now)
      .replaceAll(Pattern.quote(EXECUTION_TIME), Long.toString(elapsed))
      .replaceAll(Pattern.quote(CATEGORY), category)
      .replaceAll(Pattern.quote(EFFECTIVE_SQL), Matcher.quoteReplacement(formatter.format(prepared)))
      .replaceAll(Pattern.quote(EFFECTIVE_SQL_SINGLELINE), Matcher.quoteReplacement(formatter.format(prepared)))
      .replaceAll(Pattern.quote(SQL), Matcher.quoteReplacement(formatter.format(sql)))
      .replaceAll(Pattern.quote(SQL_SINGLE_LINE), Matcher.quoteReplacement(formatter.format(sql)))
      .replaceAll(Pattern.quote(URL), url);
  }


}
