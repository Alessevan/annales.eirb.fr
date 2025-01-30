package fr.alessevan.annales.routes

import castor.Context
import cask.{Abort, Cookie, Logger, RawDecorator, Redirect, Request, Response, Routes, get}
import cask.model.Response.Raw
import cask.router.Result
import scalatags.Text.all.*
import fr.alessevan.annales.toCAS
import fr.alessevan.annales.users.User
import fr.alessevan.annales.users.User.Normal

import java.net.{URLDecoder, URLEncoder}
import java.time.Instant
import java.util.UUID

private var sessionIds: Map[String, User] = Map.empty

private def getSessionCookieUser(request: Request): Option[User] =
  request.cookies.get("session") match
    case Some(session) => sessionIds.get(session.value)
    case None          => None

case class logUser() extends RawDecorator:

  override def wrapFunction(ctx: Request, delegate: Delegate): Result[Raw] =
    getSessionCookieUser(ctx) match
      case Some(user) => delegate(ctx, Map("user" -> user))
      case None       => Result.Success(Redirect("/api/auth/login?redirect=" + URLEncoder.encode(ctx.exchange.getRequestURL, "UTF-8")))

case class isLogged() extends RawDecorator:

  override def wrapFunction(ctx: Request, delegate: Delegate): Result[Raw] =
    getSessionCookieUser(ctx) match
      case Some(user) => delegate(ctx, Map("user" -> user))
      case None       => Result.Success(Abort(401))

case class AuthenticationRoutes()(implicit cc: Context, log: Logger) extends Routes:
  @get("/api/auth/login")
  def authRedirection(request: Request, redirect: String = "/"): Response[String] =
    Redirect("https://cas.bordeaux-inp.fr/login?service=" + URLEncoder.encode(
      s"http://localhost.ipb.fr:8080/api/auth/validate?redirect=$redirect",
      "UTF-8"
    ))

  @get("/api/auth/validate")
  def authTicket(request: Request, redirect: String = "/", ticket: String = ""): Response[String] =
    val encoded = URLEncoder.encode(s"http://localhost.ipb.fr:8080/api/auth/validate?redirect=$redirect", "UTF-8")
    val redirectDecoded = URLDecoder.decode(redirect, "UTF-8")
    val response: String = requests.get(
      "https://cas.bordeaux-inp.fr/serviceValidate?service=" + encoded + "&ticket=" + ticket
    ).text()
    """<cas:user>([a-z]+[0-9]*)</cas:user>""".r.findFirstIn(response).map(_.replaceAll("</?cas:user>", "")) match
      case Some(cas) =>
        toCAS(cas) match
          case Right(login) =>
            val session = UUID.randomUUID().toString
            val user = Normal(login)
            sessionIds = sessionIds.filterNot((_, other) => other.equals(user)) + (session -> user)
            println(s"Authenticated as $cas")
            Response("OK", cookies = Seq(Cookie("session", s"$session")))
            // TODO : redirect
          case _ =>
            println("Authentication failed.")
            Response("KO", 401)
      case None =>
        println("Authentication failed.")
        Response("KO", 401)

  @isLogged
  @get("/api/auth/logout")
  def authLogout(request: Request)(user: User): Response[String] =
    sessionIds = sessionIds.filterNot((_, other) => other.equals(user))
    Response("OK", cookies = Seq(Cookie("session", "", expires = Instant.EPOCH)))

  initialize()
