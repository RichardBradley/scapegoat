package com.sksamuel.scapegoat.inspections

import com.sksamuel.scapegoat.PluginRunner
import org.scalatest.{Matchers, FreeSpec}

/** @author Stephen Samuel */
class FilterSizeTest
  extends FreeSpec with Matchers with PluginRunner {

  override val inspections = Seq(new FilterSize)

  "filter then size" - {
    "should report warning" in {

      val code = """object Test {
                      val list = List(1,2,3,4).filter(_ % 2 == 0).size
                    } """.stripMargin

      compileCodeSnippet(code)
      compiler.scapegoat.feedback.warnings.size shouldBe 1
    }
  }
}