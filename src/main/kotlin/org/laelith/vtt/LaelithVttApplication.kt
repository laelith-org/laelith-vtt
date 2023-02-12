package org.laelith.vtt

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

@SpringBootApplication
@ComponentScan(
	basePackages = ["org.laelith.vtt"],
	excludeFilters = [ComponentScan.Filter(type = FilterType.REGEX, pattern = ["org.laelith.vtt.Application*"])]
)
class LaelithVttApplication

fun main(args: Array<String>) {
	runApplication<LaelithVttApplication>(*args)
}
