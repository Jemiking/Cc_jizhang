package com.ccjizhang.javapoet;

/**
 * 这是一个临时解决方案，用于修复JavaPoet的WildcardTypeName问题
 */
public class WildcardTypeName extends TypeName {
    
    private final TypeName upperBound;
    private final TypeName lowerBound;
    
    private WildcardTypeName(TypeName upperBound, TypeName lowerBound) {
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }
    
    /**
     * 创建通配符类型 ? extends upperBound
     */
    public static WildcardTypeName subtypeOf(TypeName upperBound) {
        return new WildcardTypeName(upperBound, null);
    }
    
    /**
     * 创建通配符类型 ? extends upperBound
     */
    public static WildcardTypeName subtypeOf(Class<?> upperBound) {
        return subtypeOf(ClassName.get(upperBound));
    }
    
    /**
     * 创建通配符类型 ? super lowerBound
     */
    public static WildcardTypeName supertypeOf(TypeName lowerBound) {
        return new WildcardTypeName(null, lowerBound);
    }
    
    /**
     * 创建通配符类型 ? super lowerBound
     */
    public static WildcardTypeName supertypeOf(Class<?> lowerBound) {
        return supertypeOf(ClassName.get(lowerBound));
    }
    
    public TypeName upperBound() {
        return upperBound;
    }
    
    public TypeName lowerBound() {
        return lowerBound;
    }
    
    @Override
    public String toString() {
        if (lowerBound != null) {
            return "? super " + lowerBound;
        } else if (upperBound != null && !OBJECT.equals(upperBound)) {
            return "? extends " + upperBound;
        } else {
            return "?";
        }
    }
} 