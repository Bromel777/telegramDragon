package org.encryfoundation.tg.pipelines.groupVerification

import cats.Applicative
import cats.effect.concurrent.MVar
import cats.effect.{Concurrent, Sync, Timer}
import io.chrisdavenport.log4cats.Logger
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi.SecretChat
import org.encryfoundation.mitmImun.{Prover, Verifier}
import org.encryfoundation.tg.RunApp.sendMessage
import org.encryfoundation.tg.pipelines.{HeadPipeline, Pipeline}
import scorex.crypto.encode.Base64
import scorex.crypto.hash.Blake2b256
import cats.implicits._
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory
import org.encryfoundation.tg.pipelines.groupVerification.messages.StepMsg
import org.encryfoundation.tg.pipelines.groupVerification.messages.StepMsg.GroupVerificationStepMsg.{ProverFirstStepMsg, ProverThirdStepMsg, VerifierSecondStepMsg}
import org.encryfoundation.tg.pipelines.groupVerification.messages.serializer.groupVerification.VerifierSecondMsgSerializer._
import org.encryfoundation.tg.pipelines.groupVerification.messages.StepMsg.{EndPipeline, StartPipeline}
import org.encryfoundation.tg.pipelines.groupVerification.messages.serializer.StartPipelineMsgSerializer._
import org.encryfoundation.tg.pipelines.groupVerification.messages.serializer.EndPipelineMsgSerializer._
import org.encryfoundation.tg.pipelines.groupVerification.messages.serializer.StepMsgSerializer

case class VerifierSecondStep[F[_]: Concurrent: Timer: Logger](proverMsg: MVar[F, ProverFirstStepMsg],
                                                               verifierVar: MVar[F, Verifier],
                                                               chatId: Long,
                                                               client: Client[F]) extends HeadPipeline[F] {

  private def send2Chat[M <: StepMsg](msg: M)(implicit s: StepMsgSerializer[M]): F[Unit] = for {
    _ <- Logger[F].info(s"Send: ${msg}")
    _ <- sendMessage(
      chatId,
      Base64.encode(StepMsgSerializer.toBytes(msg)),
      client
    )
  } yield ()


  //todo: add logging
  def processPreviousStepStart: F[Pipeline[F]] = Applicative[F].pure(this)

  def processPreviousStepEnd: F[Pipeline[F]] = for {
    _ <- Logger[F].info("Process processPreviousStepEnd on VerifierSecondStep")
    pairing <- Sync[F].delay(PairingFactory.getPairing("src/main/resources/properties/a.properties"))
    msg <- proverMsg.read
    verifier <- Verifier(
      msg.g1Gen,
      msg.g2Gen,
      msg.zRGen,
      msg.proverPublicKey1,
      msg.proverPublicKey2,
      msg.gTilda,
      pairing.getZr.newRandomElement(),
      pairing
    ).pure[F]
    _ <- Logger[F].info("Getting second step")
    secondStep <- verifier.secondStep().pure[F]
    _ <- Logger[F].info("Send VerifierSecondStepMsg")
    _ <- send2Chat(StartPipeline(VerifierSecondStep.pipeLineName))
    _ <- send2Chat(VerifierSecondStepMsg(verifier.publicKey, secondStep))
    _ <- send2Chat(EndPipeline(VerifierSecondStep.pipeLineName))
    emptyMVar <- MVar.empty[F, ProverThirdStepMsg]
  } yield VerifierForthStep(
    verifier,
    msg.firstStep,
    secondStep,
    emptyMVar,
    client,
    chatId
  )

  //todo errors
  def processStepInput(input: StepMsg): F[Pipeline[F]] = input match {
    case msg: ProverFirstStepMsg => for {
      _ <- proverMsg.put(msg)
    } yield this
    case _ => Applicative[F].pure(this)
  }

  //todo: remove get
  override def processInput(input: Array[Byte]): F[Pipeline[F]] =
    StepMsgSerializer.parseBytes(input).right.get match {
      case StartPipeline(pipelineName) if pipelineName == ProverFirstStep.pipelineName  => processPreviousStepStart
      case EndPipeline(pipelineName) if pipelineName == ProverFirstStep.pipelineName => processPreviousStepEnd
      case msg: ProverFirstStepMsg => processStepInput(msg)
    }

  override val pipelineName: String = VerifierSecondStep.pipeLineName
}

object VerifierSecondStep {

  val pipeLineName = "verifierSecondStep"
}
