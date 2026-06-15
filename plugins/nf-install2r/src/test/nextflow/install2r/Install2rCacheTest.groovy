/*
 * Copyright 2013-2026, Seqera Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nextflow.install2r

import java.nio.file.Files
import java.nio.file.Paths

import nextflow.SysEnv
import nextflow.util.Duration
import spock.lang.Specification

/**
 *
 * @author Evan Floden
 */
class Install2rCacheTest extends Specification {

    def setupSpec() {
        SysEnv.push([:])
    }

    def cleanupSpec() {
        SysEnv.pop()
    }

    def 'should create install2r env prefix path for a string env' () {
        given:
        def ENV = 'dplyr ggplot2'
        def cache = Spy(Install2rCache)
        def BASE = Paths.get('/i2r/envs')

        when:
        def prefix = cache.install2rPrefixPath(ENV)
        then:
        1 * cache.getCacheDir() >> BASE
        prefix.toString().startsWith('/i2r/envs/env-')
    }

    def 'should create the correct install2.r command for package list' () {
        given:
        def folder = Files.createTempDirectory('test')
        def prefixPath = folder.resolve('env-x')
        def cache = Spy(Install2rCache)
        cache.@installOptions = null
        cache.@createTimeout = Duration.of('20min')

        when:
        cache.createLocalInstall2rEnv0('dplyr ggplot2', prefixPath)
        then:
        1 * cache.runCommand({ String c ->
            c.contains('install2.r') && c.contains('-l') && c.contains('dplyr ggplot2')
        }) >> 0

        cleanup:
        folder?.deleteDir()
    }

}
