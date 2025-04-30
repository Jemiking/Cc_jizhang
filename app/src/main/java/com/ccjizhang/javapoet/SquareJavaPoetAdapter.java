package com.ccjizhang.javapoet;

/**
 * 这个类负责将我们的自定义JavaPoet实现桥接到原始包
 */
public class SquareJavaPoetAdapter {
    
    /**
     * 获取原始ClassName类的类型
     */
    public static Class<?> getOriginalClassName() {
        try {
            return Class.forName("com.squareup.javapoet.ClassName");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("未找到JavaPoet库", e);
        }
    }
    
    /**
     * 创建一个字符串形式的完全限定类名
     */
    public static String getCanonicalName(ClassName className) {
        return className.packageName().isEmpty() 
            ? className.simpleName() 
            : className.packageName() + "." + className.simpleName();
    }
} 