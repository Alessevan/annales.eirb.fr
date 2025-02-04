package fr.alessevan.annales.routes

import castor.Context
import cask.{Abort, Cookie, Logger, RawDecorator, Redirect, Request, Response, Routes, get}
import cask.model.Response.Raw
import cask.router.Result
import fr.alessevan.annales.renderer.{html, rendererToResponse, body, script}
import fr.alessevan.annales.toCAS
import fr.alessevan.annales.users.User
import fr.alessevan.annales.users.User.Normal

import java.net.{URLDecoder, URLEncoder}
import java.time.{Duration, Instant}
import java.time.temporal.TemporalAmount
import java.util.UUID

private var sessionIds: Map[String, (User, Instant)] = Map.empty
private val expirationDelay: TemporalAmount = Duration.ofHours(1)

private def filter(sessionIds: Map[String, (User, Instant)], user: User, now: Instant): Map[String, (User, Instant)] =
  sessionIds.filter((_, other) => other._1.equals(user) && other._2.plus(expirationDelay).isAfter(now))

private def filterNot(sessionIds: Map[String, (User, Instant)], user: User, now: Instant): Map[String, (User, Instant)] =
  sessionIds.filterNot((_, other) => other._1.equals(user) || other._2.plus(expirationDelay).isBefore(now))

private def filterTime(sessionIds: Map[String, (User, Instant)], now: Instant): Map[String, (User, Instant)] =
  sessionIds.filter((_, other) => other._2.plus(expirationDelay).isAfter(now))

/**
 * Get the user from the session cookie.
 * @param request The request to get the cookie from.
 * @return The user if the session cookie is valid, None otherwise.
 */
private def getSessionCookieUser(request: Request): Option[User] =
  request.cookies.get("session") match
    case Some(session) => filterTime(sessionIds, Instant.now()).get(session.value) match
        case Some((user, _)) => Some(user)
        case _               => None
    case None => None

/**
 * Decorator to log the user in the session if their not already connected.
 */
case class logUser() extends RawDecorator:

  override def wrapFunction(ctx: Request, delegate: Delegate): Result[Raw] =
    getSessionCookieUser(ctx) match
      case Some(user) => delegate(ctx, Map("user" -> user))
      case None       => Result.Success(Redirect("/api/auth/login?redirect=" + URLEncoder.encode(ctx.exchange.getRequestURL, "UTF-8")))

/**
 * Decorator to check if the user is logged in.
 */
case class isLogged() extends RawDecorator:

  override def wrapFunction(ctx: Request, delegate: Delegate): Result[Raw] =
    getSessionCookieUser(ctx) match
      case Some(user) => delegate(ctx, Map("user" -> user))
      case None       => Result.Success(Abort(401))

/**
 * Decorator to get the user if their logged in.
 */
case class getSomeUser() extends RawDecorator:

  override def wrapFunction(ctx: Request, delegate: Delegate): Result[Raw] =
    delegate(ctx, Map("user" -> getSessionCookieUser(ctx)))

/**
 * Routes for the authentication.
 * @param cc The context of the application.
 * @param log The logger of the application.
 */
case class AuthenticationRoutes()(implicit cc: Context, log: Logger) extends Routes:

  /**
   * Route to redirect to the CAS login page.
   * @param request The request to get the URL from.
   * @param redirect The URL to redirect to after the authentication.
   * @return The redirection to the CAS login page.
   */
  @getSomeUser
  @get("/api/auth/login")
  def authRedirection(request: Request, redirect: String = "/")(user: Option[User]): Response[String] =
    user match
      case Some(_) => Redirect(redirect)
      case _ =>
        Redirect("https://cas.bordeaux-inp.fr/login?service=" + URLEncoder.encode(
          s"http://localhost.ipb.fr:8080/api/auth/validate?redirect=$redirect",
          "UTF-8"
        ))

  /**
   * Route to validate the CAS ticket.
   * @param request The request to get the ticket from.
   * @param redirect The URL to redirect to after the authentication.
   * @param ticket The ticket to validate.
   * @return The validation of the ticket.
   */
  @getSomeUser
  @get("/api/auth/validate")
  def authTicket(request: Request, redirect: String = "/", ticket: String = "")(user: Option[User]): Response[String] =
    user match
      case Some(_) => Redirect(redirect)
      case _ =>
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
                val now = Instant.now()
                sessionIds = filterNot(sessionIds, user, now) + (session -> (user, now))
                println(s"${request.exchange.getSourceAddress.getHostString} Authenticated as $cas")
                rendererToResponse(
                  html(body = body(script(s"window.location = '$redirectDecoded';"))),
                  cookies = Seq(Cookie("session", s"$session", expires = now.plus(expirationDelay), path = "/"))
                )
              case _ =>
                println(s"${request.exchange.getSourceAddress.getAddress} Authentication failed.")
                Response("KO", 401)
          case None =>
            println(s"${request.exchange.getSourceAddress.getAddress} Authentication failed.")
            Response("KO", 401)

  /**
   * Route to logout the user.
   * @param request The request to get the user from.
   * @param user The user to logout.
   * @return The logout of the user.
   */
  @isLogged
  @get("/api/auth/logout")
  def authLogout(request: Request)(user: User): Response[String] =
    sessionIds = filterNot(sessionIds, user, Instant.now())
    Response("OK", cookies = Seq(Cookie("session", "", expires = Instant.EPOCH)))

  initialize()
