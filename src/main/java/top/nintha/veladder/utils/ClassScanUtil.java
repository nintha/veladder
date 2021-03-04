package top.nintha.veladder.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
public class ClassScanUtil {
    private static final String CLASS_FILE_SUFFIX = ".class";

    /**
     * 扫描指定包路径下所有包含指定注解的类
     *
     * @param packageName 包名
     * @param annotation  指定的注解
     * @return Set
     */
    public static <A extends Annotation> Set<Class<?>> scanByAnnotation(String packageName, Class<A> annotation) {
        final Set<Class<?>> classSet = new HashSet<>();
        String packageDirName = packageName.replace('.', '/');
        final Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
        } catch (IOException e) {
            log.warn("[ClassScan] failed to get package resources, packageDirName={}", packageDirName, e);
            return classSet;
        }
        while (dirs.hasMoreElements()) {
            // 获取下一个元素
            URL url = dirs.nextElement();
            // 得到协议的名称
            String protocol = url.getProtocol();
            Set<String> classNames = Collections.emptySet();
            // 如果是以文件的形式保存在服务器上
            if ("file".equals(protocol)) {
                classNames = scanFile(url, packageName);
            }
            // 如果是jar包文件
            else if ("jar".equals(protocol)) {
                classNames = scanJar(url, packageName, packageDirName);
            }
            for (String cls : classNames) {
                Class<?> clazz = null;
                try {
                    clazz = Class.forName(cls);
                } catch (ClassNotFoundException e) {
                    log.warn("[ClassScan] failed to load class '{}'", cls, e);
                }
                if (clazz != null && null != clazz.getAnnotation(annotation)) {
                    classSet.add(clazz);
                }
            }

        }
        return classSet;
    }

    private static Set<String> scanFile(URL url, String packageName) {
        // 获取包的物理路径
        String filePath = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8);
        // 以文件的方式扫描整个包下的文件 并添加到集合中
        File dir = new File(filePath);
        Set<String> packages = new HashSet<>();
        fetchFilePackages(dir, packageName, packages);
        return packages;
    }

    private static Set<String> scanJar(URL url, String packageName, String packageDirName) {
        JarFile jar;
        try {
            JarURLConnection urlConnection = (JarURLConnection) url.openConnection();
            jar = urlConnection.getJarFile();
        } catch (IOException e) {
            log.warn("[ClassScan] failed to resolve jar entry", e);
            return Set.of();
        }

        Set<String> classNames = new HashSet<>();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            // 如果是以/开头的
            if (name.charAt(0) == '/') {
                // 获取后面的字符串
                name = name.substring(1);
            }
            // 如果前半部分和定义的包名相同
            if (name.startsWith(packageDirName)) {
                int idx = name.lastIndexOf('/');
                // 如果以"/"结尾 是一个包
                if (idx != -1) {
                    // 获取包名 把"/"替换成"."
                    packageName = name.substring(0, idx).replace('/', '.');
                }
                // 如果可以迭代下去 并且是一个包
                // 如果是一个.class文件 而且不是目录
                if (name.endsWith(".class") && !entry.isDirectory()) {
                    // 去掉后面的".class" 获取真正的类名
                    String className = name.substring(packageName.length() + 1, name.length() - 6);
                    classNames.add(packageName + '.' + className);
                }
            }
        }
        return classNames;
    }

    /**
     * 查找所有的文件
     */
    private static void fetchFilePackages(File dir, String packageName, Set<String> packages) {
        if (dir.isDirectory()) {
            for (File f : Objects.requireNonNull(dir.listFiles())) {
                fetchFilePackages(f, String.format("%s.%s", packageName, f.getName()), packages);
            }
        } else if (packageName.endsWith(CLASS_FILE_SUFFIX)) {
            packages.add(packageName.substring(0, packageName.length() - CLASS_FILE_SUFFIX.length()));
        }
    }

}
