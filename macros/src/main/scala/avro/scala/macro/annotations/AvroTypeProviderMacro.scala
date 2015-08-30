package com.julianpeeters.avro.annotations

import provider._

import scala.reflect.macros.blackbox.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation

import collection.JavaConversions._
import java.io.File

import com.typesafe.scalalogging._

object AvroTypeProviderMacro extends LazyLogging {

  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._

    //get the namespace from the context and passing it around instead of using schema.getNamespace
    //in order to read schemas that omit namespace (e.g. nested schemas or python's avrostorage default)
    val namespace = NamespaceProbe.getNamespace(c)

    val result = {
      annottees.map(_.tree).toList match {
        case q"$mods class $name[..$tparams](..$first)(...$rest) extends ..$parents { $self => ..$body }" :: tail => {

          //currently, having a `@AvroRecord` the only thing that will trigger the writing of vars instead of vals
          val isImmutable: Boolean = {
            !mods.annotations.exists(mod => mod.toString == "new AvroRecord()" | mod.toString =="new AvroRecord(null)")
          }

          //helpful for IDE users who may not be able to easily see where their files live
          logger.info(s"Current path: ${new File(".").getAbsolutePath}")

          val avroFilePath = FilePathProbe.getPath(c)
          val infile = new File(avroFilePath)
          val schema = FileParser.getSchema(infile)

          //wraps each schema field in a quasiquote, returning immutable val defs if immutable flag is true
          val newFields: List[ValDef] = ValDefGenerator.asScalaFields(schema, namespace, isImmutable, c)

          tail match {
            // if there is no preexisiting companion
            case Nil => q"$mods class $name[..$tparams](..${newFields:::first})(...$rest) extends ..$parents { $self => ..$body }"
            // if there is a preexisting companion, include it with the updated classDef
            case moduleDef @ q"object $moduleName { ..$moduleBody }" :: Nil => {
              q"""$mods class $name[..$tparams](..${newFields:::first})(...$rest) extends ..$parents { $self => ..$body }; 
                object ${name.toTermName} { ..$moduleBody }"""
            }
          }
        }
      }
    }

    c.Expr[Any](result)
  }
}

class AvroTypeProvider(inputPath: String) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro AvroTypeProviderMacro.impl
}
