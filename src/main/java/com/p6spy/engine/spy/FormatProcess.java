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
package com.p6spy.engine.spy;

import java.util.LinkedList;
import java.util.Locale;
import java.util.StringTokenizer;

import static com.p6spy.engine.spy.BasicFormatterImpl.*;

/**
 * @author Davi Távora
 * @since 02/2021
 */
public class FormatProcess {

  public static final String WHITESPACE = " \n\r\f\t";
  private static final String INDENT_STRING = "    ";
  private static final String INITIAL = System.lineSeparator() + INDENT_STRING;

  boolean beginLine = true;
  boolean afterBeginBeforeEnd;
  boolean afterByOrSetOrFromOrSelect;
  boolean afterOn;
  boolean afterBetween;
  boolean afterInsert;
  int inFunction;
  int parensSinceSelect;
  private LinkedList<Integer> parenCounts = new LinkedList<Integer>();
  private LinkedList<Boolean> afterByOrFromOrSelects = new LinkedList<Boolean>();

  int indent = 1;

  StringBuilder result = new StringBuilder();
  StringTokenizer tokens;
  String lastToken;
  String token;
  String lcToken;

  public FormatProcess(String sql) {
    tokens = new StringTokenizer(
      sql,
      "()+*/-=<>'`\"[]," + WHITESPACE,
      true
    );
  }

  public String perform() {

    result.append(INITIAL);

    while (tokens.hasMoreTokens()) {
      token = tokens.nextToken();
      lcToken = token.toLowerCase(Locale.ROOT);

      if ("'".equals(token)) {
        String t;
        do {
          t = tokens.nextToken();
          token += t;
        }
        while (!"'".equals(t) && tokens.hasMoreTokens());
      } else if ("\"".equals(token)) {
        String t;
        do {
          t = tokens.nextToken();
          token += t;
        }
        while (!"\"".equals(t) && tokens.hasMoreTokens());
      }
      else if ("[".equals(token)) {
        String t;
        do {
          t = tokens.nextToken();
          token += t;
        }
        while (!"]".equals(t) && tokens.hasMoreTokens());
      }

      if (afterByOrSetOrFromOrSelect && ",".equals(token)) {
        commaAfterByOrFromOrSelect();
      } else if (afterOn && ",".equals(token)) {
        commaAfterOn();
      } else if ("(".equals(token)) {
        openParen();
      } else if (")".equals(token)) {
        closeParen();
      } else if (BEGIN_CLAUSES.contains(lcToken)) {
        beginNewClause();
      } else if (END_CLAUSES.contains(lcToken)) {
        endNewClause();
      } else if ("select".equals(lcToken)) {
        select();
      } else if (DML.contains(lcToken)) {
        updateOrInsertOrDelete();
      } else if ("values".equals(lcToken)) {
        values();
      } else if ("on".equals(lcToken)) {
        on();
      } else if (afterBetween && lcToken.equals("and")) {
        misc();
        afterBetween = false;
      } else if (LOGICAL.contains(lcToken)) {
        logical();
      } else if (isWhitespace(token)) {
        white();
      } else {
        misc();
      }

      if (!isWhitespace(token)) {
        lastToken = lcToken;
      }

    }
    return result.toString();
  }

  private void commaAfterOn() {
    out();
    indent--;
    newline();
    afterOn = false;
    afterByOrSetOrFromOrSelect = true;
  }

  private void commaAfterByOrFromOrSelect() {
    out();
    newline();
  }

  private void logical() {
    if ("end".equals(lcToken)) {
      indent--;
    }
    newline();
    out();
    beginLine = false;
  }

  private void on() {
    indent++;
    afterOn = true;
    newline();
    out();
    beginLine = false;
  }

  private void misc() {
    out();
    if ("between".equals(lcToken)) {
      afterBetween = true;
    }
    if (afterInsert) {
      newline();
      afterInsert = false;
    } else {
      beginLine = false;
      if ("case".equals(lcToken)) {
        indent++;
      }
    }
  }

  private void white() {
    if (!beginLine) {
      result.append(" ");
    }
  }

  private void updateOrInsertOrDelete() {
    out();
    indent++;
    beginLine = false;
    if ("update".equals(lcToken)) {
      newline();
    }
    if ("insert".equals(lcToken)) {
      afterInsert = true;
    }
  }

  private void select() {
    out();
    indent++;
    newline();
    parenCounts.addLast(parensSinceSelect);
    afterByOrFromOrSelects.addLast(afterByOrSetOrFromOrSelect);
    parensSinceSelect = 0;
    afterByOrSetOrFromOrSelect = true;
  }

  private void out() {
    result.append(token);
  }

  private void endNewClause() {
    if (!afterBeginBeforeEnd) {
      indent--;
      if (afterOn) {
        indent--;
        afterOn = false;
      }
      newline();
    }
    out();
    if (!"union".equals(lcToken)) {
      indent++;
    }
    newline();
    afterBeginBeforeEnd = false;
    afterByOrSetOrFromOrSelect = "by".equals(lcToken)
      || "set".equals(lcToken)
      || "from".equals(lcToken);
  }

  private void beginNewClause() {
    if (!afterBeginBeforeEnd) {
      if (afterOn) {
        indent--;
        afterOn = false;
      }
      indent--;
      newline();
    }
    out();
    beginLine = false;
    afterBeginBeforeEnd = true;
  }

  private void values() {
    indent--;
    newline();
    out();
    indent++;
    newline();
  }

  private void closeParen() {
    parensSinceSelect--;
    if (parensSinceSelect < 0) {
      indent--;
      parensSinceSelect = parenCounts.removeLast();
      afterByOrSetOrFromOrSelect = afterByOrFromOrSelects.removeLast();
    }
    if (inFunction > 0) {
      inFunction--;
      out();
    } else {
      if (!afterByOrSetOrFromOrSelect) {
        indent--;
        newline();
      }
      out();
    }
    beginLine = false;
  }

  private void openParen() {
    if (isFunctionName(lastToken) || inFunction > 0) {
      inFunction++;
    }
    beginLine = false;
    if (inFunction > 0) {
      out();
    } else {
      out();
      if (!afterByOrSetOrFromOrSelect) {
        indent++;
        newline();
        beginLine = true;
      }
    }
    parensSinceSelect++;
  }

  private boolean isFunctionName(String tok) {
    if (tok == null || tok.length() == 0) {
      return false;
    }

    final char begin = tok.charAt(0);
    final boolean isIdentifier = Character.isJavaIdentifierStart(begin) || '"' == begin;
    return isIdentifier &&
      !LOGICAL.contains(tok) &&
      !END_CLAUSES.contains(tok) &&
      !QUANTIFIERS.contains(tok) &&
      !DML.contains(tok) &&
      !MISC.contains(tok);
  }

  private boolean isWhitespace(String token) {
    return WHITESPACE.contains(token);
  }

  private void newline() {
    result.append(System.lineSeparator());
    for (int i = 0; i < indent; i++) {
      result.append(INDENT_STRING);
    }
    beginLine = true;
  }

}
