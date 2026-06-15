#!/usr/bin/env nextflow

nextflow.enable.dsl=2

process helloConda {
    package "cowsay", provider: "conda"

    output:
    stdout

    script:
    """
    cowsay "Hello conda"
    """
}

workflow {
    helloConda() | view
}
