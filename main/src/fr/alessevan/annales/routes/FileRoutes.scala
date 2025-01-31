package fr.alessevan.annales.routes

import cask.model.Response.Raw
import cask.router.Result
import cask.{Logger, RawDecorator, Redirect, RemainingPathSegments, Request, Response, Routes, get, staticResources}
import castor.Context
import fr.alessevan.annales.files.File.Folder
import fr.alessevan.annales.files.{File, findFile}
import fr.alessevan.annales.toFilePath
import io.github.iltotore.iron.autoRefine

protected var rootFiles: File.Folder = File.Folder("root", List.empty)
protected var requestFiles: File.Folder = File.Folder("waiting", List.empty)

private abstract class getFileDecorator extends RawDecorator:

  def getResult(folder: Folder, ctx: Request, delegate: Delegate): Result[Raw] =
    val path: String = ctx.remainingPathSegments.mkString("/")
    toFilePath(path) match
      case Right(filePath) =>
        findFile(rootFiles, filePath) match
          case Right(file) => delegate(ctx, Map("file" -> file))
          case Left(error) => Result.Success(Response(error))
      case Left(error) => Result.Success(Response(error))

private class getRootFile extends getFileDecorator:

  override def wrapFunction(ctx: Request, delegate: Delegate): Result[Raw] =
    super.getResult(rootFiles, ctx, delegate)

private class getRequestFile extends getFileDecorator:

  override def wrapFunction(ctx: Request, delegate: Delegate): Result[Raw] =
    super.getResult(requestFiles, ctx, delegate)

case class FileRoutes()(implicit cc: Context, log: Logger) extends Routes:

  @getRootFile
  @get("/files")
  def getResourceFile()(file: File) =
    file match
      case folder: File.Folder =>
        Response("")
      case file: File.FileAccepted => Redirect(s"/static/${file.hash}")
      case _                       => Redirect("/static/404.html")

  initialize()
