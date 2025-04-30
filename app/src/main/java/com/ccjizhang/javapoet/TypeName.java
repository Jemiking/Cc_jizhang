package com.ccjizhang.javapoet;

/**
 * 这是一个临时解决方案，用于修复JavaPoet的TypeName问题
 */
public abstract class TypeName {
    
    public static final TypeName VOID = new TypeName() {};
    public static final TypeName BOOLEAN = new TypeName() {};
    public static final TypeName BYTE = new TypeName() {};
    public static final TypeName SHORT = new TypeName() {};
    public static final TypeName INT = new TypeName() {};
    public static final TypeName LONG = new TypeName() {};
    public static final TypeName CHAR = new TypeName() {};
    public static final TypeName FLOAT = new TypeName() {};
    public static final TypeName DOUBLE = new TypeName() {};
    public static final TypeName OBJECT = new TypeName() {};
    
    protected TypeName() {}
    
    public String toString() {
        return "TypeName";
    }
} 