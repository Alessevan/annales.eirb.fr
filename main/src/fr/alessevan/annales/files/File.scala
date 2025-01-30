package fr.alessevan.annales.files

import fr.alessevan.annales.*
import fr.alessevan.annales.files.File.Folder

import java.time.LocalDateTime

enum File:

  def name: FileName

  case Folder(name: FileName, subfiles: List[File]) extends File
  case FileRequest(name: FileName, hash: Hash, owner: CAS, time: LocalDateTime) extends File
  case FileAccepted(name: FileName, hash: Hash, owner: CAS, time: LocalDateTime, acceptedBy: CAS, acceptedTime: LocalDateTime) extends File

def findFile(root: Folder, path: FilePath): Either[String, File] =
  if path.isEmpty then return Left("Not Found")
  val parts = path.split("/")
  val firstPart = parts.head
  root.subfiles.filter(_.name.equals(firstPart)).head match
    case folder: File.Folder =>
      for
        newPath <- toFilePath(parts.tail.mkString("/"))
      yield findFile(folder, newPath)
      Right(folder)
    case file => Right(file)
