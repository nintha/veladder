package top.nintha.veladder.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.nintha.veladder.annotations.RestController;
import top.nintha.veladder.controller.HelloController;
import top.nintha.veladder.controller.HelloRxController;

import java.util.Set;

class ClassScanUtilTest {

    @Test
    void scanAnnotation() {
        String packageName = "top.nintha.veladder";
        Set<Class<?>> classSet = ClassScanUtil.scanByAnnotation(packageName, RestController.class);
        Assertions.assertEquals(2, classSet.size());
        Assertions.assertTrue(classSet.contains(HelloRxController.class));
        Assertions.assertTrue(classSet.contains(HelloController.class));
    }
}