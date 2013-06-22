package org.educoder.epic

import org.cometd.client._
import org.cometd.bayeux.client._
import org.cometd.bayeux._
import org.cometd.websocket.client._
import org.eclipse.jetty.websocket._
import org.eclipse.jetty.client.{ContentExchange, HttpExchange, HttpClient}
import org.cometd.client.transport.ClientTransport
import org.cometd.client.transport.LongPollingTransport
import java.lang.String
import ltg.commons.porcupine_handler._
import akka.actor.{Props, ActorSystem, Actor}
import akka.event.Logging
import com.fasterxml.jackson._
import com.fasterxml.jackson.databind.{ObjectMapper, JsonNode}
import scala.collection.JavaConversions._
import scala.collection._
import com.fasterxml.jackson.databind.node.ObjectNode
import scala.util.matching.Regex
import scala.Some

object PeacefulPorcupine extends App {

  val DrowsyDbPattern = """[^\s\.\$\/\\\*]+"""
  val DrowsyCollPattern = """[_[a-z]][^$]+"""
  val DrowsyIdPattern = """[0-9a-f]+"""

  val DrowsyCollChannelPattern = ("(/"+DrowsyDbPattern+"/"+DrowsyCollPattern+")/\\*$").r
  val DrowsyDocChannelPattern = ("(/"+DrowsyDbPattern+"/"+DrowsyCollPattern+"/"+DrowsyIdPattern+")$").r

  val system = ActorSystem("EPIC")
  val ag = system.actorOf(Props[Peacemaker], name = "peaceful-porcupine")

  class Peacemaker extends Actor {
    val http: HttpClient = new HttpClient()
    http.start()

    val peh: PorcupineEventHandler = new PorcupineEventHandler("gugo@glint", "gugo", "porcupine@conference.glint")

    val weaselUrl = "http://localhost:7777/faye"
    val drowsyUrl = "http://localhost:9292"

    val options = new java.util.HashMap[String, Object]()
    val transport: ClientTransport = LongPollingTransport.create(options, http)
    //val transport: ClientTransport = WebSocketTransport.create(options, websocketFactory);
    val fayeClient: BayeuxClient = new BayeuxClient(weaselUrl, transport)
    val drowsyClient: HttpClient = new HttpClient()
    drowsyClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL)

    val jsonObjectMapper = new ObjectMapper()

    val wakefulListener = new ClientSessionChannel.MessageListener {

      def onMessage(channel: ClientSessionChannel, message: Message) {

        val channelName = channel.getId
        val url = PeacefulPorcupine.DrowsyCollChannelPattern findFirstIn channelName match {
          case Some(PeacefulPorcupine.DrowsyCollChannelPattern(collUrl)) => collUrl
          case None => channelName
        }
        val payload: java.util.Map[String, AnyRef] = message.getDataAsMap

        val action = payload("action").asInstanceOf[String]
        val data = payload("data").asInstanceOf[java.util.Map[String, AnyRef]]

        println(s"< $action $url $data")

        //    val json: ObjectNode = objectMapper.createObjectNode()
        //    json.put("action", action)
        //    json.put("url", url)
        //    json.put("data",  )

        peh.generateEvent(action, url, jsonObjectMapper.valueToTree[JsonNode](data))
      }
    }

    val subRequestListener = new PorcupineEventListener {
      def processEvent(e: PorcupineEvent) {
        val e2 = e.asInstanceOf[PorcupineEventFromXMPP]
        val url = e2.getURL
        val data: JsonNode = e2.getData
        val origin = e2.getOrigin
        val req = e2.getRequest
        //println("got create", data)
        //peh.generateEvent("creation", url, data)

        val channelOption: Option[String] = url match {
          case DrowsyDocChannelPattern(docUrl) => Some(docUrl)
          case DrowsyCollChannelPattern(collUrl) => Some(collUrl)
        }

        if (channelOption.isEmpty) {
          println(s"! TRIED TO SUBSCRIBE TO INVALID CHANNEL: $url")
          return
        }


        val read = new ContentExchange(true) {
          override def onResponseComplete() {
            val status = getResponseStatus
            if (status == 200) {
              val data = getResponseContent
              println(s"= $url $data")
              peh.generateEvent("update", url, jsonObjectMapper.readTree(data))
            } else {
              println(s"! read failed $url $status")
            }
          }
        }
        read.setMethod("GET")
        read.setURL(drowsyUrl+url)
        drowsyClient.send(read)
      }
    }

    val crudRequestListener = new PorcupineEventListener {
      def processEvent(e: PorcupineEvent) {
        val e2 = e.asInstanceOf[PorcupineEventFromXMPP]
        val url = e2.getURL
        val data: JsonNode = e2.getData
        val origin = e2.getOrigin
        val req = e2.getRequest
        //println("got create", data)
        //peh.generateEvent("creation", url, data)

        println(s"> $req $url ($origin) $data")

        fayeClient.getChannel(url).publish(data.toString)
      }
    }

    fayeClient.getChannel(Channel.META_HANDSHAKE).addListener(new ClientSessionChannel.MessageListener() {
      def onMessage(channel: ClientSessionChannel, message: Message) {
        if (message.isSuccessful) {
          println(s"# $channel")
          subscribe("/ck-test2/contributions/*")
        } else {
          println(s"! handshake failed")
        }
      }
    })

    fayeClient.getChannel(Channel.META_SUBSCRIBE).addListener(new ClientSessionChannel.MessageListener() {
      def onMessage(channel: ClientSessionChannel, message: Message) {
        if (message.isSuccessful) {
          println(s"@ $channel $message")

          //val data = new java.util.HashMap[String, Object]();
          //data.put("foo", "faa")
          //client.getChannel("/ck-test2/tags").publish(data);
        } else {
          println(s"! subscription failed $channel")
        }
      }
    })

    def subscribe(url: String) {
      fayeClient.getChannel(url).subscribe(wakefulListener)
    }

    peh.registerHandler("create", crudRequestListener)
    peh.registerHandler("replace", crudRequestListener)
    peh.registerHandler("patch", crudRequestListener)
    peh.registerHandler("delete", crudRequestListener)
    peh.registerHandler("subscribe", subRequestListener)

    val log = Logging(context.system, this)

    def receive = {
      // this is just a stub; for now we use an actor only to ensure that
      // the program keeps running like a server
      case _ => log.info("Received a message")
    }

    override def preStart() {
      fayeClient.handshake()
      drowsyClient.start()
      peh.runAsynchronously()
    }
  }
}
