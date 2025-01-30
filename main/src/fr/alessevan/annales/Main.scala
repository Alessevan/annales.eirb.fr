package fr.alessevan.annales

import cask.model.Response.Raw
import cask.router.Result
import cask.{Cookie, MainRoutes, RawDecorator, Redirect, RemainingPathSegments, Request, Response}
import fr.alessevan.annales.files.{File, findFile}
import fr.alessevan.annales.toFilePath
import fr.alessevan.annales.users.User
import io.github.iltotore.iron.autoRefine

import java.lang.invoke.MethodHandles
import java.net.URLEncoder

object Main extends MainRoutes:

  private var rootFiles: File.Folder = File.Folder("root", List.empty)
  private var requestFiles: File.Folder = File.Folder("waiting", List.empty)

  override def host: String = "0.0.0.0"
  override def port: Int = 8080

  private class loggedUser extends RawDecorator:

    override def wrapFunction(ctx: Request, delegate: Delegate): Result[Raw] =
      for
        cookie <- ctx.cookies.get("username")
        if "admin".eq(cookie)
      yield delegate(ctx, Map("user" -> User.Admin("admin")))
      Result.Success(Response("Unauthorized", statusCode = 401))

  @loggedUser
  @cask.get("/")
  def welcome(request: Request)(user: User): Unit =
    println(
      s"Receive ${request.exchange.getRequestMethod} from ${request.exchange.getConnection.getPeerAddress} to ${request.exchange.getRequestURL}"
    )

  @cask.staticResources("/static/")
  def staticFileRoutes(segments: RemainingPathSegments): String =
    println("Receive request for file.")
    for
      path <- toFilePath(segments.value.mkString("/"))
      file <- findFile(rootFiles, path)
    yield s"static/$file"
    "404"

  @cask.get("/api/auth/login")
  def authRedirection(request: Request): Response[String] =
    Redirect("https://cas.bordeaux-inp.fr/login?service=http://localhost.ipb.fr:8080/api/auth/validate")

  @cask.get("/api/auth/validate")
  def authTicket(request: Request, ticket: String) =
    val encoded = URLEncoder.encode("http://localhost.ipb.fr:8080/api/auth/validate", "UTF-8")
    val response: String = requests.get(
      "https://cas.bordeaux-inp.fr/serviceValidate?service=" + encoded + "&ticket=" + ticket
    ).text()
    """<cas:user>([a-z]+[0-9]*)</cas:user>""".r.findFirstIn(response).map(_.replaceAll("</?cas:user>", "")) match
      case Some(cas) =>
        println(s"Authenticated as $cas")
        "OK"
      case None =>
        println("Authentication failed.")
        "KO"

  println("root@annales.eirb.fr $ ./initialize")
  initialize()
  println("Initialized !")
