package com.wwx.compiler;

import com.wwx.compiler.annotation.AutoDelete;
import com.wwx.compiler.annotation.AutoInsert;
import com.wwx.compiler.annotation.AutoSelect;
import com.wwx.compiler.annotation.AutoUpdate;
import org.apache.ibatis.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

public interface Constants {
    String OVER_RIDE = "@" + Override.class.getSimpleName() + "\n";
    String AUTOWIRED = "@" + Autowired.class.getSimpleName() + "\n";
    String COMPONENT = "@" + Component.class.getSimpleName() + "\n";
    String MAPPER_ANT = "@" + Mapper.class.getSimpleName() + "\n";
    String INSERT_PROVIDER = "@" + InsertProvider.class.getSimpleName();
    String SELECT_PROVIDER = "@" + SelectProvider.class.getSimpleName();
    String DELETE_PROVIDER = "@" + DeleteProvider.class.getSimpleName();
    String UPDATE_PROVIDER = "@" + UpdateProvider.class.getSimpleName();

    String PUBLIC = "public ";
    String PRIVATE = "private ";
    String RETURN = "return ";
    String PACKAGE = "package ";
    String IMPORT = "import ";
    String SELECT = "SELECT";
    String INSERT = "INSERT";
    String UPDATE = "UPDATE";
    String DELETE = "DELETE";
    String SELECT_ALL = "SELECT_ALL";

    String PROVIDER = "Provider";

    String SELECT_MODE = AutoSelect.class.getSimpleName();
    String INSERT_MODE = AutoInsert.class.getSimpleName();
    String UPDATE_MODE = AutoUpdate.class.getSimpleName();
    String DELETE_MODE = AutoDelete.class.getSimpleName();
}
