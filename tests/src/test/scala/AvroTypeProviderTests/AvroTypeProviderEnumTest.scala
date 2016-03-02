package com.miguno.avro

import test.TestUtil

import org.specs2.mutable.Specification
import org.apache.avro.Schema.Parser
import org.apache.avro.file.DataFileWriter

import com.julianpeeters.avro.annotations._

@AvroTypeProvider("tests/src/test/resources/AvroTypeProviderTestEnum.avsc")
@AvroRecord
case class AvroTypeProviderTestEnum1()

@AvroTypeProvider("tests/src/test/resources/AvroTypeProviderTestEnum.avsc")
@AvroRecord
case class AvroTypeProviderTestEnum2()

@AvroTypeProvider("tests/src/test/resources/AvroTypeProviderTestEnum.avsc")
@AvroRecord
case class AvroTypeProviderTestEnum3()


class AvroTypeProviderEnumTest extends Specification {

  // Use the original schema, not the auto-generated version,
  // because the auto-generated one doesn't have the symbols in it.
  val schema = new Parser().parse(this.getClass.getResourceAsStream("/AvroTypeProviderTestEnum.avsc"))

  "Case classes generated from an .avsc file containing an enum field" should {

    "serialize and deserialize if the value is one of the expected symbols" in {
      val record = AvroTypeProviderTestEnum1(suit = "CLUBS")
      TestUtil.verifyWriteAndReadWith(schema)(List(record))
    }

    "fail to serialize if the value is not in the expected symbols" in {
      val record = AvroTypeProviderTestEnum1(suit = "clubs")
      TestUtil.verifyWriteAndReadWith(schema)(List(record)) must throwA[DataFileWriter.AppendWriteException]
    }

    "handle optional values when they're empty" in {
      val record = AvroTypeProviderTestEnum2(estimate = None)
      TestUtil.verifyWriteAndReadWith(schema)(List(record))
    }

    "handle optional values when they're defined" in {
      skipped("This would require a proper enum type according to https://issues.apache.org/jira/browse/AVRO-997")
      val record = AvroTypeProviderTestEnum2(estimate = Some("SMALL"))
      TestUtil.verifyWriteAndReadWith(schema)(List(record))
    }

    "apply default value if defined in schema" in {
      val record = AvroTypeProviderTestEnum3()
      record.status must ===("ACTIVE")
      TestUtil.verifyWriteAndReadWith(schema)(List(record))
    }

    "overwrite default with value from the file" in {
      val record = AvroTypeProviderTestEnum3(status = "COMPLETED")
      TestUtil.verifyWriteAndReadWith(schema)(List(record))
    }
  }

}
