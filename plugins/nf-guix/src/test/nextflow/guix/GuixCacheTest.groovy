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

package nextflow.guix

import java.nio.file.Files
import java.nio.file.Paths

import nextflow.SysEnv
import nextflow.util.Duration
import spock.lang.Specification

/**
 *
 * @author Evan Floden
 */
class GuixCacheTest extends Specification {

    def setupSpec() {
        SysEnv.push([:])
    }

    def cleanupSpec() {
        SysEnv.pop()
    }

    def 'should create guix env prefix path for a string env' () {
        given:
        def ENV = 'bwa samtools'
        def cache = Spy(GuixCache)
        def BASE = Paths.get('/guix/envs')

        when:
        def prefix = cache.guixPrefixPath(ENV)
        then:
        1 * cache.getCacheDir() >> BASE
        prefix.toString().startsWith('/guix/envs/env-')
    }

    def 'should create the correct guix command for package list' () {
        given:
        def folder = Files.createTempDirectory('test')
        def prefixPath = folder.resolve('env-x')
        def cache = Spy(GuixCache)
        cache.@installOptions = null
        cache.@createTimeout = Duration.of('20min')

        when:
        cache.createLocalGuixEnv0('bwa samtools', prefixPath)
        then:
        1 * cache.runCommand({ String c ->
            c.contains('guix package') && c.contains('--install') && c.contains('bwa samtools')
        }) >> 0

        cleanup:
        folder?.deleteDir()
    }

    def 'should detect a manifest file' () {
        given:
        def folder = Files.createTempDirectory('test')
        def manifest = folder.resolve('manifest.scm')
        manifest.text = '(specifications->manifest (list "bwa"))'
        def cache = Spy(GuixCache)

        expect:
        cache.isManifestFile(manifest.toString())
        !cache.isManifestFile('bwa samtools')

        cleanup:
        folder?.deleteDir()
    }

    def 'should create the correct guix command for a manifest file' () {
        given:
        def folder = Files.createTempDirectory('test')
        def manifest = folder.resolve('manifest.scm')
        manifest.text = '(specifications->manifest (list "bwa"))'
        def prefixPath = folder.resolve('env-x')
        def cache = Spy(GuixCache)
        cache.@installOptions = null
        cache.@createTimeout = Duration.of('20min')

        when:
        cache.createLocalGuixEnv0(manifest.toString(), prefixPath)
        then:
        1 * cache.runCommand({ String c ->
            c.contains('guix package') && c.contains('--manifest=') && c.contains('manifest.scm')
        }) >> 0

        cleanup:
        folder?.deleteDir()
    }

    def 'should hash manifest content rather than the path' () {
        given:
        def folder = Files.createTempDirectory('test')
        def manifest = folder.resolve('manifest.scm')
        manifest.text = '(specifications->manifest (list "bwa"))'
        def cache = Spy(GuixCache)
        def BASE = Paths.get('/guix/envs')

        when:
        def prefix = cache.guixPrefixPath(manifest.toString())
        then:
        1 * cache.getCacheDir() >> BASE
        prefix.toString().startsWith('/guix/envs/env-')

        cleanup:
        folder?.deleteDir()
    }

}
