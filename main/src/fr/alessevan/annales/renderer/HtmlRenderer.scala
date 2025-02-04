package fr.alessevan.annales.renderer

import cask.{Cookie, Response}

abstract class HtmlRenderer:

  def render(): String

class raw(raw: String) extends HtmlRenderer():

  override def render(): String = raw

case class empty() extends raw("")

case class html(header: header = header(empty()), body: body = body(empty()))
    extends raw(s"<!DOCTYPE html><html lang=\"fr\">${header.render()}${body.render()}</html>")

case class header(elements: HtmlRenderer*) extends raw(s"<head>${elements.map(_.render()).mkString("")}</head>")

case class body(elements: HtmlRenderer*) extends raw(s"<body>${elements.map(_.render()).mkString("")}</body>")

case class script(js: String) extends raw(s"<script>$js</script>")

case class p(paragraph: String) extends raw(s"$paragraph")

def rendererToResponse(htmlRenderer: HtmlRenderer, headers: Seq[(String, String)] = Seq(), cookies: Seq[Cookie] = Seq()): Response[String] =
  Response(
    htmlRenderer.render(),
    headers = Seq(("content-type", "text/html; charset=UTF-8")) ++ headers,
    cookies = cookies
  )
