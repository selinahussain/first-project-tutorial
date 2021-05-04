package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import models.DataModel
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.FakeRequest
import play.api.http.Status
import repositories.DataRepository

import scala.concurrent.{ExecutionContext, Future}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, Json}
import reactivemongo.api.commands.{LastError, WriteResult}
import reactivemongo.core.errors.GenericDriverException

class ApplicationControllerSpec extends UnitSpec with GuiceOneAppPerTest with MockitoSugar {

  lazy val controllerComponents: ControllerComponents = app.injector.instanceOf[ControllerComponents]

  implicit lazy val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val mockDataRepository: DataRepository = mock[DataRepository]

  implicit val system: ActorSystem = ActorSystem("Sys")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  object TestApplicationController extends ApplicationController(
    controllerComponents,
    mockDataRepository,
    executionContext
  )

  val dataModel: DataModel = DataModel(
    "abcd",
    "test name",
    "test description",
    100
  )

  "ApplicationController .index" should {

    "return OK" in {

      when(mockDataRepository.find(any())(any()))
        .thenReturn(Future(List(dataModel)))

      val result = TestApplicationController.index()(FakeRequest())

      status(result) shouldBe Status.OK
    }

//    "checking the json body" in {
//      val result = TestApplicationController.index()(FakeRequest())
//      await(jsonBodyOf(result))
//    }
  }

  "ApplicationController .create" should {

    "the json body is valid" should {


      "return Created" in {
        val jsonBody: JsObject = Json.obj(
          "_id" -> "abcd",
          "name" -> "test name",
          "description" -> "test description",
          "numSales" -> 100
        )

        val writeResult: WriteResult = LastError(ok = true, None, None, None, 0, None, updatedExisting = false, None, None, wtimeout = false, None, None)

        when(mockDataRepository.create(any()))
          .thenReturn(Future(writeResult))

        val result = TestApplicationController.create()(FakeRequest().withBody(jsonBody))

        status(result) shouldBe Status.CREATED
      }
    }

    "the json body is not valid" should {

      "return BAD_REQUEST" in {
        val jsonBody: JsObject = Json.obj(
          "_id" -> "abcd",
          "fail" -> "test name"
        )

        val result = TestApplicationController.create()(FakeRequest().withBody(jsonBody))

        status(result) shouldBe Status.BAD_REQUEST
      }

    }

    "the mongo data creation failed" should {

      val jsonBody: JsObject = Json.obj(
        "_id" -> "abcd",
        "name" -> "test name",
        "description" -> "test description",
        "numSales" -> 100
      )

      "return an error" in {

        when(mockDataRepository.create(any()))
          .thenReturn(Future.failed(GenericDriverException("Error")))

        val result = TestApplicationController.create()(FakeRequest().withBody(jsonBody))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        await(bodyOf(result)) shouldBe Json.obj("message" -> "Error adding item to Mongo").toString()
      }
    }

  }


  "ApplicationController .read()" should {

    "return OK" in {

      when(mockDataRepository.read(any()))
        .thenReturn(Future(dataModel))

      val result = TestApplicationController.read("_id": String)(FakeRequest())

      status(result) shouldBe Status.OK
    }
  }

  "ApplicationController.update()" when {

    "supplied valid json" should {

      val jsonBody: JsObject = Json.obj(
        "_id" -> "abcd",
        "name" -> "test name",
        "description" -> "test description",
        "numSales" -> 100
      )

      "return a <status code for success>" in {

        when(mockDataRepository.update(dataModel))
          .thenReturn(Future(dataModel))

        val result = TestApplicationController.update("_id": String)(FakeRequest().withBody(jsonBody))

        status(result) shouldBe Status.ACCEPTED
      }

      "return the correct JSON body" in {
        val result = TestApplicationController.update("_id": String)(FakeRequest().withBody(jsonBody))
        await(jsonBodyOf(result)) shouldBe jsonBody
      }
    }

    "supplied invalid json" should {
      val jsonBody: JsObject = Json.obj(
        "unexpected field" -> "foo"
      )

      "return a <status code for failure>" in {
        val result = TestApplicationController.update("_id": String)(FakeRequest().withBody(jsonBody))

        status(result) shouldBe Status.BAD_REQUEST
      }

    }
  }


  "ApplicationController .delete()" should {
    "return Accepted" in{
      val writeResult: WriteResult = LastError(ok = true, None, None, None, 0, None, updatedExisting = false, None, None, wtimeout = false, None, None)

      when(mockDataRepository.delete("_id":String))
        .thenReturn(Future(writeResult))

      val result = TestApplicationController.delete("_id":String)(FakeRequest())
      status(result) shouldBe Status.ACCEPTED
    }
  }

}
