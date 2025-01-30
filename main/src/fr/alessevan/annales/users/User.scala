package fr.alessevan.annales.users

import fr.alessevan.annales.CAS

enum User:

  def name: CAS

  case Normal(name: CAS) extends User
  case Admin(name: CAS) extends User
