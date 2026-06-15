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

package nextflow.pak

import java.nio.file.Files
import java.nio.file.Paths

import nextflow.SysEnv
import nextflow.util.Duration
import spock.lang.Specification

/**
 *
 * @author Evan Floden
 */
class PakCacheTest extends Specification {

    def setupSpec() {
        SysEnv.push([:])
    }

    def cleanupSpec() {
        SysEnv.pop()
    }

    def 'should create pak env prefix path for a string env' () {
        given:
        def ENV = 'dplyr ggplot2'
        def cache = Spy(PakCache)
        def BASE = Paths.get('/pak/envs')

        when:
        def prefix = cache.pakPrefixPath(ENV)
        then:
        1 * cache.getCacheDir() >> BASE
        prefix.toString().startsWith('/pak/envs/env-')
    }

    def 'should create the correct pak command for package list' () {
        given:
        def folder = Files.createTempDirectory('test')
        def prefixPath = folder.resolve('env-x')
        def cache = Spy(PakCache)
        cache.@installOptions = null
        cache.@createTimeout = Duration.of('20min')

        when:
        cache.createLocalPakEnv0('dplyr', prefixPath)
        then:
        1 * cache.runCommand({ String c ->
            c.contains('Rscript') && c.contains('pak::pkg_install') && c.contains('"dplyr"')
        }) >> 0

        cleanup:
        folder?.deleteDir()
    }

    def 'should create the correct pak command for an renv.lock file' () {
        given:
        def folder = Files.createTempDirectory('test')
        def lock = folder.resolve('renv.lock')
        lock.text = '{"R":{"Version":"4.3.0"}}'
        def prefixPath = folder.resolve('env-x')
        def cache = Spy(PakCache)
        cache.@installOptions = null
        cache.@createTimeout = Duration.of('20min')

        when:
        cache.createLocalPakEnv0(lock.toString(), prefixPath)
        then:
        1 * cache.runCommand({ String c ->
            c.contains('pak::lockfile_install') && c.contains('renv.lock')
        }) >> 0

        cleanup:
        folder?.deleteDir()
    }

    def 'should create the correct pak command for a DESCRIPTION file' () {
        given:
        def folder = Files.createTempDirectory('test')
        def desc = folder.resolve('DESCRIPTION')
        desc.text = 'Package: demo\nImports: dplyr'
        def prefixPath = folder.resolve('env-x')
        def cache = Spy(PakCache)
        cache.@installOptions = null
        cache.@createTimeout = Duration.of('20min')

        when:
        cache.createLocalPakEnv0(desc.toString(), prefixPath)
        then:
        1 * cache.runCommand({ String c ->
            c.contains('pak::local_install_deps')
        }) >> 0

        cleanup:
        folder?.deleteDir()
    }

}
