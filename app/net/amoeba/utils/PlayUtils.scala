package net.amoeba.utils

import play.api.Play
import play.api.data.validation.Constraints
import play.api.data.validation.Valid

trait PlayUtils {
  val playconfig = Play.current.configuration

}

object ut extends PlayUtils {
  val nonEmptyValidator = Constraints.nonEmpty

  val EmailValidator = Constraints.pattern(
    """\b[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*\b""".r,
    "constraint.email",
    "error.email")

  def isEmpty(s: String) = nonEmptyValidator(s) match {
    case Valid => true
    case _ => false
  }

  def isEmail(s: String) = EmailValidator(s) match {
    case Valid => true
    case _ => false
  }

}

