/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ig.psiutils;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ObjectUtils;
import one.util.streamex.StreamEx;

/**
 * Utility methods related to the expressions of functional type
 *
 * @author Tagir Valeev
 */
public class FunctionalExpressionUtils {
  /**
   * Returns true if the supplied expression is the functional expression (method reference or lambda)
   * which refers to the given method call
   *
   * @param expression     expression to test
   * @param className      class name where the wanted method should be located
   * @param returnType     the return type of the wanted method (null if should not be checked)
   * @param methodName     method name of the wanted method
   * @param parameterTypes wanted method parameter types (nulls for parameters which should not be checked)
   * @return true if the supplied expression references the wanted call
   */
  public static boolean isFunctionalReferenceTo(PsiExpression expression, String className, PsiType returnType,
                                                String methodName, PsiType... parameterTypes) {
    expression = PsiUtil.skipParenthesizedExprDown(expression);
    if (expression instanceof PsiMethodReferenceExpression) {
      PsiMethodReferenceExpression methodRef = (PsiMethodReferenceExpression)expression;
      if (!methodName.equals(methodRef.getReferenceName())) return false;
      PsiMethod method = ObjectUtils.tryCast(methodRef.resolve(), PsiMethod.class);
      PsiReferenceExpression ref = ObjectUtils.tryCast(methodRef.getQualifier(), PsiReferenceExpression.class);
      return ref != null &&
             method != null &&
             MethodUtils.methodMatches(method, className, returnType, methodName, parameterTypes) &&
             ref.isReferenceTo(method.getContainingClass());
    }
    if (expression instanceof PsiLambdaExpression) {
      PsiLambdaExpression lambda = (PsiLambdaExpression)expression;
      PsiExpression body = PsiUtil.skipParenthesizedExprDown(LambdaUtil.extractSingleExpressionFromBody(lambda.getBody()));
      PsiMethodCallExpression call = ObjectUtils.tryCast(body, PsiMethodCallExpression.class);
      if (call == null || !MethodCallUtils.isCallToMethod(call, className, returnType, methodName, parameterTypes)) return false;
      PsiParameter[] parameters = lambda.getParameterList().getParameters();
      PsiExpression[] args = call.getArgumentList().getExpressions();
      PsiMethod method = call.resolveMethod();
      if (method != null && !method.hasModifierProperty(PsiModifier.STATIC)) {
        args = ArrayUtil.prepend(call.getMethodExpression().getQualifierExpression(), args);
      }
      if (parameters.length != args.length || StreamEx.zip(args, parameters, ExpressionUtils::isReferenceTo).has(false)) return false;
      return MethodCallUtils.isCallToMethod(call, className, returnType, methodName, parameterTypes);
    }
    return false;
  }
}
