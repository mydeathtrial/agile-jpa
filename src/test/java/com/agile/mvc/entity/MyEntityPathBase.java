package com.agile.mvc.entity;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.ComparablePath;
import com.querydsl.core.types.dsl.DatePath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.SimplePath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.core.types.dsl.TimePath;

/**
 * @author 佟盟
 * 日期 2021-01-29 14:08
 * 描述 TODO
 * @version 1.0
 * @since 1.0
 */
public class MyEntityPathBase<T> extends EntityPathBase<T> {
    public MyEntityPathBase(Class<? extends T> type, String variable) {
        super(type, variable);
    }


    public <P extends Path<?>> P add(P path) {
        return super.add(path);
    }

    public <A, E> ArrayPath<A, E> createArray(String property, Class<? super A> type) {
        return super.createArray(property, type);
    }

    public BooleanPath createBoolean(String property) {
        return super.createBoolean(property);
    }

    public <A extends Comparable> ComparablePath<A> createComparable(String property, Class<? super A> type) {
        return super.createComparable(property, type);
    }

    public <A extends Enum<A>> EnumPath<A> createEnum(String property, Class<A> type) {
        return super.createEnum(property, type);
    }

    public <A extends Comparable> DatePath<A> createDate(String property, Class<? super A> type) {
        return super.createDate(property, type);
    }

    public <A extends Comparable> DateTimePath<A> createDateTime(String property, Class<? super A> type) {
        return super.createDateTime(property, type);
    }

    public NumberPath<Long> createLong(String property) {
        return super.createNumber(property, Long.class);
    }

    public NumberPath<Integer> createInteger(String property) {
        return super.createNumber(property, Integer.class);
    }

    public NumberPath<Short> createShort(String property) {
        return super.createNumber(property, Short.class);
    }

    public NumberPath<Double> createDouble(String property) {
        return super.createNumber(property, Double.class);
    }

    public NumberPath<Float> createFloat(String property) {
        return super.createNumber(property, Float.class);
    }

    public <A> SimplePath<A> createSimple(String property, Class<? super A> type) {
        return super.createSimple(property, type);
    }

    public StringPath createString(String property) {
        return super.createString(property);
    }

    public <A extends Comparable> TimePath<A> createTime(String property, Class<? super A> type) {
        return super.createTime(property, type);
    }

    public PathMetadata forProperty(String property) {
        return super.forProperty(property);
    }
}
