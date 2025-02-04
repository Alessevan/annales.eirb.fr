package fr.alessevan.annales.routes

import cask.model.Response.Raw
import cask.router.Result
import cask.{Logger, RawDecorator, Redirect, Request, Response, Routes, get, staticResources}
import castor.Context
import fr.alessevan.annales.files.File.Folder
import fr.alessevan.annales.files.{File, findFile}
import fr.alessevan.annales.renderer.{body, html, p, rendererToResponse}
import fr.alessevan.annales.users.User
import fr.alessevan.annales.users.User.{Admin, Normal}
import fr.alessevan.annales.{Hash, toFilePath, toHash}
import io.github.iltotore.iron.autoRefine

import java.time.LocalDateTime

protected var rootFiles: File.Folder = File.Folder(
  "root",
  Seq(File.Folder("waiting", Seq.empty), File.FileAccepted("test", "af", "test", LocalDateTime.now(), "test", LocalDateTime.now()))
)
protected var requestFiles: File.Folder = File.Folder("waiting", Seq.empty)

private abstract class getFileDecorator extends RawDecorator:

  def getResult(folder: Folder, ctx: Request, delegate: Delegate): Result[Raw] =
    val path: String = ctx.remainingPathSegments.mkString("/")
    if path.isBlank then
      delegate(ctx, Map("file" -> folder))
    else
      toFilePath(path) match
        case Right(filePath) =>
          findFile(folder, filePath) match
            case Right(file) => delegate(ctx, Map("file" -> file))
            case Left(error) => Result.Success(Response(error))
        case Left(error) => Result.Success(Response(error))

private class fromFolder(folder: Folder) extends getFileDecorator:

  override def wrapFunction(ctx: Request, delegate: Delegate): Result[Raw] =
    super.getResult(folder, ctx, delegate)

case class FileRoutes()(implicit cc: Context, log: Logger) extends Routes:

  @logUser
  @fromFolder(rootFiles)
  @get("/files")
  def getResourceFile()(file: File)(user: User): Response[String] =
    file match
      case folder: File.Folder =>
        rendererToResponse(html(body =
          body(
            (user match
              case Normal(cas) =>
                folder.subfiles.map:
                  case File.Folder(name, _)                                                 => p(s"Folder Icon$$$name")
                  case File.FileAccepted(name, hash, owner, time, acceptedBy, acceptedTime) => p(s"""File Icon$$$name$$$hash""")
                  case _                                                                    => p("")
              case Admin(cas) => Seq(p(s"Admin$$$cas"))
            )*
          )
        ))
      case file: File.FileAccepted => Redirect(s"/static/${file.hash}")
      case _                       => Redirect("/static/404.html")

  @logUser
  @staticResources("/static/:file")
  def getStaticFile(file: String)(user: User): String =
    for
      hash: Hash <- toHash(file)
    yield s"/files/$hash"
    "/files/404.html"

  initialize()
