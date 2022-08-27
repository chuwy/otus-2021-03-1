package me.chuwy.otusfp

import cats.data.{Validated, ValidatedNec}
import cats.implicits.catsSyntaxTuple3Semigroupal

package object homework8 {

  case class CounterDTO(counter: Long)

  case class SlowParams(chunk: Int, total: Int, time: Int)

  case class SlowParamsToCheck(chunk: String, total: String, time: String) { self =>

    private def validateInt(v: String, errorMsg: String): Validated[String, Int] = {
      if (v.matches("^[1-9]\\d*$")) Validated.Valid(v.toInt) else Validated.Invalid(errorMsg)
    }

    def validate: ValidatedNec[String, SlowParams]  = (
      validateInt(self.chunk, "chunk is not integer greater than 0").toValidatedNec,
      validateInt(self.total, "total is not integer greater than 0").toValidatedNec,
      validateInt(self.time, "time is not integer greater than 0").toValidatedNec,
      ).mapN {(chunk, total, time) => SlowParams(chunk, total, time)}
  }

}
