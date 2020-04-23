/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.internal.scripts;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.gradle.internal.FileUtils.hasExtension;

public class DefaultScriptFileResolver implements ScriptFileResolver {
    private final List<String> extensions;

    public DefaultScriptFileResolver() {
        extensions = ImmutableList.of(".gradle", ".gradle.kts");
    }

    @Override
    public File resolveScriptFile(File dir, String basename) {
        for (String extension : extensions) {
            File candidate = new File(dir, basename + extension);
            if (candidate.isFile()) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public List<File> findScriptsIn(File dir) {
        File[] candidates = dir.listFiles();
        if (candidates == null || candidates.length == 0) {
            return emptyList();
        }
        List<File> found = new ArrayList<File>(candidates.length);
        for (File candidate : candidates) {
            if (candidate.isFile() && hasScriptExtension(candidate)) {
                found.add(candidate);
            }
        }
        return found;
    }

    private boolean hasScriptExtension(File file) {
        for (String extension : extensions) {
            if (hasExtension(file, extension)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> scriptingLanguageExtensions(List<DslLanguageScriptPluginFactory> scriptPluginFactories) {
        // Ensure a fixed order, with the fallback ordered first
        List<DslLanguageScriptPluginFactory> orderedFactories = new ArrayList<>(scriptPluginFactories);
        orderedFactories.sort((a, b) -> {
            if (a.isFallback()) {
                return -1;
            }
            if (b.isFallback()) {
                return 1;
            }
            return a.getExtension().compareTo(b.getExtension());
        });
        ImmutableList.Builder<String> extensions = ImmutableList.builderWithExpectedSize(orderedFactories.size());
        for (DslLanguageScriptPluginFactory scriptPluginFactory : orderedFactories) {
            extensions.add(scriptPluginFactory.getExtension());
        }
        return extensions.build();
    }
}
