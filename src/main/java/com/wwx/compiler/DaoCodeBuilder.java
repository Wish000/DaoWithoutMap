package com.wwx.compiler;

import com.wwx.compiler.annotation.*;
import com.wwx.compiler.annotation.sql.ID;
import com.wwx.compiler.exception.DynamicCompileException;
import com.wwx.compiler.exception.IDaoMethodArgumentException;
import com.wwx.compiler.exception.IDaoTypeParamException;
import com.wwx.compiler.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static com.wwx.compiler.ComponentScanner.CLASS_SUFFIX;
import static com.wwx.compiler.Constants.*;
import static com.wwx.compiler.DaoFactory.idaoPkg;


/**
 * dao代码生产器
 * 生成DaoImpl代码，Mapper代码，Provider代码
 * 其中provider代码中有用到bean信息
 * 所以，这个类中需要封装dao接口类对象、dao的须代理方法对象与之注解组的映射、bean的类对象
 */
class DaoCodeBuilder {
    static final String IMPL = "Impl";
    static final String MAPPER = "Mapper";
    private static final String PREFIX = "Proxy";
    private static final String PROVIDER = "Provider";
    /**
     * 指定注解
     */
    private final List<Class> definedAnnotationList;
    /**
     * dao接口类对象
     */
    private final Class iDao;
    /**
     * Impl的根路径
     */
    private String implPath;
    /**
     * Mapper和Provider的根路径
     */
    private String mapperPath;
    /**
     * 须代理方法
     */
    private Map<Method, List<Annotation>> methodMap = new HashMap<>();
    /**
     * bean的类对象
     */
    private Class beanClz;
    /**
     * 所有impl方法代码，方法与所属接口类名映射
     */
    private List<ImplMethodCodeCache> implMethodCodes = new ArrayList<>(4);
    /**
     * 所有mapper方法代码，方法与所属接口类名映射
     */
    private List<MapperMethodCodeCache> mapperMethodCodes = new ArrayList<>(4);
    /**
     * 所有provider方法代码，方法与所属接口类名映射
     */
    private List<ProviderMethodCodeCache> providerMethodCodes = new ArrayList<>(4);

    DaoCodeBuilder(Class iDao) {
        definedAnnotationList = new ArrayList<>(5);
        definedAnnotationList.add(AutoSelect.class);
        definedAnnotationList.add(AutoUpdate.class);
        definedAnnotationList.add(AutoInsert.class);
        definedAnnotationList.add(AutoDelete.class);
        definedAnnotationList.add(AutoSelectOne.class);
        this.iDao = iDao;
        init();
    }

    /**
     * 创建Impl、Mapper、Provider三个类的代码，以map封装
     * map封装的value是 类名->代码 的map
     *
     * @return 三个类的代码map
     */
    Map<String, Map<String, String>> buildCode() {
        createMethodsCode();
        Map<String, Map<String, String>> codeMap = new HashMap<>(3);
        Map<String, String> implCode = buildImplCode();
        Map<String, String> mapperCode = buildMapperCode();
        Map<String, String> providerCode = buildProviderCode();
        codeMap.put(IMPL, implCode);
        codeMap.put(MAPPER, mapperCode);
        codeMap.put(PROVIDER, providerCode);
        return codeMap;
    }

    private Map<String, String> buildImplCode() {
        Map<String, String> implCode = new HashMap<>(1);
        StringBuilder code = new StringBuilder();
        code.append(PACKAGE).append(idaoPkg).append(".impl;\n");
        // imports
        code.append(IMPORT).append(iDao.getName()).append(";\n");
        code.append(IMPORT).append(beanClz.getName()).append(";\n");
        code.append(IMPORT).append(mapperPath).append(".").append(beanClz.getSimpleName()).append("ProxyMapper;\n");
        code.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        code.append("import org.springframework.stereotype.Component;\n");
        code.append("import java.util.List;\n");
        // class
        code.append(COMPONENT);
        String className = iDao.getSimpleName() + "Proxy" + IMPL;
        code.append(PUBLIC).append("class ").append(className).append(" implements ").append(iDao.getSimpleName()).append(" {\n");
        // fields
        code.append("\t").append(AUTOWIRED);
        code.append("\t").append(PRIVATE).append(beanClz.getSimpleName()).append("ProxyMapper ").append("mapper;\n");
        // methods
        for (ImplMethodCodeCache methodCodeCache : implMethodCodes) {
            code.append(methodCodeCache);
        }
        code.append("}\n");
        implCode.put(className, code.toString());
        return implCode;
    }

