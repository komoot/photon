package org.elasticsearch.bootstrap;

import java.net.URL;
import java.util.Set;
import com.google.common.collect.ImmutableSet;

public class JarHellNo {
    private JarHellNo() {}
    public static void checkJarHell() throws Exception {}
//    public static void checkJarHell(URL urls[]) throws Exception {}
    public static void checkJarHell(Set<URL> urls) throws Exception {}
    public static void checkVersionFormat(String targetVersion) {}
    public static void checkJavaVersion(String resource, String targetVersion) {}
//  5.6 change  public static URL[] parseClassPath() {return new URL[]{};}
    public static Set<URL> parseClassPath() {return ImmutableSet.of();}
}