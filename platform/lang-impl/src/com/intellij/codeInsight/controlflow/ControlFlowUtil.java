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
package com.intellij.codeInsight.controlflow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @author oleg
 */
public class ControlFlowUtil {
  private static final Logger LOG = Logger.getInstance(ControlFlowUtil.class.getName());

  private ControlFlowUtil() {
  }

  public static class Stack {

    private final int myCapacity;
    private final int[] myValues;
    private int myIndex;
    public Stack(final int capacity) {
      myCapacity = capacity;
      myValues = new int[myCapacity];
      clear();
    }

    public void push(final int value) {
      myValues[++myIndex] = value;
    }

    public int pop() {
      assert !isEmpty() : "Cannot pop on empty stack";
      return myValues[myIndex--];
    }

    public boolean isEmpty() {
      return myIndex == -1;
    }

    public void clear() {
      myIndex = -1;
    }

    @Override
    public String toString() {
      return "Stack(" + (myIndex + 1) + ") elements";
    }

  }
  public static int[] postOrder(Instruction[] flow) {
    final int length = flow.length;
    int[] result = new int[length];
    boolean[] visited = new boolean[length];
    Arrays.fill(visited, false);
    final Stack stack = new Stack(length);

    int N = 0;
    for (int i = 0; i < length; i++) { //graph might not be connected
      if (!visited[i]) {
        visited[i] = true;
        stack.clear();
        stack.push(i);

        while (!stack.isEmpty()) {
          final int num = stack.pop();
          result[N++] = num;
          for (Instruction succ : flow[num].allSucc()) {
            final int succNum = succ.num();
            if (!visited[succNum]) {
              visited[succNum] = true;
              stack.push(succNum);
            }
          }
        }
      }
    }
    LOG.assertTrue(N == length);
    return result;
  }

  public static int findInstructionNumberByElement(final Instruction[] flow, final PsiElement element){
    for (int i=0;i<flow.length;i++) {
      // Check if canceled
      ProgressManager.checkCanceled();

      if (element == flow[i].getElement()){
        return i;
      }
    }
    return -1;
  }

  // Process control flow in depth first order

  public static boolean process(final Instruction[] flow, final int start, final Processor<Instruction> processor){
    final int length = flow.length;
    boolean[] visited = new boolean[length];
    Arrays.fill(visited, false);

    final ControlFlowUtil.Stack stack = new ControlFlowUtil.Stack(length);
    stack.push(start);

    while (!stack.isEmpty()) {
      // Check if canceled
      ProgressManager.checkCanceled();

      final int num = stack.pop();
      if (num == length - 1){
        continue;
      }
      final Instruction instruction = flow[num];
      if (!processor.process(instruction)){
        return false;
      }
      for (Instruction succ : instruction.allSucc()) {
        final int succNum = succ.num();
        if (!visited[succNum]) {
          visited[succNum] = true;
          stack.push(succNum);
        }
      }
    }
    return true;
  }


  public static void iteratePrev(final int startInstruction,
                                 @NotNull final Instruction[] instructions,
                                 @NotNull final Function<Instruction, Operation> closure) {
    final Stack stack = new Stack(instructions.length);
    final boolean[] visited = new boolean[instructions.length];

    stack.push(startInstruction);
    while (!stack.isEmpty()) {
      // Check if canceled
      ProgressManager.checkCanceled();

      final int num = stack.pop();
      if (visited[num]){
        continue;
      }
      visited[num] = true;
      final Instruction instr = instructions[num];
      final Operation nextOperation = closure.fun(instr);
      if (nextOperation == Operation.CONTINUE) {
        continue;
      } else if (nextOperation == Operation.BREAK) {
        break;
      }
      for (Instruction pred : instr.allPred()) {
        stack.push(pred.num());
      }
    }
  }

  public static enum Operation {
    CONTINUE, BREAK, NEXT;
  }
}