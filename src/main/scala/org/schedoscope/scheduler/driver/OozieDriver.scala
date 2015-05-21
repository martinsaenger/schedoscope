package org.schedoscope.scheduler.driver

import java.util.Properties

import scala.collection.JavaConversions.asScalaSet
import scala.collection.JavaConversions.propertiesAsScalaMap
import scala.concurrent.duration.Duration

import org.apache.hadoop.security.UserGroupInformation
import org.apache.oozie.client.OozieClient
import org.apache.oozie.client.WorkflowJob.Status.PREP
import org.apache.oozie.client.WorkflowJob.Status.RUNNING
import org.apache.oozie.client.WorkflowJob.Status.SUCCEEDED
import org.apache.oozie.client.WorkflowJob.Status.SUSPENDED
import org.joda.time.LocalDateTime

import org.schedoscope.DriverSettings
import org.schedoscope.Settings
import org.schedoscope.dsl.transformations.OozieTransformation
import com.typesafe.config.ConfigFactory

class OozieDriver(val client: OozieClient) extends Driver[OozieTransformation] {

  override def transformationName = "oozie"

  def run(t: OozieTransformation): DriverRunHandle[OozieTransformation] = try {
    val jobConf = createOozieJobConf(t)
    val oozieJobId = runOozieJob(jobConf)
    new DriverRunHandle[OozieTransformation](this, new LocalDateTime(), t, oozieJobId)
  } catch {
    case e: Throwable => throw DriverException("Unexpected error occurred while running Oozie job", e)
  }

  override def getDriverRunState(run: DriverRunHandle[OozieTransformation]) = {
    val jobId = run.stateHandle.toString
    try {
      val state = getJobInfo(jobId).getStatus()

      state match {
        case SUCCEEDED => DriverRunSucceeded[OozieTransformation](this, s"Oozie job ${jobId} succeeded")
        case SUSPENDED | RUNNING | PREP => DriverRunOngoing[OozieTransformation](this, run)
        case _ => DriverRunFailed[OozieTransformation](this, s"Oozie job ${jobId} failed", DriverException(s"Failed Oozie job status ${state}"))
      }
    } catch {
      case e: Throwable => throw DriverException(s"Unexpected error occurred while checking run state of Oozie job ${jobId}", e)
    }
  }

  override def runAndWait(t: OozieTransformation): DriverRunState[OozieTransformation] = {
    val runHandle = run(t)

    while (getDriverRunState(runHandle).isInstanceOf[DriverRunOngoing[OozieTransformation]])
      Thread.sleep(5000)

    getDriverRunState(runHandle)
  }

  override def killRun(run: DriverRunHandle[OozieTransformation]) = {
    val jobId = run.stateHandle.toString
    try {
      client.kill(jobId)
    } catch {
      case e: Throwable => throw DriverException(s"Unexpected error occurred while killing Oozie job ${run.stateHandle}", e)
    }
  }

  def runOozieJob(jobProperties: Properties): String = client.run(jobProperties)

  def getJobInfo(jobId: String) = client.getJobInfo(jobId)

  def createOozieJobConf(wf: OozieTransformation): Properties =
    wf match {
      case o: OozieTransformation => {
        val properties = new Properties()
        o.configuration.foreach(c => properties.put(c._1, c._2.toString()))

        properties.put(OozieClient.APP_PATH, wf.workflowAppPath)
        properties.remove(OozieClient.BUNDLE_APP_PATH)
        properties.remove(OozieClient.COORDINATOR_APP_PATH)

        // resolve embedded variables
        val config = ConfigFactory.parseProperties(properties).resolve()
        config.entrySet().foreach(e => properties.put(e.getKey(), e.getValue().unwrapped().toString()))
        if (!properties.containsKey("user.name"))
          properties.put("user.name", UserGroupInformation.getLoginUser().getUserName());
        properties
      }
    }
}

object OozieDriver {
  def apply(ds: DriverSettings) = new OozieDriver(new OozieClient(ds.url))
}