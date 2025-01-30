package fr.alessevan.annales.routes

import cask.{Logger, RemainingPathSegments, Routes, staticResources}
import castor.Context
import fr.alessevan.annales.files.{File, findFile}
import fr.alessevan.annales.toFilePath
import io.github.iltotore.iron.autoRefine

case class FileRoutes()(implicit cc: Context, log: Logger) extends Routes:

  private var rootFiles: File.Folder = File.Folder("root", List.empty)
  private var requestFiles: File.Folder = File.Folder("waiting", List.empty)

  @staticResources("/static/")
  def staticFileRoutes(segments: RemainingPathSegments): String =
    println("Receive request for file.")
    for
      path <- toFilePath(segments.value.mkString("/"))
      file <- findFile(this.rootFiles, path)
    yield s"static/$file"
    "404"

  initialize()
