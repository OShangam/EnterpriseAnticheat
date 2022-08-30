package dev.brighten.ac.utils;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.annotation.Init;
import dev.brighten.ac.utils.reflections.Reflections;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import org.bukkit.Bukkit;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This is copied from somewhere, can't remember from where though. Modified.
 * */
public class ClassScanner {
    private static final PathMatcher CLASS_FILE = create("glob:*.class");
    private static final PathMatcher ARCHIVE = create("glob:*.{jar}");

    //TODO Get check classes too
    public static Set<WrappedClass> getClasses(Class<? extends Annotation> annotationClass,
                                               String packageName) {
        Map<String, byte[]> map = Anticheat.INSTANCE.getStuffs();
        Map<String, byte[]> loadedClasses = Anticheat.INSTANCE.getLoadedClasses();
        Set<WrappedClass> toReturn = new HashSet<>();

        for (Map.Entry<String, byte[]> entry : map.entrySet()) {
            boolean startsWith = entry.getKey().startsWith(packageName);
            boolean hasAnnotation = findClass(new ByteArrayInputStream(entry.getValue()), annotationClass) != null;

            if(startsWith && hasAnnotation) {
                toReturn.add(Reflections.getClass(entry.getKey()));
            }
        }

        for (Map.Entry<String, byte[]> entry : loadedClasses.entrySet()) {
            boolean startsWith = entry.getKey().startsWith(packageName);
            boolean hasAnnotation = findClass(new ByteArrayInputStream(entry.getValue()), annotationClass) != null;

            if(startsWith && hasAnnotation) {
                toReturn.add(Reflections.getClass(entry.getKey()));
            }
        }
        return toReturn;
    }

    public static Set<String> getNames() {
        Map<String, byte[]> map = Anticheat.INSTANCE.getStuffs();

        Set<String> nameSet = new HashSet<>();

        for (String loadedClass : Anticheat.INSTANCE.getLoadedClasses().keySet()) {
            InputStream stream = new ByteArrayInputStream(Anticheat.INSTANCE.getLoadedClasses().get(loadedClass));

            if(findClass(stream, Init.class) != null) {
                nameSet.add(loadedClass);
            }
        }

        map.keySet().stream().filter(n -> !n.endsWith(".yml")
                && !n.endsWith(".xml") && !n.endsWith(".") && !n.endsWith(".properties")
                && !n.contains("dev.brighten.ac.packet")
                && Character.isLetterOrDigit(n.charAt(n.length() - 1))
                && findClass(map.get(n), Init.class) != null).forEach(nameSet::add);

        return nameSet;
    }

    public static String findClass(byte[] array, Class<? extends Annotation> annotationClass) {
        try {
            ClassReader reader = new ClassReader(array);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            String className = classNode.name.replace('/', '.');

            final String anName = annotationClass.getName().replace(".", "/");
            if (classNode.visibleAnnotations != null) {
                for (Object node : classNode.visibleAnnotations) {
                    AnnotationNode annotation = (AnnotationNode) node;
                    if (annotation.desc
                            .equals("L" + anName + ";")) {
                        return className;
                    }
                }
            }
        } catch (Exception e) {
            //Bukkit.getLogger().info("Failed to scan");
            //e.printStackTrace();
        }
        return null;
    }

    public static String findClass(InputStream stream, Class<? extends Annotation> annotationClass) {
        try {
            ClassReader reader = new ClassReader(stream);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            String className = classNode.name.replace('/', '.');

            final String anName = annotationClass.getName().replace(".", "/");
            if (classNode.visibleAnnotations != null) {
                for (Object node : classNode.visibleAnnotations) {
                    AnnotationNode annotation = (AnnotationNode) node;
                    if (annotation.desc
                            .equals("L" + anName + ";")) {
                        return className;
                    }
                }
            }
        } catch (Exception e) {
            //Bukkit.getLogger().info("Failed to scan");
            //e.printStackTrace();
        }
        return null;
    }

    private static <E> HashSet<E> newHashSet(E... elements) {
        HashSet<E> set = new HashSet<>();
        Collections.addAll(set, elements);
        return set;
    }


    private static void scanZip(String file, Path path, Set<String> plugins) {
        if (!ARCHIVE.matches(path.getFileName())) {
            return;
        }

        try (ZipFile zip = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }

                try (InputStream in = zip.getInputStream(entry)) {
                    String plugin = findPlugin(file, in);
                    if (plugin != null) {
                        plugins.add(plugin);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String findPlugin(String file, InputStream in) {
        try {
            ClassReader reader = new ClassReader(in);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            String className = classNode.name.replace('/', '.');
            if (classNode.visibleAnnotations != null) {
                for (Object node : classNode.visibleAnnotations) {
                    AnnotationNode annotation = (AnnotationNode) node;
                    if ((file == null && annotation.desc
                            .equals("L" + Init.class.getName().replace(".", "/") + ";"))
                            || (file != null && annotation.desc
                            .equals("L" + file.replace(".", "/") + ";")))
                        return className;
                }
            }
            if (classNode.superName != null && (classNode.superName.equals(file))) return className;
        } catch (Exception e) {
            //System.out.println("Failed to scan: " + in.toString());
        }
        return null;
    }

    public static String findClasses(String file, InputStream in) {
        try {
            ClassReader reader = new ClassReader(in);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            return classNode.name.replace('/', '.');
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to scan: " + in.toString());
        }
        return null;
    }

    public static PathMatcher create(String pattern) {
        return FileSystems.getDefault().getPathMatcher(pattern);
    }
}