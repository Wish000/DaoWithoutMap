package com.wwx.compiler;

import com.wwx.compiler.exception.DynamicCompileException;
import com.wwx.compiler.util.StringUtils;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class DynamicCompiler {

    void compile(Map<String, String> classCode, String output) {
        String name = classCode.keySet().iterator().next();
        String code = classCode.get(name);
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(code)) {
            throw new DynamicCompileException("类名或代码为空");
        }
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        writer.println(code);
        JavaFileObject fileObject = new JavaSourceObject(name, out.toString());
        List<String> options = new ArrayList<>(4);
        options.add("-encoding");
        options.add("UTF-8");
        options.add("-d");
        options.add(output);
        JavaCompiler.CompilationTask task = javaCompiler.getTask(null, null, null,
                options, null, Collections.singletonList(fileObject));
        Boolean isSuccess = task.call();
        if (isSuccess) {
            System.out.println(name + "编译成功");
        }

    }

    private class JavaSourceObject extends SimpleJavaFileObject {
        private final String code;

        JavaSourceObject(String name, String code) {
            super(URI.create(name + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return code;
        }
    }

}
