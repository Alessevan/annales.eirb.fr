package fr.alessevan.annales.routes

import cask.{get, Logger, Response, Routes}
import castor.Context
import fr.alessevan.annales.renderer.*

case class WelcomeRoutes()(implicit cc: Context, log: Logger) extends Routes:

  @get("/")
  def welcome(): Response[String] =
    rendererToResponse(html(body = body(script("alert(1);"))))

  initialize()
