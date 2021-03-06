/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package com.intellij.util.xml.impl;

import com.intellij.openapi.util.Pair;

/**
 * @author peter
 */
public class GetFixedChildInvocation implements Invocation {
  private final Pair<FixedChildDescriptionImpl,Integer> myPair;

  public GetFixedChildInvocation(final Pair<FixedChildDescriptionImpl, Integer> pair) {
    myPair = pair;
  }

  public Object invoke(final DomInvocationHandler<?> handler, final Object[] args) throws Throwable {
    handler.checkIsValid();
    return handler.getFixedChild(myPair).getProxy();
  }
}
