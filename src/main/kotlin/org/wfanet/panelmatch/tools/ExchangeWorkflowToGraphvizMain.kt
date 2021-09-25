// Copyright 2021 The Cross-Media Measurement Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.wfanet.panelmatch.tools

import com.google.protobuf.TextFormat
import java.io.File
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Party.DATA_PROVIDER
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Party.MODEL_PROVIDER

fun main(args: Array<String>) {
  val workflow =
    File(args[0]).bufferedReader().use { reader ->
      ExchangeWorkflow.newBuilder().apply { TextFormat.merge(reader, this) }.build()
    }
  val steps = workflow.stepsList

  val graph = digraph {
    val colorByParty = mapOf(DATA_PROVIDER to "red", MODEL_PROVIDER to "blue")
    for ((party, partySteps) in steps.groupBy { it.party }) {
      val color = colorByParty.getValue(party)
      partySteps.forEach {
        node(it.stepId, mapOf("color" to color, "shape" to "box", "label" to it.stepId))
      }
      partySteps.flatMap { it.privateOutputLabelsMap.values }.toSet().forEach {
        node(it, mapOf("color" to color, "shape" to "egg", "label" to it))
      }
    }

    steps.flatMap { it.sharedOutputLabelsMap.values }.toSet().forEach {
      node(it, mapOf("shape" to "egg", "label" to it))
    }

    set("splines" to "ortho")
    for (step in steps) {
      for (label in step.privateInputLabelsMap.values + step.sharedInputLabelsMap.values) {
        edge(label, step.stepId)
      }
      for (label in step.privateOutputLabelsMap.values + step.sharedOutputLabelsMap.values) {
        edge(step.stepId, label)
      }
    }
  }

  graph.render(Printer())
}

private interface Element {
  fun render(printer: Printer)
}

@DslMarker private annotation class GraphvizDsl

private fun digraph(init: Digraph.() -> Unit) = Digraph().apply(init)

@GraphvizDsl
private class Digraph() : Element {
  private val children = mutableListOf<Element>()
  private val attributes = mutableMapOf<String, String>()

  override fun render(printer: Printer) {
    with(printer) {
      output("digraph {")
      withIndent {
        for ((k, v) in attributes) output("$k=$v")
        for (child in children) child.render(this)
      }
      output("}")
    }
  }

  fun node(name: String, attributes: Map<String, String> = emptyMap()) =
    children.add(Node(name, attributes))

  fun edge(from: String, to: String) = children.add(Edge(from, to))

  fun set(pair: Pair<String, String>) {
    attributes[pair.first] = pair.second
  }
}

@GraphvizDsl
private class Edge(private val from: String, private val to: String) : Element {
  override fun render(printer: Printer) {
    printer.output("${fixNodeName(from)} -> ${fixNodeName(to)}")
  }
}

@GraphvizDsl
private class Node(private val name: String, private val attributes: Map<String, String>) :
  Element {
  override fun render(printer: Printer) {
    val attributeString = attributes.toList().joinToString { (k, v) -> "$k=\"$v\"" }
    printer.output("${fixNodeName(name)} [$attributeString]")
  }
}

private class Printer private constructor(private var indent: Int) {
  constructor() : this(0)

  fun output(string: String) {
    repeat(indent) { print("  ") }
    println(string)
  }

  fun withIndent(block: () -> Unit) {
    indent++
    block()
    indent--
  }
}

private fun fixNodeName(name: String) = name.replace("-", "_")
