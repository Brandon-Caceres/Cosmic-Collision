/*
 * Copyright 2020 damios
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//Note, the above license and copyright applies to this file only.

package com.cosmic.collision.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3NativesLoader;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;

public class StartupHelper {
    private static final String JVM_RESTARTED_ARG = "jvmIsRestarted";
    private StartupHelper() { throw new UnsupportedOperationException(); }

    public static boolean startNewJvmIfRequired(boolean redirectOutput) {
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("mac")) {
            if (osName.contains("windows")) {
                try { Lwjgl3NativesLoader.load(); } catch (Throwable ignored) {}
            }
            return false;
        }
        if ("true".equals(System.getProperty(JVM_RESTARTED_ARG))) return false;
        if (ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-XstartOnFirstThread"))
            return false;
        try {
            ArrayList<String> jvmArgs = new ArrayList<>();
            String sep = System.getProperty("file.separator");
            String javaExecPath = System.getProperty("java.home") + sep + "bin" + sep + "java";
            jvmArgs.add(javaExecPath);
            jvmArgs.add("-XstartOnFirstThread");
            if (redirectOutput) jvmArgs.add("-D" + JVM_RESTARTED_ARG + "=true");
            jvmArgs.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
            jvmArgs.add("-cp");
            jvmArgs.add(System.getProperty("java.class.path"));
            jvmArgs.add(System.getProperty("sun.java.command"));
            ProcessBuilder pb = new ProcessBuilder(jvmArgs);
            if (redirectOutput) {
                pb.redirectErrorStream(true);
                Process p = pb.start();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line; while ((line = br.readLine()) != null) System.out.println(line);
                }
                p.waitFor();
            } else {
                pb.start();
            }
            return true;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public static boolean startNewJvmIfRequired() { return startNewJvmIfRequired(true); }
}