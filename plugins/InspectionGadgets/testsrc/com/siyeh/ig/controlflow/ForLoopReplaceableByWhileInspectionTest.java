package com.siyeh.ig.controlflow;

import com.IGInspectionTestCase;

public class ForLoopReplaceableByWhileInspectionTest extends IGInspectionTestCase {

    public void test() throws Exception {
        doTest("com/siyeh/igtest/controlflow/for_loop_replaceable_by_while", 
                new ForLoopReplaceableByWhileInspection());
    }
}