package com.ccjizhang.javapoet;

import java.util.Arrays;
import java.util.List;

/**
 * 这是一个临时解决方案，用于修复JavaPoet的ParameterizedTypeName问题
 */
public class ParameterizedTypeName extends TypeName {
    
    private final TypeName rawType;
    private final List<TypeName> typeArguments;
    
    private ParameterizedTypeName(TypeName rawType, List<TypeName> typeArguments) {
        this.rawType = rawType;
        this.typeArguments = typeArguments;
    }
    
    /**
     * 从原始类型和类型参数创建参数化类型
     */
    public static ParameterizedTypeName get(TypeName rawType, TypeName... typeArguments) {
        return new ParameterizedTypeName(rawType, Arrays.asList(typeArguments));
    }
    
    /**
     * 从类和类型参数创建参数化类型
     */
    public static ParameterizedTypeName get(Class<?> rawType, TypeName... typeArguments) {
        return new ParameterizedTypeName(ClassName.get(rawType), Arrays.asList(typeArguments));
    }
    
    public TypeName rawType() {
        return rawType;
    }
    
    public List<TypeName> typeArguments() {
        return typeArguments;
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(rawType);
        if (!typeArguments.isEmpty()) {
            result.append("<");
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) result.append(", ");
                result.append(typeArguments.get(i));
            }
            result.append(">");
        }
        return result.toString();
    }
} 