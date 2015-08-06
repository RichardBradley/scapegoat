package com.sksamuel.scapegoat.inspections.unnecessary

import com.sksamuel.scapegoat.inspections.NoOpOverride
import com.sksamuel.scapegoat.inspections.unneccesary.UnnecessaryToInt
import com.sksamuel.scapegoat.test.ScapegoatTestPluginRunner
import org.scalatest.{ FreeSpec, Matchers, OneInstancePerTest }

/** @author Stephen Samuel */
class NoOpOverrideTest
    extends FreeSpec
    with Matchers
    with ScapegoatTestPluginRunner
    with OneInstancePerTest {

  override val inspections = Seq(new NoOpOverride)

  "UnnecessaryOverride" - {
    "should report warning" - {
      "when overriding method to just call super" in {

        val code =
          """class Test {
            |  override def finalize() {
            |    super.finalize()
            |  }
            |}
          """.stripMargin

        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings.size shouldBe 1
      }
    }
    "should not report warning" - {
      "when overriding method with super call and other code" in {

        val code =
          """class Test {
            |  override def finalize() {
            |    super.finalize()
            |    println("sam")
            |  }
            |}
          """.stripMargin

        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings.size shouldBe 0
      }
    }
  }
}
