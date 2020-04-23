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

import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.internal.initialization.ClassLoaderScope;
import org.gradle.configuration.BuildOperationScriptPlugin;
import org.gradle.configuration.ScriptPlugin;
import org.gradle.configuration.internal.UserCodeApplicationContext;
import org.gradle.groovy.scripts.ScriptSource;
import org.gradle.internal.operations.BuildOperationExecutor;

import java.util.List;

/**
 * Selects a {@link ScriptPluginFactory} suitable for handling a given build script based
 * on its file name. Selects a backing {@link DslLanguageScriptPluginFactory} based on the
 * extension of the file, using the first fallback if there is no match.
 * This approach allows users to name build scripts with a suffix of choice, e.g. "build.groovy"
 * or "my.build" instead of the typical "build.gradle" while preserving default behaviour which
 * is to fallback to Groovy support.
 *
 * This factory wraps each {@link ScriptPlugin} implementation in a {@link BuildOperationScriptPlugin}.
 *
 * @since 2.14
 */
public class ScriptPluginFactorySelector implements ScriptPluginFactory {
    private final List<DslLanguageScriptPluginFactory> scriptPluginFactories;
    private final BuildOperationExecutor buildOperationExecutor;
    private final UserCodeApplicationContext userCodeApplicationContext;

    public ScriptPluginFactorySelector(List<DslLanguageScriptPluginFactory> scriptPluginFactories,
                                       BuildOperationExecutor buildOperationExecutor,
                                       UserCodeApplicationContext userCodeApplicationContext) {
        this.scriptPluginFactories = scriptPluginFactories;
        this.buildOperationExecutor = buildOperationExecutor;
        this.userCodeApplicationContext = userCodeApplicationContext;
    }

    @Override
    public ScriptPlugin create(ScriptSource scriptSource, ScriptHandler scriptHandler, ClassLoaderScope targetScope,
                               ClassLoaderScope baseScope, boolean topLevelScript) {
        ScriptPlugin scriptPlugin = scriptPluginFactoryFor(scriptSource.getFileName())
            .create(scriptSource, scriptHandler, targetScope, baseScope, topLevelScript);
        return new BuildOperationScriptPlugin(scriptPlugin, buildOperationExecutor, userCodeApplicationContext);
    }

    private DslLanguageScriptPluginFactory scriptPluginFactoryFor(String fileName) {
        for (DslLanguageScriptPluginFactory scriptPluginFactory : scriptPluginFactories) {
            if (fileName.endsWith(scriptPluginFactory.getExtension())) {
                return scriptPluginFactory;
            }
        }
        return getFallbackFactory();
    }

    private DslLanguageScriptPluginFactory getFallbackFactory() {
        for (DslLanguageScriptPluginFactory scriptPluginFactory : scriptPluginFactories) {
            if (scriptPluginFactory.isFallback()) {
                return scriptPluginFactory;
            }
        }
        throw new IllegalArgumentException("No fallback script factory in: " + scriptPluginFactories);
    }
}
