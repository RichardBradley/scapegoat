package com.sksamuel.scapegoat

import java.io.{File, FileNotFoundException}
import java.net.URL

import scala.collection.mutable.ListBuffer
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.transform.{Transform, TypingTransformers}

/** @author Stephen Samuel */
trait PluginRunner {

  val scalaVersion = "2.11.1"
  val shortScalaVersion = scalaVersion.dropRight(2)

  val classPath = getScalaJars.map(_.getAbsolutePath) :+ sbtCompileDir.getAbsolutePath

  val settings = {
    val s = new scala.tools.nsc.Settings
    s.Xprint.value = List("all")
    s.Yrangepos.value = true
    s.Yposdebug.value = true
    s.classpath.value = classPath.mkString(":")
    s
  }

  val inspections: Seq[Inspection]
  val reporter = new ConsoleReporter(settings)
  lazy val compiler = new ScapegoatCompiler(settings, inspections, reporter)

  def writeCodeSnippetToTempFile(code: String): File = {
    val file = File.createTempFile("scapegoat_snippet", ".scala")
    org.apache.commons.io.FileUtils.write(file, code)
    file.deleteOnExit()
    file
  }

  def addToClassPath(groupId: String, artifactId: String, version: String): Unit = {
    settings.classpath.value = settings.classpath.value + ":" + findIvyJar(groupId, artifactId, version).getAbsolutePath
  }

  def compileCodeSnippet(code: String): ScapegoatCompiler = compileSourceFiles(writeCodeSnippetToTempFile(code))
  def compileSourceResources(urls: URL*): ScapegoatCompiler = {
    compileSourceFiles(urls.map(_.getFile).map(new File(_)): _*)
  }
  def compileSourceFiles(files: File*): ScapegoatCompiler = {
    reporter.flush()
    val command = new scala.tools.nsc.CompilerCommand(files.map(_.getAbsolutePath).toList, settings)
    new compiler.Run().compile(command.files)
    compiler
  }

  def getScalaJars: List[File] = {
    val scalaJars = List("scala-compiler", "scala-library", "scala-reflect")
    scalaJars.map(findScalaJar)
  }

  def findScalaJar(artifactId: String): File = findIvyJar("org.scala-lang", artifactId, scalaVersion)

  def findIvyJar(groupId: String, artifactId: String, version: String): File = {
    val userHome = System.getProperty("user.home")
    val sbtHome = userHome + "/.ivy2"
    val jarPath = sbtHome + "/cache/" + groupId + "/" + artifactId + "/jars/" + artifactId + "-" + version + ".jar"
    val file = new File(jarPath)
    if (file.exists) {
      println(s"Located ivy jar [$file]")
      file
    } else throw new FileNotFoundException(s"Could not locate [$jarPath].")
  }

  def sbtCompileDir: File = {
    val dir = new File("./target/scala-" + shortScalaVersion + "/classes")
    if (dir.exists) dir
    else throw new FileNotFoundException(s"Could not locate SBT compile directory for plugin files [$dir]")
  }
}

class ScapegoatCompiler(settings: scala.tools.nsc.Settings,
                        inspections: Seq[Inspection],
                        reporter: ConsoleReporter)
  extends scala.tools.nsc.Global(settings, reporter) {

  val scapegoat = new ScapegoatComponent(this, inspections)
  val store = new SourceStoreComponent(this)

  override def computeInternalPhases() {
    val phs = List(
      syntaxAnalyzer -> "parse source into ASTs, perform simple desugaring",
      analyzer.namerFactory -> "resolve names, attach symbols to named trees",
      analyzer.packageObjects -> "load package objects",
      analyzer.typerFactory -> "the meat and potatoes: type the trees",
      scapegoat -> "scapegoat",
      patmat -> "translate match expressions",
      superAccessors -> "add super accessors in traits and nested classes",
      extensionMethods -> "add extension methods for inline classes",
      pickler -> "serialize symbol tables",
      refChecks -> "reference/override checking, translate nested objects",
      uncurry -> "uncurry, translate function values to anonymous classes",
      tailCalls -> "replace tail calls by jumps",
      specializeTypes -> "@specialized-driven class and method specialization",
      explicitOuter -> "this refs to outer pointers, translate patterns",
      erasure -> "erase types, add interfaces for traits",
      postErasure -> "clean up erased inline classes",
      lazyVals -> "allocate bitmaps, translate lazy vals into lazified defs",
      lambdaLift -> "move nested functions to top level",
      constructors -> "move field definitions into constructors",
      mixer -> "mixin composition",
      cleanup -> "platform-specific cleanups, generate reflective calls",
      genicode -> "generate portable intermediate code",
      inliner -> "optimization: do inlining",
      inlineExceptionHandlers -> "optimization: inline exception handlers",
      closureElimination -> "optimization: eliminate uncalled closures",
      deadCode -> "optimization: eliminate dead code",
      //store -> "store",
      terminal -> "The last phase in the compiler chain"
    )
    phs foreach (addToPhasesSet _).tupled
  }
}

class SourceStoreComponent(val global: Global) extends PluginComponent with TypingTransformers with Transform {

  val sources = new ListBuffer[String]

  override val phaseName: String = "scapegoat-teststore"
  override val runsAfter: List[String] = List("dce")
  // deadcode
  override val runsBefore = List[String]("terminal")

  override protected def newTransformer(unit: global.CompilationUnit): global.Transformer = new Transformer(unit)
  class Transformer(unit: global.CompilationUnit) extends TypingTransformer(unit) {

    override def transform(tree: global.Tree) = {
      sources append tree.toString
      tree
    }
  }
}

