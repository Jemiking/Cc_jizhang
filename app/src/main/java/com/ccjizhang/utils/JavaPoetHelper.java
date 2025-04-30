package com.ccjizhang.utils;

import com.ccjizhang.javapoet.*;

/**
 * 辅助类，帮助在运行时初始化JavaPoet类
 * 这个类对于促使KSP能够找到并加载JavaPoet类可能有帮助
 */
public class JavaPoetHelper {
    
    static {
        // 确保JavaPoet核心类被加载
        TypeName typeName = TypeName.OBJECT;
        ClassName className = ClassName.get("test.package", "TestClass");
        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(
                className, TypeName.OBJECT);
        WildcardTypeName wildcardTypeName = WildcardTypeName.subtypeOf(Object.class);
        ArrayTypeName arrayTypeName = ArrayTypeName.of(className);
    }
    
    /**
     * 预加载JavaPoet类
     */
    public static void preloadJavaPoetClasses() {
        // 空方法，仅用于触发静态初始化块
    }
} 