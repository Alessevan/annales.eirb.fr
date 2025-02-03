package fr.alessevan.annales.routes

import cask.{get, Logger, Response, Routes}
import castor.Context

case class WelcomeRoutes()(implicit cc: Context, log: Logger) extends Routes:

  @get("/")
  def welcome() = Response("<script>alert(\"Welcome to Annales !\");</script>", headers = Seq("content-type" -> "text/html; charset=UTF-8"))

  initialize()
