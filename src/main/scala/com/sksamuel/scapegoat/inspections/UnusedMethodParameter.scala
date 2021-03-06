package com.sksamuel.scapegoat.inspections

import com.sksamuel.scapegoat.{Inspection, Levels, Reporter}

/** @author Stephen Samuel */
class UnusedMethodParameter extends Inspection {

  import scala.reflect.runtime.universe._

  override def traverser(reporter: Reporter): Traverser = new Traverser {

    private def usesParameter(param: ValDef, rhs: Tree): Boolean = {
      rhs match {
        case Ident(TermName(name)) => name == param.name.toString
        case _ => rhs.children.exists(usesParameter(param, _))
      }
    }

    override def traverse(tree: Tree): Unit = {
      tree match {
        // ignore abstract methods obv.
        case DefDef(mods, _, _, _, _, _) if mods.hasFlag(Flag.ABSTRACT) =>
        // ignore constructors, those params become fields
        case DefDef(_, name, _, vparamss, _, rhs) if name.toString != "<init>" =>
          for ( vparams <- vparamss;
                vparam <- vparams ) {
            if (!usesParameter(vparam, rhs))
              reporter.warn("Unused method parameter", tree, level = Levels.Warning,
                s"Unused method parameter ($vparam) at " + name.toString.take(100))
          }
        case _ => super.traverse(tree)
      }
    }
  }
}
