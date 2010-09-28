/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.editor.impl.softwrap.mapping;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.impl.EditorTextRepresentationHelper;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapsStorage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
* @author Denis Zhdanov
* @since Sep 9, 2010 10:08:08 AM
*/
@SuppressWarnings({"UnusedDeclaration"})
class LogicalToVisualMappingStrategy extends AbstractMappingStrategy<VisualPosition> {

  private LogicalPosition myTargetLogical;

  LogicalToVisualMappingStrategy(@NotNull final LogicalPosition logical, Editor editor, final SoftWrapsStorage storage,
                                 EditorTextRepresentationHelper representationHelper, final List<CacheEntry> cache)
    throws IllegalStateException
  {
    super(editor, storage, representationHelper);
    myTargetLogical = logical;
  }

  public void init(@NotNull final LogicalPosition logical, final @NotNull List<CacheEntry> cache) {
    setEagerMatch(null);

    myTargetLogical = logical;
    int start = 0;
    int end = cache.size() - 1;

    // We inline binary search here because profiling indicates that it becomes bottleneck to use Collections.binarySearch().
    while (start <= end) {
      int i = (end + start) >>> 1;
      CacheEntry cacheEntry = cache.get(i);

      // There is a possible case that single logical line is represented on multiple visual lines due to soft wraps processing.
      // Hence, we check for bot logical line and logical columns during searching 'anchor' cache entry.

      if (cacheEntry.endLogicalLine < logical.line
          || (cacheEntry.endLogicalLine == logical.line && myStorage.getSoftWrap(cacheEntry.endOffset) != null
              && cacheEntry.endLogicalColumn <= logical.column)) {
        start = i + 1;
        continue;
      }
      if (cacheEntry.startLogicalLine > logical.line
          || (cacheEntry.startLogicalLine == logical.line
              && cacheEntry.startLogicalColumn > logical.column)) {
        end = i - 1;
        continue;
      }

      // There is a possible case that currently found cache entry corresponds to soft-wrapped line and soft wrap occurred
      // at target logical column. We need to return cache entry for the next visual line then (because single logical column
      // is shared for 'before soft wrap' and 'after soft wrap' positions and we want to use the one that points to
      // 'after soft wrap' position).
      if (cacheEntry.endLogicalLine == logical.line && cacheEntry.endLogicalColumn == logical.column && i < cache.size() - 1) {
        CacheEntry nextLineCacheEntry = cache.get(i + 1);
        if (nextLineCacheEntry.startLogicalLine == logical.line
            && nextLineCacheEntry.startLogicalColumn == logical.column) {
          setCacheEntry(nextLineCacheEntry);
          return;
        }
      }
      setCacheEntry(cacheEntry);
      return;
    }

    throw new IllegalStateException(String.format(
      "Can't map logical position (%s) to visual position. Reason: no cached information information about target visual "
      + "line is found. Registered entries: %s", logical, cache
    ));
  }

  @Override
  protected VisualPosition buildIfExceeds(ProcessingContext context, int offset) {
    if (context.logicalLine < myTargetLogical.line) {
       return null;
    }

    int diff = myTargetLogical.column - context.logicalColumn;
    if (offset - context.offset < diff) {
      return null;
    }

    context.visualColumn += diff;
    // Don't update other dimensions like logical position and offset because we need only visual position here.
    return context.buildVisualPosition();
  }

  @Override
  protected VisualPosition buildIfExceeds(@NotNull ProcessingContext context, @NotNull FoldRegion foldRegion) {
    int foldEndLine = myEditor.getDocument().getLineNumber(foldRegion.getEndOffset());
    if (myTargetLogical.line > foldEndLine) {
      return null;
    }

    if (myTargetLogical.line < foldEndLine) {
      // Map all logical position that point inside collapsed fold region to visual position of its start.
      return context.buildVisualPosition();
    }

    int foldEndColumn = getFoldRegionData(foldRegion).getCollapsedSymbolsWidthInColumns();
    if (foldEndLine == context.logicalLine) {
      // Single-line fold region.
      foldEndColumn += context.logicalColumn;
    }

    if (foldEndColumn <= myTargetLogical.column) {
      return null;
    }

    // Map all logical position that point inside collapsed fold region to visual position of its start.
    return context.buildVisualPosition();
  }

  @Override
  protected VisualPosition buildIfExceeds(ProcessingContext context, TabData tabData) {
    if (context.logicalLine < myTargetLogical.line) {
      return null;
    }

    int diff = myTargetLogical.column - context.logicalColumn;
    if (diff >= tabData.widthInColumns) {
      return null;
    }

    context.logicalColumn += diff;
    context.visualColumn += diff;

    return context.buildVisualPosition();
  }

  @Override
  public VisualPosition processSoftWrap(ProcessingContext context, SoftWrap softWrap) {
    context.visualColumn = softWrap.getIndentInColumns();
    context.softWrapColumnDiff += softWrap.getIndentInColumns();

    if (context.logicalLine < myTargetLogical.line || context.logicalColumn != myTargetLogical.column) {
      return null;
    }
    return context.buildVisualPosition();
  }

  @NotNull
  @Override
  public VisualPosition build(ProcessingContext context) {
    int diff = myTargetLogical.column - context.logicalColumn;
    context.logicalColumn += diff;
    context.visualColumn += diff;
    context.offset += diff;
    return context.buildVisualPosition();
  }
}
