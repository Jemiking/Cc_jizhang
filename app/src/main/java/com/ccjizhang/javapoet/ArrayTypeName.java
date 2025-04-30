package com.ccjizhang.javapoet;

/**
 * 这是一个临时解决方案，用于修复JavaPoet的ArrayTypeName问题
 */
public class ArrayTypeName extends TypeName {
    
    private final TypeName componentType;
    
    private ArrayTypeName(TypeName componentType) {
        this.componentType = componentType;
    }
    
    /**
     * 从组件类型创建数组类型
     */
    public static ArrayTypeName of(TypeName componentType) {
        return new ArrayTypeName(componentType);
    }
    
    /**
     * 从Class创建数组类型
     */
    public static ArrayTypeName of(Class<?> componentType) {
        return of(ClassName.get(componentType));
    }
    
    public TypeName componentType() {
        return componentType;
    }
    
    @Override
    public String toString() {
        return componentType + "[]";
    }
} 