package com.wwx.compiler;


import com.wwx.compiler.annotation.DaoProxy;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.wwx.compiler.Constants.PROVIDER;
import static com.wwx.compiler.DaoCodeBuilder.IMPL;
import static com.wwx.compiler.DaoCodeBuilder.MAPPER;

public class DaoFactory {
    static String idaoPkg;
    private static final String CLASS_ROOT_PATH = DaoFactory.class.getClassLoader().getResource("").getPath();
    private static final int THREADS_NUM = 4;
    static boolean hasException;
    static Throwable e;
    private static AtomicInteger totalDao = new AtomicInteger();
    private static AtomicInteger compileTaskNum;

    public static void createDaoImpls(String daoPkg, Class mainClass) {
        String name = mainClass.getName();
        String buildClassPath = name.substring(0, name.lastIndexOf('.'));
        createDaoImpls(daoPkg, buildClassPath);
    }

    private static void createDaoImpls(String daoPkg, String buildClassPath) {
        idaoPkg = daoPkg;
        long timeStart = System.currentTimeMillis();

        // 扫描dao包下的接口，生成dao集合
        ComponentScanner componentScanner = new ComponentScanner(buildClassPath);
        List<Class<?>> daoSet = componentScanner.findComponents(daoPkg);
        List<Class<?>> daoImpls = componentScanner.findComponents(daoPkg + ".impl");
        distinct(daoSet, daoImpls);

		/*
        遍历dao集合
			对每一个接口生成：
                1.DaoImpl代码
                2.Mapper代码
                3.Provider代码
			编译这些代码到接口注解的包路径下（编译到项目的类根目录即可，类的位置会根据package自动放置）
		 */
        int daoNum = daoSet.size();
        if (daoNum <= 0) {
            System.out.println("没有需要生成的Dao");
            return;
        }
        ExecutorService executorService = Executors.newScheduledThreadPool(THREADS_NUM, new CaughtThreadFactory());
        int daoPerTask = daoNum / THREADS_NUM == 0 ? 1 : daoNum / THREADS_NUM;
        compileTaskNum = new AtomicInteger((int) Math.ceil(1.0 * daoNum / daoPerTask));
        System.out.println("编译任务数量：" + compileTaskNum.get());
        for (int i = 0; i < daoNum; i += daoPerTask) {
            int startIndex = i;
            int endIndex = startIndex + daoPerTask - 1 > daoNum ? daoNum - 1 : startIndex + daoPerTask - 1;
            System.out.println(startIndex + " - " + endIndex);
            executorService.submit(() -> {
                try {
                    buildClasses(daoSet, startIndex, endIndex);
                    compileTaskNum.decrementAndGet();
                } catch (Exception e) {
                    hasException = true;
                    DaoFactory.e = e;
                }
            });
        }
        while (compileTaskNum.get() > 0) {
            if (hasException) {
                throw new RuntimeException(e);
            }
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long timeEnd = System.currentTimeMillis();
        System.out.println("生成daos用时：" + (timeEnd - timeStart) / 1000 + "秒");
    }

    /**
     * 去重，从给定类对象集合中去掉指定类对象集合中的类对象
     * 根据类名和方法数量比较
     *
     * @param daoSet   给定类对象
     * @param daoImpls 指定类对象
     */
    private static void distinct(List<Class<?>> daoSet, List<Class<?>> daoImpls) {
        for (int i = 0; i < daoSet.size(); i++) {
            Class<?> dao = daoSet.get(i);
            for (Class<?> daoImpl : daoImpls) {
                String daoImplName = daoImpl.getSimpleName();
                String daoName = dao.getSimpleName();
                boolean toExcept = false;
                if (daoName.endsWith("ProxyImpl")) {
                    toExcept = true;
                } else if (daoImplName.startsWith(daoName)) {
                    List<Method> implMethods = Arrays.asList(daoImpl.getDeclaredMethods());
                    List<String> implMethodNames = implMethods.stream().map(Method::getName).distinct().sorted().collect(Collectors.toList());
                    List<Method> daoMethods = Arrays.asList(dao.getMethods());
                    List<String> daoMethodNames = daoMethods.stream().map(Method::getName).sorted().collect(Collectors.toList());
                    if (implMethodNames.equals(daoMethodNames)) {
                        toExcept = true;
                    }
                }
                if (toExcept) {
                    daoSet.remove(i--);
                    break;
                }
            }
        }
    }

    private static void buildClasses(List<Class<?>> daoSet, int startIndex, int endIndex) {
        for (int i = startIndex; i <= endIndex; i++) {
            Class<?> dao = daoSet.get(i);

            DaoCodeBuilder daoCodeBuilder = new DaoCodeBuilder(dao);
            DaoProxy daoProxyAnt = dao.getAnnotation(DaoProxy.class);
            if (daoProxyAnt == null) {
                System.err.println(dao.getSimpleName() + "没有配置编译输出路径，将不会被自动代理");
                continue;
            }
            Map<String, Map<String, String>> codeMap = daoCodeBuilder.buildCode();

            DynamicCompiler dynamicCompiler = new DynamicCompiler();
            dynamicCompiler.compile(codeMap.get(PROVIDER), CLASS_ROOT_PATH);
            dynamicCompiler.compile(codeMap.get(MAPPER), CLASS_ROOT_PATH);
            dynamicCompiler.compile(codeMap.get(IMPL), CLASS_ROOT_PATH);

            totalDao.incrementAndGet();
        }
    }

    private static class CaughtThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(r);
            t.setUncaughtExceptionHandler((thread, throwable) -> {
                throwable.printStackTrace();
                DaoFactory.e = throwable;
                hasException = true;
            });
            t.setDaemon(true);
            return t;
        }
    }
}
