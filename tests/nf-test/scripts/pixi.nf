#!/usr/bin/env nextflow

nextflow.enable.dsl=2

process helloPixi {
    package "cowpy", provider: "pixi"

    output:
    stdout

    script:
    """
    cowpy "Hello pixi"
    """
}

workflow {
    helloPixi() | view
}
