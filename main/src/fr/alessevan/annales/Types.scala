package fr.alessevan.annales

import io.github.iltotore.iron.constraint.string.Match
import io.github.iltotore.iron.{:|, DescribedAs, refineEither}

type FileNameRegEx =
  DescribedAs[
    Match[
      "^[a-zA-Z0-9-_\\.]+$"
    ],
    "A file name must only contain alphanumeric characters, dashes and underscores."
  ]
type FileName = String :| FileNameRegEx

type FilePathRegEx =
  DescribedAs[
    Match[
      "^([a-zA-Z0-9-_\\.]+\\/)*[a-zA-Z0-9-_\\.]+\\.(pdf|jpg|png)$"
    ],
    "A file path must only contain alphanumeric characters, dashes and underscores, and end with a file extension."
  ]
type FilePath = String :| FilePathRegEx

def toFilePath(path: String): Either[String, FilePath] =
  for
    filePath: FilePath <- path.refineEither[FilePathRegEx]
  yield filePath

type CasRegEx =
  DescribedAs[
    Match[
      "^[a-z]+[0-9]*$"
    ],
    "A CAS must only be alphabetic characters followed by numbers."
  ]
type CAS = String :| CasRegEx

def toCAS(login: String): Either[String, CAS] =
  for
    cas: CAS <- login.refineEither[CasRegEx]
  yield cas

type Hash = String :| Match["^([a-f0-9][a-f0-9])+$"]
