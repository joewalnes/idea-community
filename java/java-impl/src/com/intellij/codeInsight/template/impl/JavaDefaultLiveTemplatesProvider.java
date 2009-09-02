package com.intellij.codeInsight.template.impl;

import org.jetbrains.annotations.NonNls;

/**
 * @author yole
 */
public class JavaDefaultLiveTemplatesProvider implements DefaultLiveTemplatesProvider {
  private static final @NonNls String[] DEFAULT_TEMPLATES = new String[]{
    "/liveTemplates/iterations",
    "/liveTemplates/other",
    "/liveTemplates/output",
    "/liveTemplates/plain",
    "/liveTemplates/surround",
    "/liveTemplates/css"
  };

  public String[] getDefaultLiveTemplateFiles() {
    return DEFAULT_TEMPLATES;
  }
}