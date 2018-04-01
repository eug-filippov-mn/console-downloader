package com.eug.md.utils

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Option

fun CommandLine.getOptionValue(option: Option): String = this.getOptionValue(option.opt)

fun CommandLine.hasOption(option: Option): Boolean = this.hasOption(option.opt)