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

package nextflow.nix

import java.nio.file.Files
import java.nio.file.Paths

import nextflow.SysEnv
import nextflow.util.Duration
import spock.lang.Specification

/**
 *
 * @author Evan Floden
 */
class NixCacheTest extends Specification {

    def setupSpec() {
        SysEnv.push([:])
    }

    def cleanupSpec() {
        SysEnv.pop()
    }

    def 'should create nix env prefix path for a string env' () {
        given:
        def ENV = 'bwa samtools'
        def cache = Spy(NixCache)
        def BASE = Paths.get('/nix/envs')

        when:
        def prefix = cache.nixPrefixPath(ENV)
        then:
        1 * cache.getCacheDir() >> BASE
        prefix.toString().startsWith('/nix/envs/env-')
    }

    def 'should include flake ref in hash' () {
        given:
        def cache1 = Spy(NixCache)
        cache1.@flakeRef = 'nixpkgs'
        def cache2 = Spy(NixCache)
        cache2.@flakeRef = 'other'
        def BASE = Paths.get('/nix/envs')

        when:
        def prefix1 = cache1.nixPrefixPath('bwa')
        def prefix2 = cache2.nixPrefixPath('bwa')
        then:
        _ * cache1.getCacheDir() >> BASE
        _ * cache2.getCacheDir() >> BASE
        prefix1 != prefix2
    }

    def 'should create the correct nix command for package list' () {
        given:
        def folder = Files.createTempDirectory('test')
        def prefixPath = folder.resolve('env-x')
        def cache = Spy(NixCache)
        cache.@flakeRef = 'nixpkgs'
        cache.@installOptions = null
        cache.@createTimeout = Duration.of('20min')

        when:
        cache.createLocalNixEnv0('bwa', prefixPath)
        then:
        1 * cache.runCommand({ String c ->
            c.contains('nix') && c.contains('profile install') && c.contains('nixpkgs#bwa')
        }) >> 0

        cleanup:
        folder?.deleteDir()
    }

}
