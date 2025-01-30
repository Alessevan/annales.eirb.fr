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

type CAS = String :| Match["^[a-z]+[0-9]*$"]

type Hash = String :| Match["^([a-f0-9][a-f0-9])+$"]