    private Map<String, String> buildMapperCode() {
        Map<String, String> mapperCode = new HashMap<>(1);
        StringBuilder code = new StringBuilder();
        code.append(PACKAGE).append(mapperPath).append(";\n");
        // imports
        code.append("import org.apache.ibatis.annotations.*;\n");
        code.append("import java.util.List;\n");
        code.append("import org.apache.ibatis.annotations.Param;\n");
        code.append(IMPORT).append(beanClz.getName()).append(";\n");
        code.append(IMPORT).append(mapperPath).append(".provider.").append(beanClz.getSimpleName()).append("ProxyProvider;\n");
        // class
        code.append(MAPPER_ANT);
        String className = beanClz.getSimpleName() + "ProxyMapper";
        code.append(PUBLIC).append("interface ").append(className).append(" {\n");
        // methods
        for (MapperMethodCodeCache methodCodeCache : mapperMethodCodes) {
            code.append(methodCodeCache);
        }
        code.append("}\n");
        mapperCode.put(className, code.toString());
        return mapperCode;
    }

    private Map<String, String> buildProviderCode() {
        Map<String, String> providerCode = new HashMap<>(1);
        StringBuilder code = new StringBuilder();
        code.append(PACKAGE).append(mapperPath).append(".provider;\n");
        // imports
        code.append("import java.util.List;\n");
        code.append("import org.apache.ibatis.jdbc.SQL;\n");
        code.append("import ").append(StringUtils.class.getName()).append(";\n");
        code.append("import java.text.SimpleDateFormat;\n");
        code.append("import org.apache.ibatis.annotations.Param;\n");
        code.append(IMPORT).append(beanClz.getName()).append(";\n");
        // class
        String className = beanClz.getSimpleName() + "ProxyProvider";
        code.append(PUBLIC).append("class ").append(className).append(" {\n");
        // methods
        for (ProviderMethodCodeCache methodCodeCache : providerMethodCodes) {
            code.append(methodCodeCache);
        }
        code.append("}\n");
        providerCode.put(className, code.toString());
        return providerCode;
    }

