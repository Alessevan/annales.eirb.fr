import mill.Agg
import mill.scalalib.scalafmt.ScalafmtModule
import mill.scalalib.{DepSyntax, ScalaModule}

object main extends ScalaModule with ScalafmtModule {

  def scalaVersion = "3.6.3"

  override def ivyDeps = Agg(
    ivy"io.github.iltotore::iron::2.6.0",
    ivy"com.lihaoyi::cask:0.9.5",
    ivy"com.lihaoyi::requests:0.9.0",
    ivy"com.lihaoyi::scalatags:0.13.1",
  )
}