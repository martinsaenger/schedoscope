package com.ottogroup.bi.soda.bottler

import akka.actor.Actor
import com.ottogroup.bi.soda.dsl.transformations.filesystem._
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.conf.Configuration
import java.net.URI
import org.apache.hadoop.fs.Path
import com.ottogroup.bi.soda.dsl.transformations.filesystem.IfNotExists
import akka.actor.Props
import akka.actor.Actor
import com.ottogroup.bi.soda.dsl.transformations.sql.HiveQl
import com.ottogroup.bi.soda.bottler.driver.HiveDriver
import scala.concurrent._
import org.apache.hadoop.fs.FileUtil
import org.apache.hadoop.fs.PathFilter
import scala.util.matching.Regex
import org.apache.hadoop.security.UserGroupInformation
import java.security.PrivilegedAction
import org.apache.hadoop.fs.FileStatus
import akka.actor.ActorRef
import akka.event.Logging
import com.ottogroup.bi.soda.bottler.driver.FileSystemDriver
import com.ottogroup.bi.soda.dsl.Transformation
import com.ottogroup.bi.soda.bottler.api.SettingsImpl
import com.ottogroup.bi.soda.bottler.api.DriverSettings

class FileSystemActor(ds:DriverSettings) extends Actor {
  import context._
  val ec = ExecutionContext.global
  val ugi = UserGroupInformation.getLoginUser()
  val driver =  FileSystemDriver(ds)
  val log = Logging(system, this)

  def receive = {
  	case WorkAvailable => sender ! PollCommand("file")
    case CommandWithSender(cmd: FileOperation, sendingActor: ActorRef) => {
      val requester = sendingActor
      val operation = future {
        driver.runAndWait(cmd)
      }(ec)
      operation.onSuccess {
        case true => { requester ! new Success }
        case false => { requester ! new Failure }
      }(ec)
      operation.onFailure { case t => { requester ! Exception(t) } }(ec)
    }
  }
}

object FileSystemActor {
  def props(ds:DriverSettings) = Props(new FileSystemActor(ds))

}
