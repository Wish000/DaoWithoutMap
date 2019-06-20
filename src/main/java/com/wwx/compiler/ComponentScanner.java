package com.wwx.compiler;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

class ComponentScanner {
    static final String CLASS_SUFFIX = ".class";

    private String buildClassPath;

    ComponentScanner(String buildClassPath) {
        this.buildClassPath = buildClassPath.replace('.', '\\');
    }
    private static final Pattern INNER_PATTERN =
            Pattern.compile("\\$(\\d+).", Pattern.CASE_INSENSITIVE);

    List<Class<?>> findComponents(String pkg) {
        List<Class<?>> classSet = new ArrayList<>(16);
        if (pkg.endsWith(".")) {
            pkg = pkg.substring(0, pkg.length() - 1);
        }
        String pkgPath = pkg.replaceAll("\\.", "/");
        Enumeration<URL> urls = findAllClassPathResources(pkgPath);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                String fileName;
                try {
                    fileName = URLDecoder.decode(url.getFile(), "UTF-8");
                    File file = new File(fileName);
                    if (file.isDirectory()) {
                        this.parseClassFile(file, classSet);
                    } else {
                        throw new IllegalArgumentException("包路径对应的不是文件夹");
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return classSet;
    }

    private void parseClassFile(File file, List<Class<?>> classSet) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            assert files != null;
            for (File f : files) {
                parseClassFile(f, classSet);
            }
        } else if (!"Dao.class".equals(file.getName()) && file.getName().endsWith(CLASS_SUFFIX)) {
            String path = file.getPath();
            String name = path.substring(path.indexOf(buildClassPath))
                    .replace(File.separator, ".");

            addToClassSet(name, classSet);
        }
    }

    private void addToClassSet(String name, List<Class<?>> classSet) {
        // 过滤掉匿名内部类
        /*if (INNER_PATTERN.matcher(name).find()) {
            return;
        }*/

        String className = name.substring(0, name.length() - CLASS_SUFFIX.length());
        try {
            Class<?> clz = Class.forName(className);
            classSet.add(clz);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Enumeration<URL> findAllClassPathResources(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        try {
            return ComponentScanner.class.getClassLoader().getResources(path);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
