package com.wwx.compiler;

import com.wwx.compiler.annotation.*;

import java.util.List;

public interface Dao<T> {

    @AutoInsert
    int INSERT(T t);

    @AutoDelete
    int DELETE(T t);

    @AutoUpdate
    int UPDATE(T s, T c);

    @AutoSelectOne
    T SELECT_ONE(T t);

    @AutoSelect
    List<T> SELECT(T t);
}
