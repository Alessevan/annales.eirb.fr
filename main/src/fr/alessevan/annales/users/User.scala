package fr.alessevan.annales.users

import fr.alessevan.annales.CAS

enum User:

  def name: CAS

  case Normal(name: CAS) extends User
  case Admin(name: CAS) extends User

  override def equals(that: Any): Boolean =
    that match
      case normal: Normal => name.eq(normal.name)
      case _              => false
