package com.sksamuel.scapegoat.inspections

import com.sksamuel.scapegoat.PluginRunner
import org.scalatest.{FreeSpec, Matchers}

/** @author Stephen Samuel */
class VarUseTest extends FreeSpec with ASTSugar with Matchers with PluginRunner {

  override val inspections = Seq(new VarUse)

  "var keyword use" - {
    "should report warning" in {
      val code = """class Test {
                      def hello : String = "sammy"
                      var name = hello
                    } """.stripMargin

      compileCodeSnippet(code)
      compiler.scapegoat.reporter.warnings.size shouldBe 1
    }
  }
}
