package com.siyeh.ig.bugs;

import com.IGInspectionTestCase;

public class ConstantAssertConditionInspectionTest extends IGInspectionTestCase {

    public void test() throws Exception {
        doTest("com/siyeh/igtest/bugs/constant_assert_condition",
                new ConstantAssertConditionInspection());
    }
}