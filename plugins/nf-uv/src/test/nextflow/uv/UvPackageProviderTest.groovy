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

package nextflow.uv

import java.nio.file.Paths

import nextflow.packages.PackageSpec
import spock.lang.Specification

/**
 *
 * @author Evan Floden
 */
class UvPackageProviderTest extends Specification {

    def 'should report provider name' () {
        given:
        def provider = new UvPackageProvider(new UvConfig([:], [:]))
        expect:
        provider.getName() == 'uv'
    }

    def 'should only support uv specs' () {
        given:
        def provider = new UvPackageProvider(new UvConfig([:], [:]))
        expect:
        provider.supportsSpec(new PackageSpec('uv', ['numpy']))
        !provider.supportsSpec(new PackageSpec('conda', ['samtools']))
    }

    def 'should build the activation script' () {
        given:
        def provider = new UvPackageProvider(new UvConfig([:], [:]))
        when:
        def script = provider.getActivationScript(Paths.get('/work/uv/env-abc'))
        then:
        script.trim() == 'source /work/uv/env-abc/bin/activate'
    }

    def 'should reject a spec for another provider' () {
        given:
        def provider = new UvPackageProvider(new UvConfig([:], [:]))
        when:
        provider.createEnvironment(new PackageSpec('conda', ['samtools']))
        then:
        thrown(IllegalArgumentException)
    }

    def 'should reject an empty spec' () {
        given:
        def provider = new UvPackageProvider(new UvConfig([:], [:]))
        when:
        provider.createEnvironment(new PackageSpec('uv', []))
        then:
        thrown(IllegalArgumentException)
    }

    def 'should delegate package entries to the cache as a space-separated env' () {
        given:
        def config = new UvConfig([:], [:])
        def provider = new UvPackageProvider(config)
        def cache = Mock(UvCache)
        provider.@cache = cache

        when:
        provider.createEnvironment(new PackageSpec('uv', ['numpy', 'pandas']))
        then:
        1 * cache.getCachePathFor('numpy pandas') >> Paths.get('/work/uv/env-abc')
    }

    def 'should delegate an environment file to the cache' () {
        given:
        def config = new UvConfig([:], [:])
        def provider = new UvPackageProvider(config)
        def cache = Mock(UvCache)
        provider.@cache = cache
        def spec = new PackageSpec('uv').withEnvironment('/path/to/requirements.txt')

        when:
        provider.createEnvironment(spec)
        then:
        1 * cache.getCachePathFor('/path/to/requirements.txt') >> Paths.get('/work/uv/env-def')
    }

    def 'should report config' () {
        given:
        def config = new UvConfig([:], [:])
        def provider = new UvPackageProvider(config)
        expect:
        provider.getConfig().is(config)
    }
}
