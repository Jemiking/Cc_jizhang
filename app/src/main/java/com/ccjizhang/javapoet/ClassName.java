package com.ccjizhang.javapoet;

import java.util.List;
import java.util.ArrayList;

/**
 * 这是一个临时解决方案，用于修复JavaPoet的ClassName问题
 * 这个类提供必要的方法以解决编译错误
 */
public class ClassName extends TypeName {
    private final String packageName;
    private final String simpleName;
    private final String canonicalName;

    private ClassName(String packageName, String simpleName) {
        this.packageName = packageName;
        this.simpleName = simpleName;
        this.canonicalName = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
    }

    /**
     * 返回此类型的规范名称
     */
    public String canonicalName() {
        return canonicalName;
    }

    /**
     * 获取包名
     */
    public String packageName() {
        return packageName;
    }

    /**
     * 获取简单名称
     */
    public String simpleName() {
        return simpleName;
    }

    /**
     * 从完全限定名称创建ClassName
     */
    public static ClassName get(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        if (lastDot == -1) {
            return new ClassName("", fullName);
        } else {
            return new ClassName(
                    fullName.substring(0, lastDot),
                    fullName.substring(lastDot + 1));
        }
    }

    /**
     * 从包名和简单名称创建ClassName
     */
    public static ClassName get(String packageName, String simpleName) {
        return new ClassName(packageName, simpleName);
    }

    /**
     * 从类创建一个ClassName实例
     */
    public static ClassName get(Class<?> clazz) {
        return new ClassName(clazz.getPackage().getName(), clazz.getSimpleName());
    }

    @Override
    public String toString() {
        return packageName.isEmpty() 
            ? simpleName 
            : packageName + "." + simpleName;
    }
} 