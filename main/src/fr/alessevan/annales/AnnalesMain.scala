package fr.alessevan.annales

import cask.{RemainingPathSegments, Request, get, staticResources}
import fr.alessevan.annales.files.{File, findFile}
import fr.alessevan.annales.routes.{AuthenticationRoutes, FileRoutes, WelcomeRoutes}

object AnnalesMain extends cask.Main:

  override def host: String = "0.0.0.0"
  override def port: Int = 8080

  println("root@annales.eirb.fr $ ./initialize")
//  initialize()
  val allRoutes: Seq[cask.Routes] = Seq(AuthenticationRoutes(), FileRoutes(), WelcomeRoutes())
  println("Initialized !")