    private void init() {
        DaoProxy daoProxyAnt = (DaoProxy) iDao.getAnnotation(DaoProxy.class);
        mapperPath = daoProxyAnt.output();
        implPath = iDao.getName().substring(iDao.getName().lastIndexOf("."));

        Type[] genericInterfaces = iDao.getGenericInterfaces();
        for (Type type : genericInterfaces) {
            String typeName = type.getTypeName();
            if (typeName.startsWith(Dao.class.getName()) && typeName.endsWith(">")) {
                try {
                    Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
                    if (actualTypeArguments.length == 0) {
                        throw new IDaoTypeParamException(iDao.getName() + "-需要规定泛型：泛型为实体类的类型\n");
                    }
                    beanClz = Class.forName(actualTypeArguments[0].getTypeName());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        checkMethods(iDao);
    }

    /**
     * 遍历dao接口的方法，将符合条件的方法加入须代理方法集
     * 0.Dao中的方法全部须代理
     * 1.注解包含@AutoSelect、@AutoInsert、@AutoUpdate、@AutoDelete中的仅一个
     * 且不同方法不能包含同一个上述注解的对象（注解类和参数均相同）
     * 2.带@AutoSelect、@AutoInsert、@AutoDelete注解的方法有且仅有一个参数，
     * 带@AutoUpdate的有两个参数
     *
     * @param iDao dao接口类对象
     */
    private void checkMethods(Class iDao) {
        Method[] methods = iDao.getMethods();
        for (Method method : methods) {
            Annotation[] annotations = method.getDeclaredAnnotations();
            if (annotations.length == 0) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            List<Annotation> ants = new ArrayList<>(1);
            for (Annotation annotation : annotations) {
                if (annotation instanceof AutoSelect || annotation instanceof AutoSelectOne
                        || annotation instanceof AutoInsert || annotation instanceof AutoDelete) {
                    if (parameterTypes.length != 1) {
                        throw new IDaoMethodArgumentException(iDao.getName() + "." + method.getName() + " - "
                                + annotation.annotationType().getSimpleName().substring("Auto".length()) + "方法必须有且仅有一个参数");
                    }
                    ants.add(annotation);
                } else if (annotation instanceof AutoUpdate) {
                    if (parameterTypes.length != 2) {
                        throw new IDaoMethodArgumentException(iDao.getName() + "." + method.getName() + " - "
                                + annotation.annotationType().getSimpleName().substring("Auto".length()) + "方法必须有且仅有两个参数");
                    }
                    ants.add(annotation);
                }
                if (ants.size() > 1) {
                    throw new IDaoMethodArgumentException(iDao.getName() + " - " + method.getName() + "方法有冲突注解");
                }
            }
            if (!ants.isEmpty()) {
                methodMap.put(method, ants);
            }
        }
        this.checkAnnotations(methodMap.values());

    }

    /**
     * 检查重复注解
     *
     * @param annotations 所有注解
     */
    private void checkAnnotations(Collection<List<Annotation>> annotations) {
        List<Annotation> allAnts = new ArrayList<>(5);
        for (List<Annotation> list : annotations) {
            list.forEach(ant -> {
                Class<? extends Annotation> annotationType = ant.annotationType();
                if (allAnts.contains(ant) && definedAnnotationList.contains(annotationType)) {
                    throw new IDaoMethodArgumentException(iDao.getName() + "接口中有重复注解" + ant);
                }
                allAnts.add(ant);
            });
        }
    }

    /**
     * 创建methodMap中的方法，包括Dao中的和用户Dao中的
     */
    private void createMethodsCode() {
        for (Map.Entry<Method, List<Annotation>> entry : methodMap.entrySet()) {
            Method method = entry.getKey();
            List<Annotation> annotations = entry.getValue();
            for (Annotation annotation : annotations) {
                if (definedAnnotationList.contains(annotation.annotationType())) {
                    createMethodCode(method, annotation);
                    break;
                }
            }
        }
    }

    /**
     * 根据注解生成方法代码
     *
     * @param method     要创建的方法类对象
     * @param annotation 增删改查
     */
    private void createMethodCode(Method method, Annotation annotation) {
        ImplMethodCodeCache implMethodCodeCache = new ImplMethodCodeCache(method, annotation);
        MapperMethodCodeCache mapperMethodCodeCache = new MapperMethodCodeCache(method, annotation);
        ProviderMethodCodeCache providerMethodCodeCache = new ProviderMethodCodeCache(method, annotation);
        implMethodCodes.add(implMethodCodeCache);
        mapperMethodCodes.add(mapperMethodCodeCache);
        providerMethodCodes.add(providerMethodCodeCache);
    }

    /**
     * Impl方法代码封装类
     * 封装：所属类，方法
     * 最后用于输出方法代码字符串
     */
    public class ImplMethodCodeCache {
        private String className;
        private Method method;
        private String mode;
        private StringBuilder code = new StringBuilder();

        ImplMethodCodeCache(Method method, Annotation ant) {
            this.className = method.getDeclaringClass().getSimpleName();
            this.mode = ant.annotationType().getSimpleName();
            this.method = method;
            init();
        }

        private void init() {
            String methodName = method.getName();
            Class<?> returnType = method.getReturnType();
            switch (mode) {
                case "AutoInsert":
                case "AutoDelete":
                    code.append("\t").append(OVER_RIDE);
                    code.append("\t").append(PUBLIC).append(returnType.getSimpleName()).append(" ").append(methodName).
                            append("(").append(beanClz.getSimpleName()).append(" var) {\n");
                    code.append("\t\t").append(RETURN).append("mapper.").append(methodName).append("(var);\n");
                    code.append("\t}\n");
                    break;
                case "AutoUpdate":
                    code.append("\t").append(OVER_RIDE);
                    code.append("\t").append(PUBLIC).append(returnType.getSimpleName()).append(" ").append(methodName)
                            .append("(").append(beanClz.getSimpleName()).append(" set, ").append(beanClz.getSimpleName()).append(" con) {\n");
                    code.append("\t\t").append(RETURN).append("mapper.").append(methodName).append("(set, con);\n");
                    code.append("\t}\n");
                    break;
                case "AutoSelect":
                    code.append("\t").append(OVER_RIDE);
                    code.append("\t").append(PUBLIC).append(returnType.getSimpleName()).append("<").append(beanClz.getSimpleName()).append("> ").
                            append(methodName).append("(").append(beanClz.getSimpleName()).append(" var) {\n");
                    code.append("\t\t").append(RETURN).append("mapper.").append(methodName).append("(var);\n");
                    code.append("\t}\n");
                    break;
                case "AutoSelectOne":
                    code.append("\t").append(OVER_RIDE);
                    code.append("\t").append(PUBLIC).append(beanClz.getSimpleName()).append(" ").append(methodName)
                            .append("(").append(beanClz.getSimpleName()).append(" var) {\n");
                    code.append("\t\t").append(RETURN).append("mapper.").append(methodName).append("(var);\n");
                    code.append("\t}\n");
                    break;
                default:
            }
        }

        @Override
        public String toString() {
            return code.toString();
        }

        String getClassName() {
            return className;
        }
    }

    /**
     * provider中的方法代码类
     */
    public class ProviderMethodCodeCache {
        private String className;
        private Method method;
        private String methodName;
        private Annotation ant;
        private String mode;
        private StringBuilder code = new StringBuilder();
        private String table;

        ProviderMethodCodeCache(Method method, Annotation ant) {
            this.className = method.getDeclaringClass().getSimpleName();
            this.ant = ant;
            this.mode = ant.annotationType().getSimpleName();
            this.method = method;
            init();
        }

        private void init() {
            methodName = method.getName();
            Table tableAnt = (Table) beanClz.getAnnotation(Table.class);
            if (tableAnt == null) {
                throw new IDaoTypeParamException(beanClz.getName() + "没有对应表的注解");
            }
            table = tableAnt.value();
            switch (mode) {
                case "AutoInsert":
                    writeInsertCode();
                    break;
                case "AutoDelete":
                    writeDeleteCode();
                    break;
                case "AutoUpdate":
                    writeUpdateCode();
                    break;
                case "AutoSelectOne":
                    writeSelectCode(true);
                    break;
                case "AutoSelect":
                    writeSelectCode();
                    break;
                default:
            }
        }

        private void writeUpdateCode() {
            code.append("\t").append(PUBLIC).append("String ").append(methodName).append("(@Param(\"set\") ").append(beanClz.getSimpleName())
                    .append(" set, @Param(\"con\") ").append(beanClz.getSimpleName()).append(" con) {\n");
            code.append("\t\tSQL sql = new SQL();\n");

            code.append("\t\tsql.UPDATE(\"").append(table).append("\");\n");
            List<Method> getterOrSetters = getterOrSetters(beanClz, "get");
            // SETS
            for (Method getter : getterOrSetters) {
                Class<?> getterReturnType = getter.getReturnType();
                String getterName = getter.getName();
                String fieldName;
                String columnName;
                String baseName = getterName.substring("get".length());
                if (beanClz.getAnnotation(NonCamelCase.class) == null) {
                    fieldName = StringUtils.toCamelCase(StringUtils.camelCase2_(baseName));
                    columnName = StringUtils.camelCase2_(fieldName);
                } else {
                    fieldName = baseName;
                    columnName = fieldName;
                }
                if (String.class.equals(getterReturnType)) {
                    Field field = null;
                    try {
                        field = getter.getDeclaringClass().getDeclaredField(fieldName);
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    SQLDatePattern datePattern = field != null ? field.getAnnotation(SQLDatePattern.class) : null;
                    if (datePattern == null) {
                        code.append("\t\tif (!StringUtils.isEmpty(set.").append(getterName).append("())) {\n");
                        code.append("\t\t\tsql.SET(\"").append(columnName).append(" = #{set.").append(fieldName).append("}\");\n");
                        code.append("\t\t}\n");
                    } else {
                        String pattern = datePattern.pattern();
                        String func = datePattern.func();
                        code.append("\t\tif (set.").append(getterName).append("() != null) {\n");
                        code.append("\t\t\tsql.SET(\"").append(columnName).append(" = ").append(func).append("(#{set.").append(fieldName)
                                .append("}, \\'").append(pattern).append("\\')\");\n");
                        code.append("\t\t}\n");
                    }
                } else if (Integer.class.equals(getterReturnType) || Double.class.equals(getterReturnType)
                        || Long.class.equals(getterReturnType) || Date.class.equals(getterReturnType)) {
                    code.append("\t\tif (set.").append(getterName).append("() != null) {\n");
                    code.append("\t\t\tsql.SET(\"").append(columnName).append(" = #{set.").append(fieldName).append("}\");\n");
                    code.append("\t\t}\n");
                }
            }
            // WHERE
            // 查看注解
            String[] other;
            String[] compare;
            other = ((AutoUpdate) ant).other();
            compare = ((AutoUpdate) ant).compare();
            Map<String, String> otherMap = new HashMap<>(3);
            for (String o : other) {
                String column = o.split("->")[0].trim().toUpperCase();
                String condition = o.split("->")[1];
                otherMap.put(column, condition);
            }
            Map<String, String> compareMap = new HashMap<>(2);
            for (String c : compare) {
                String column = c.split(":")[0].trim().toUpperCase();
                String operator = c.split(":")[1];
                compareMap.put(column, operator);
            }
            for (Method getter : getterOrSetters) {
                Class<?> getterReturnType = getter.getReturnType();
                String getterName = getter.getName();
                String fieldName;
                String columnName;
                String baseName = getterName.substring("get".length());
                if (beanClz.getAnnotation(NonCamelCase.class) == null) {
                    fieldName = StringUtils.toCamelCase(StringUtils.camelCase2_(baseName));
                    columnName = StringUtils.camelCase2_(fieldName);
                } else {
                    fieldName = baseName;
                    columnName = fieldName;
                }
                // 优先拼接注解中的条件
                if (otherMap.containsKey(columnName)) {
                    code.append("\t\tsql.WHERE(\"(").append(otherMap.get(columnName)).append(")\");\n");
                    continue;
                }
                if (compareMap.containsKey(columnName)) {
                    if ("LIKE".equals(compareMap.get(columnName).trim().toUpperCase())) {
                        code.append("\t\tsql.WHERE(\"").append(columnName).append(" ").append(compareMap.get(columnName))
                                .append(" '%\" + con.").append(getterName).append("() + \"%'\");\n");
                    } else {
                        code.append("\t\tsql.WHERE(\"").append(columnName).append(" ").append(compareMap.get(columnName))
                                .append(" #{con.").append(fieldName).append("}\");\n");
                    }
                    continue;
                }

                if (String.class.equals(getterReturnType)) {
                    Field field = null;
                    try {
                        field = getter.getDeclaringClass().getDeclaredField(fieldName);
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    SQLDatePattern datePattern = field != null ? field.getAnnotation(SQLDatePattern.class) : null;
                    if (datePattern == null) {
                        code.append("\t\tif (!StringUtils.isEmpty(con.").append(getterName).append("())) {\n");
                        code.append("\t\t\tsql.WHERE(\"").append(columnName).append(" = #{con.").append(fieldName).append("}\");\n");
                        code.append("\t\t}\n");
                    } else {
                        String func = datePattern.func();
                        String pattern = datePattern.pattern();
                        code.append("\t\tif (con.").append(getterName).append("() != null) {\n");
                        code.append("\t\t\tsql.WHERE(\"").append(columnName).append(" = ").append(func).append("(#{con.").append(fieldName)
                                .append("}, \\'").append(pattern).append("\\')\");\n");
                        code.append("\t\t}\n");
                    }
                } else if (Integer.class.equals(getterReturnType) || Double.class.equals(getterReturnType)
                        || Long.class.equals(getterReturnType) || Date.class.equals(getterReturnType)) {
                    code.append("\t\tif (con.").append(getterName).append("() != null) {\n");
                    code.append("\t\t\tsql.WHERE(\"").append(columnName).append(" = #{con.").append(fieldName).append("}\");\n");
                    code.append("\t\t}\n");
                }
            }
            tail();
        }

        private void writeSelectCode() {
            writeSelectCode(false);
        }

        /**
         * 生成select的代码
         * 优先查看注解
         *
         * @param single AutoSelectOne \ AutoSelect
         */
        private void writeSelectCode(boolean single) {
            code.append("\t").append(PUBLIC).append("String ").append(methodName).
                    append("(").append(beanClz.getSimpleName()).append(" var) {\n");
            code.append("\t\tSQL sql = new SQL();\n");
            code.append("\t\tsql.SELECT(\"*\").FROM(\"").append(table).append("\");\n");
            // 查看注解
            String[] other;
            String[] compare;
            String[] orderBy = {};
            boolean desc = false;
            if (single) {
                other = ((AutoSelectOne) ant).other();
                compare = ((AutoSelectOne) ant).compare();
            } else {
                other = ((AutoSelect) ant).other();
                compare = ((AutoSelect) ant).compare();
                orderBy = ((AutoSelect) ant).orderBy();
                if (orderBy.length == 0) {
                    orderBy = ((AutoSelect) ant).orderDescBy();
                    desc = true;
                }
            }
            Map<String, String> otherMap = new HashMap<>();
            for (String o : other) {
                String column = o.split("->")[0].trim().toUpperCase();
                String condition = o.split("->")[1];
                otherMap.put(column, condition);
            }
            Map<String, String> compareMap = new HashMap<>();
            for (String c : compare) {
                String column = c.split(":")[0].trim().toUpperCase();
                String operator = c.split(":")[1];
                compareMap.put(column, operator);
            }
            makeWhere(code, otherMap, compareMap);
            if (orderBy.length > 0) {
                code.append("\t\tsql.ORDER_BY(\"").append(Arrays.stream(orderBy).collect(Collectors.joining(",")));
                if (desc) {
                    code.append(" DESC\");\n");
                } else {
                    code.append("\");\n");
                }
            }
            tail();
        }

        private void writeInsertCode() {
            code.append("\t").append(PUBLIC).append("String ").append(methodName).
                    append("(").append(beanClz.getSimpleName()).append(" var) {\n");
            code.append("\t\tSQL sql = new SQL();\n");
            code.append("\t\tsql.INSERT_INTO(\"").append(table).append("\");\n");
            // 查看注解
            boolean idSeq = ((AutoInsert) ant).idSeq();

            List<Method> getters = this.getterOrSetters(beanClz, "get");
            for (Method getter : getters) {
                Class<?> getterReturnType = getter.getReturnType();
                String getterName = getter.getName();
                String fieldName;
                String columnName;
                String baseName = getterName.substring("get".length());
                if (beanClz.getAnnotation(NonCamelCase.class) == null) {
                    fieldName = StringUtils.toCamelCase(StringUtils.camelCase2_(baseName));
                    columnName = StringUtils.camelCase2_(fieldName);
                } else {
                    fieldName = baseName;
                    columnName = fieldName;
                }
                if (String.class.equals(getterReturnType)) {
                    Field field = null;
                    try {
                        field = getter.getDeclaringClass().getDeclaredField(fieldName);
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    SQLDatePattern datePattern = field != null ? field.getAnnotation(SQLDatePattern.class) : null;
                    if (datePattern == null) {
                        code.append("\t\tif (!StringUtils.isEmpty(var.").append(getterName).append("())) {\n");
                        code.append("\t\t\tsql.INTO_COLUMNS(\"").append(columnName).append("\").INTO_VALUES(\"#{").append(fieldName).append("}\");\n");
                        code.append("\t\t}\n");
                    } else {
                        String sqlPattern = datePattern.pattern();
                        String func = datePattern.func();
                        code.append("\t\tif (var.").append(getterName).append("() != null) {\n");
                        code.append("\t\t\tsql.INTO_COLUMNS(\"").append(columnName).append("\").INTO_VALUES(\"").append(func).append("(\\'\" + var.").
                                append(getterName).append("() + \"\\', \\'").append(sqlPattern).append("\\')\");\n");
                        code.append("\t\t}\n");
                    }
                } else if (Integer.class.equals(getterReturnType)) {
                    Field field = null;
                    try {
                        field = getter.getDeclaringClass().getDeclaredField(fieldName);
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    assert field != null;
                    ID idAnt = field.getAnnotation(ID.class);
                    if (idSeq && idAnt != null) {
                        code.append("\t\tsql.INTO_COLUMNS(\"").append(columnName).append("\").INTO_VALUES(\"").append(idAnt.seq()).append(".NEXTVAL\"");
                    } else {
                        code.append("\t\tif (var.").append(getterName).append("() != null) {\n");
                        code.append("\t\t\tsql.INTO_COLUMNS(\"").append(columnName).append("\").INTO_VALUES(\"#{").append(fieldName).append("}\");\n");
                        code.append("\t\t}\n");
                    }
                } else if (Double.class.equals(getterReturnType) || Long.class.equals(getterReturnType)
                        || Date.class.equals(getterReturnType)) {
                    code.append("\t\tif (var.").append(getterName).append("() != null) {\n");
                    code.append("\t\t\tsql.INTO_COLUMNS(\"").append(columnName).append("\").INTO_VALUES(\"#{").append(fieldName).append("}\");\n");
                    code.append("\t\t}\n");
                }
            }
            tail();
        }

        private void tail() {
            code.append("\t\tSystem.out.println(sql);\n");
            code.append("\t\t").append(RETURN).append("sql.toString();\n");
            code.append("\t}\n");
        }

        private void writeDeleteCode() {
            code.append("\t").append(PUBLIC).append("String ").append(methodName).
                    append("(").append(beanClz.getSimpleName()).append(" var) {\n");
            code.append("\t\tSQL sql = new SQL();\n");
            code.append("\t\tsql.DELETE_FROM(\"").append(table).append("\");\n");
            // 查看注解
            String[] other;
            String[] compare;
            other = ((AutoDelete) ant).other();
            compare = ((AutoDelete) ant).compare();
            Map<String, String> otherMap = new HashMap<>();
            for (String o : other) {
                String column = o.split("->")[0].trim().toUpperCase();
                String condition = o.split("->")[1];
                otherMap.put(column, condition);
            }
            Map<String, String> compareMap = new HashMap<>();
            for (String c : compare) {
                String column = c.split(":")[0].trim().toUpperCase();
                String operator = c.split(":")[1];
                compareMap.put(column, operator);
            }
            makeWhere(code, otherMap, compareMap);
            tail();
        }

        /**
         * 拼接WHERE条件
         * SELECT和DELETE中的拼接WHERE条件的方式完全一样
         *
         * @param code       继续写的provider代码
         * @param otherMap   @AutoSelect或@AutoDelete注解中的自定义条件
         * @param compareMap @AutoSelect或@AutoDelete注解中的比较条件
         */
        private void makeWhere(StringBuilder code, Map<String, String> otherMap, Map<String, String> compareMap) {
            List<Method> getters = this.getterOrSetters(beanClz, "get");
            for (Method getter : getters) {
                Class<?> getterReturnType = getter.getReturnType();
                String getterName = getter.getName();
                String fieldName;
                String columnName;
                String baseName = getterName.substring("get".length());
                if (beanClz.getAnnotation(NonCamelCase.class) == null) {
                    fieldName = StringUtils.toCamelCase(StringUtils.camelCase2_(baseName));
                    columnName = StringUtils.camelCase2_(fieldName);
                } else {
                    fieldName = baseName;
                    columnName = fieldName;
                }
                // 优先拼接注解中的条件
                if (otherMap.containsKey(columnName)) {
                    code.append("\t\tsql.WHERE(\"(").append(otherMap.get(columnName)).append(")\");\n");
                    continue;
                }
                if (compareMap.containsKey(columnName)) {
                    if ("LIKE".equals(compareMap.get(columnName).trim().toUpperCase())) {
                        code.append("\t\tsql.WHERE(\"").append(columnName).append(" ").append(compareMap.get(columnName))
                                .append(" '%\" + con.").append(getterName).append("() + \"%'\");\n");
                    } else {
                        code.append("\t\tsql.WHERE(\"").append(columnName).append(" ").append(compareMap.get(columnName))
                                .append(" #{").append(fieldName).append("}\");\n");
                    }
                    continue;
                }
                if (String.class.equals(getterReturnType)) {
                    Field field;
                    try {
                        field = getter.getDeclaringClass().getDeclaredField(fieldName);
                    } catch (NoSuchFieldException e) {
                        System.err.println(beanClz);
                        e.printStackTrace();
                        DaoFactory.hasException = true;
                        throw new DynamicCompileException(beanClz + "中字段可能驼峰规则有问题");
                    }
                    SQLDatePattern datePattern = field != null ? field.getAnnotation(SQLDatePattern.class) : null;
                    if (datePattern == null) {
                        code.append("\t\tif (!StringUtils.isEmpty(var.").append(getterName).append("())) {\n");
                        code.append("\t\t\tsql.WHERE(\"").append(columnName).append(" = #{").append(fieldName).append("}\");\n");
                        code.append("\t\t}\n");
                    } else {
                        String func = datePattern.func();
                        String pattern = datePattern.pattern();
                        code.append("\t\tif (var.").append(getterName).append("() != null) {\n");
                        code.append("\t\t\tsql.WHERE(\"").append(columnName).append(" = ").append(func).append("(#{").append(fieldName)
                                .append("}, \\'").append(pattern).append("\\')\");\n");
                        code.append("\t\t}\n");
                    }
                } else if (Integer.class.equals(getterReturnType) || Double.class.equals(getterReturnType)
                        || Long.class.equals(getterReturnType) || Date.class.equals(getterReturnType)) {
                    code.append("\t\tif (var.").append(getterName).append("() != null) {\n");
                    code.append("\t\t\tsql.WHERE(\"").append(columnName).append(" = #{").append(fieldName).append("}\");\n");
                    code.append("\t\t}\n");
                }
            }
        }

        @Override
        public String toString() {
            return code.toString();
        }

        String getClassName() {
            return className;
        }

        private List<Method> getterOrSetters(Class beanClz, @NotNull String getOrSet) {
            Method[] methods = beanClz.getDeclaredMethods();
            List<Method> list = new ArrayList<>(methods.length / 2);
            for (Method m : methods) {
                if (m.getName().startsWith(getOrSet)) {
                    list.add(m);
                }
            }
            return list;
        }
    }

    /**
     * Mapper方法代码类
     */
    public class MapperMethodCodeCache {
        private String className;
        private Method method;
        private String mode;
        private StringBuilder code = new StringBuilder();

        MapperMethodCodeCache(Method method, Annotation ant) {
            this.className = method.getDeclaringClass().getSimpleName();
            this.mode = ant.annotationType().getSimpleName();
            this.method = method;
            init();
        }

        private void init() {
            String methodName = method.getName();
            Class<?> returnType = method.getReturnType();
            switch (mode) {
                case "AutoInsert":
                    code.append("\t").append(INSERT_PROVIDER).append("(type = ").append(beanClz.getSimpleName()).append("Proxy").append(PROVIDER).append(CLASS_SUFFIX).
                            append(", method = \"").append(methodName).append("\")\n");
                    code.append("\t").append(returnType.getSimpleName()).append(" ").append(methodName).
                            append("(").append(beanClz.getSimpleName()).append(" var);\n");
                    break;
                case "AutoDelete":
                    code.append("\t").append(DELETE_PROVIDER).append("(type = ").append(beanClz.getSimpleName()).append("Proxy").append(PROVIDER).append(CLASS_SUFFIX).
                            append(", method = \"").append(methodName).append("\")\n");
                    code.append("\t").append(returnType.getSimpleName()).append(" ").append(methodName).
                            append("(").append(beanClz.getSimpleName()).append(" var);\n");
                    break;
                case "AutoUpdate":
                    code.append("\t").append(UPDATE_PROVIDER).append("(type = ").append(beanClz.getSimpleName()).append("Proxy").append(PROVIDER).append(CLASS_SUFFIX).
                            append(", method = \"").append(methodName).append("\")\n");
                    code.append("\t").append(returnType.getSimpleName()).append(" ").append(methodName).
                            append("(@Param(\"set\") ").append(beanClz.getSimpleName()).append(" set, @Param(\"con\") ").append(beanClz.getSimpleName()).append(" con);\n");
                    break;
                case "AutoSelect":
                    code.append("\t").append(SELECT_PROVIDER).append("(type = ").append(beanClz.getSimpleName())
                            .append("Proxy").append(PROVIDER).append(CLASS_SUFFIX).append(", method = \"").append(methodName).append("\")\n");
                    code.append("\t").append(returnType.getSimpleName()).append("<").append(beanClz.getSimpleName()).append("> ")
                            .append(methodName).append("(").append(beanClz.getSimpleName()).append(" var);\n");

                    break;
                case "AutoSelectOne":
                    code.append("\t").append(SELECT_PROVIDER).append("(type = ").append(beanClz.getSimpleName())
                            .append("Proxy").append(PROVIDER).append(CLASS_SUFFIX).append(", method = \"").append(methodName).append("\")\n");
                    code.append("\t").append(beanClz.getSimpleName()).append(" ").append(methodName).append("(").append(beanClz.getSimpleName()).append(" var);\n");
                default:
            }
        }

        @Override
        public String toString() {
            return code.toString();
        }

        String getClassName() {
            return className;
        }
    }
}
