/**
  * Copyright 2017 Fabio Tiriticco, Fabway
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package cities

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, get, path, _}
import akka.http.scaladsl.server.PathMatchers.Segment
import akka.pattern.ask
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object CitiesApp extends App with SprayJsonSupport with DefaultJsonProtocol {

  implicit val system = ActorSystem("cities")
  implicit val materializer = ActorMaterializer()
  val supervisor = system.actorOf(Props(new Supervisor), "supervisor")

  val route = {
    path(Segment) { city =>
      post {
        entity(as[String]) { citizenName =>
          supervisor ! Citizen(city, citizenName)
          complete(StatusCodes.OK)
        }
      } ~ get {
        val futureList = (supervisor ? Census(city)) (3.seconds).mapTo[List[String]]
        onComplete(futureList) {
          case Success(list) => complete(list)
          case Failure(error) => complete(error)
        }
      }
    }
  }

  Http().bindAndHandle(route, "0.0.0.0", 8080)
  println("server listening at 0.0.0.0:8080")

}
