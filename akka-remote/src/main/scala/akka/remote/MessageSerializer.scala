/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.remote

import akka.remote.WireFormats._
import com.google.protobuf.ByteString
import akka.actor.ExtendedActorSystem
import akka.serialization.SerializationExtension

/**
 * INTERNAL API
 *
 * MessageSerializer is a helper for serializing and deserialize messages
 */
private[akka] object MessageSerializer {

  /**
   * Uses Akka Serialization for the specified ActorSystem to transform the given MessageProtocol to a message
   */
  def deserialize(system: ExtendedActorSystem, messageProtocol: SerializedMessage): AnyRef = {
    val out = ByteString.newOutput()

    SerializationExtension(system).deserialize(
      messageProtocol.getMessage.newInput(),
      messageProtocol.getSerializerId,
      if (messageProtocol.hasMessageManifest) Some(system.dynamicAccess.getClassFor[AnyRef](messageProtocol.getMessageManifest.toStringUtf8).get) else None).get
  }

  /**
   * Uses Akka Serialization for the specified ActorSystem to transform the given message to a MessageProtocol
   */
  def serialize(system: ExtendedActorSystem, message: AnyRef): SerializedMessage = {
    val s = SerializationExtension(system)
    val serializer = s.findSerializerFor(message)
    val builder = SerializedMessage.newBuilder
    val os = ByteString.newOutput()
    serializer.toOutputStream(message, os)
    builder.setMessage(os.toByteString)
    builder.setSerializerId(serializer.identifier)
    if (serializer.includeManifest)
      builder.setMessageManifest(ByteString.copyFromUtf8(message.getClass.getName))
    builder.build
  }
}
