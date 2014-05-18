/*
 * Copyright 2004-2014 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.dbflute.logic.generate.language.grammar;

import java.util.List;
import java.util.Set;

import org.apache.torque.engine.database.model.Column;
import org.seasar.dbflute.helper.StringSet;
import org.seasar.dbflute.util.DfCollectionUtil;
import org.seasar.dbflute.util.Srl;

/**
 * @author jflute
 */
public class DfLanguageGrammarCSharp implements DfLanguageGrammar {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final Set<String> _pgReservColumnSet;
    static {
        // likely words only (and only can be checked at examples)
        final StringSet stringSet = StringSet.createAsCaseInsensitive();
        final List<String> list = DfCollectionUtil.newArrayList("class");
        stringSet.addAll(list);
        _pgReservColumnSet = stringSet;
    }

    // ===================================================================================
    //                                                                       Basic Keyword
    //                                                                       =============
    public String getClassFileExtension() {
        return "cs";
    }

    public String getExtendsStringMark() {
        return ":";
    }

    public String getImplementsStringMark() {
        return ":";
    }

    public String getPublicModifier() {
        return "public";
    }

    public String getProtectedModifier() {
        return "protected";
    }

    public String getPublicFinal() {
        return "public readonly";
    }

    public String getPublicStaticFinal() {
        return "public static readonly";
    }

    // ===================================================================================
    //                                                              Programming Expression
    //                                                              ======================
    public String buildVariableSimpleDefinition(String type, String variable) {
        return type + " " + variable;
    }

    public String adjustMethodInitialChar(String methodName) {
        return Srl.initCap(methodName);
    }

    public String adjustPropertyInitialChar(String propertyName) {
        return Srl.initCap(propertyName);
    }

    public String buildPropertyGetterCall(String propertyName) {
        return propertyName;
    }

    public String getClassTypeLiteral(String className) {
        return "typeof(" + className + ")";
    }

    public String buildGenericListClassName(String element) {
        return "IList<" + element + ">";
    }

    public String buildGenericMapListClassName(String key, String value) {
        return "IList<IDictionary<" + key + ", " + value + ">>";
    }

    public String buildGenericOneClassHint(String first) {
        return "<" + first + ">";
    }

    public String buildGenericTwoClassHint(String first, String second) {
        return "<" + first + ", " + second + ">";
    }

    public String buildEntityPropertyGetSet(Column fromCol, Column toCol) {
        return toCol.getJavaName() + " = this." + fromCol.getJavaName();
    }

    public String buildEntityPropertyName(Column col) {
        return col.getJavaName();
    }

    public String buildCDefElementValue(String cdefBase, String propertyName, String valueType, boolean toNumber,
            boolean toBoolean) {
        final String cdefCode = cdefBase + ".Code";
        if (toNumber || toBoolean) {
            return toValueTypeRemovedCSharpNullable(valueType) + ".Parse(" + cdefCode + ")";
        } else {
            return cdefCode;
        }
    }

    protected String toValueTypeRemovedCSharpNullable(String valueType) {
        return valueType.endsWith("?") ? Srl.substringLastFront(valueType, "?") : valueType;
    }

    public String buildOneLinerListNewBackStage(List<String> elementList) {
        final StringBuilder sb = new StringBuilder();
        for (String element : elementList) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(element);
        }
        return "newArrayList(" + sb.toString() + ")";
    }

    // ===================================================================================
    //                                                                    Small Adjustment 
    //                                                                    ================
    public boolean isPgReservColumn(String columnName) {
        return _pgReservColumnSet.contains(columnName);
    }

    public String adjustClassElementIndent(String baseIndent) {
        return baseIndent + "    ";
    }

    public String escapeJavaDocString(String comment) {
        return comment; // no adjustment for now
    }

    public String buildJavaDocCommentWithTitleIndentDirectly(String resolvedTitle, String adjustedIndent) {
        return adjustedIndent + "/// <summary>" + resolvedTitle + " </summary>";
    }

    public String buildJavaDocLineAndIndent(String sourceCodeLineSeparator, String baseIndent) {
        return doBuildJavaDocLineAndIndent(sourceCodeLineSeparator, adjustClassElementIndent(baseIndent));
    }

    public String buildJavaDocLineAndIndentDirectly(String sourceCodeLineSeparator, String adjustedIndent) {
        return doBuildJavaDocLineAndIndent(sourceCodeLineSeparator, adjustedIndent);
    }

    protected String doBuildJavaDocLineAndIndent(String sourceCodeLineSeparator, String adjustedIndent) {
        return sourceCodeLineSeparator + adjustedIndent + "/// ";
    }
}