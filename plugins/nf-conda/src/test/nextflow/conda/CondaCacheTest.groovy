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

package nextflow.conda

import java.nio.file.Files
import java.nio.file.Paths

import spock.lang.Specification

/**
 *
 * @author Edmund Miller <edmund.miller@seqera.io>
 */
class CondaCacheTest extends Specification {

    def 'should apply a per-process create-options override in the command' () {
        given:
        def folder = Files.createTempDirectory('test')
        def prefixPath = folder.resolve('env-ovr')
        def cache = Spy(CondaCache)
        cache.@createOptions = '--from-config'   // config-level default
        cache.@createTimeout = nextflow.util.Duration.of('20min')

        when:
        cache.createLocalCondaEnv0('samtools', prefixPath, '--override-channels')
        then:
        1 * cache.runCommand({ String cmd ->
            cmd.contains('--override-channels') && !cmd.contains('--from-config')
        }) >> 0

        cleanup:
        folder?.deleteDir()
    }

    def 'should include the create-options override in the env hash' () {
        given:
        def base = Spy(CondaCache); base.@createOptions = null
        def ovr  = Spy(CondaCache); ovr.@createOptions = null
        def BASE = Paths.get('/conda/envs')

        when:
        def p1 = base.condaPrefixPath('samtools')
        def p2 = ovr.condaPrefixPath('samtools', '--override-channels')
        then:
        _ * base.getCacheDir() >> BASE
        _ * ovr.getCacheDir() >> BASE
        p1 != p2
    }

    def 'should build an env-create command for a custom-named YAML file' () {
        given:
        def folder = Files.createTempDirectory('test')
        def envFile = folder.resolve('env.yml')
        Files.write(envFile, 'name: foo\ndependencies:\n  - samtools\n'.bytes)
        def prefixPath = folder.resolve('env-yaml')
        def cache = Spy(CondaCache)
        cache.@createTimeout = nextflow.util.Duration.of('20min')

        when:
        cache.createLocalCondaEnv0(envFile.toString(), prefixPath)
        then:
        1 * cache.runCommand({ String cmd ->
            cmd.contains('conda env create ') &&
            cmd.contains('--file ') &&
            cmd.contains('env.yml')
        }) >> 0

        cleanup:
        folder?.deleteDir()
    }

    def 'should build a plain create command for a custom-named text file' () {
        given:
        def folder = Files.createTempDirectory('test')
        def envFile = folder.resolve('deps.txt')
        Files.write(envFile, 'samtools=1.17\nbcftools=1.18\n'.bytes)
        def prefixPath = folder.resolve('env-text')
        def cache = Spy(CondaCache)
        cache.@createTimeout = nextflow.util.Duration.of('20min')

        when:
        cache.createLocalCondaEnv0(envFile.toString(), prefixPath)
        then:
        1 * cache.runCommand({ String cmd ->
            cmd.contains('conda create ') &&
            !cmd.contains('env create') &&
            cmd.contains('--file ') &&
            cmd.contains('deps.txt')
        }) >> 0

        cleanup:
        folder?.deleteDir()
    }

    def 'should build a create command for a space-separated package list' () {
        given:
        def folder = Files.createTempDirectory('test')
        def prefixPath = folder.resolve('env-list')
        def cache = Spy(CondaCache)
        cache.@createTimeout = nextflow.util.Duration.of('20min')

        when:
        cache.createLocalCondaEnv0('samtools bcftools', prefixPath)
        then:
        1 * cache.runCommand({ String cmd ->
            cmd.contains('conda create ') &&
            !cmd.contains('--file') &&
            cmd.contains('samtools bcftools')
        }) >> 0

        cleanup:
        folder?.deleteDir()
    }

    def 'should reject a path-with-slash that is not an existing directory' () {
        given:
        def cache = Spy(CondaCache)

        when:
        cache.condaPrefixPath('/no/such/conda/prefix')
        then:
        thrown(IllegalArgumentException)
    }

    def 'should reject a multi-line invalid environment definition' () {
        given:
        def cache = Spy(CondaCache)

        when:
        cache.condaPrefixPath('samtools\nbcftools')
        then:
        thrown(IllegalArgumentException)
    }

}
